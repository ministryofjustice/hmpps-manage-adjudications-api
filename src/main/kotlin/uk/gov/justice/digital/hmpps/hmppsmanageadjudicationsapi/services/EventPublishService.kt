package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import java.time.Clock
import java.time.LocalDateTime

@Service
class EventPublishService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
  private val featureFlagsConfig: FeatureFlagsConfig,
) {

  fun publishEvent(event: AdjudicationDomainEventType, adjudication: ReportedAdjudicationDto) {
    when (event) {
      AdjudicationDomainEventType.ADJUDICATION_CREATED -> if (featureFlagsConfig.adjudications) publish(event = event, adjudication = adjudication)
      AdjudicationDomainEventType.HEARING_CREATED, AdjudicationDomainEventType.HEARING_UPDATED, AdjudicationDomainEventType.HEARING_DELETED ->
        /*if (featureFlagsConfig.hearings)*/ publish(event = event, adjudication = adjudication, hearingId = adjudication.hearingIdActioned)
      else -> publish(event = event, adjudication = adjudication)
    }
  }

  private fun publish(event: AdjudicationDomainEventType, adjudication: ReportedAdjudicationDto, hearingId: Long? = null) {
    snsService.publishDomainEvent(
      event,
      "${event.description} ${adjudication.chargeNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = adjudication.chargeNumber,
        prisonId = adjudication.originatingAgencyId,
        prisonerNumber = adjudication.prisonerNumber,
        hearingId = hearingId,
      ),
    )

    auditService.sendMessage(
      event.auditType,
      adjudication.chargeNumber,
      adjudication,
    )
  }
}
