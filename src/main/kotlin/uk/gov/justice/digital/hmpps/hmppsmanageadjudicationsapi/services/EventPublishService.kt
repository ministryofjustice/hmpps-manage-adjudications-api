package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import java.time.Clock
import java.time.LocalDateTime

@Service
class EventPublishService(
  private val snsService: SnsService,
  private val auditService: AuditService,
  private val clock: Clock,
) {

  fun publishEvent(event: AdjudicationDomainEventType, adjudication: ReportedAdjudicationDto) {
    publish(
      event = event,
      adjudication = adjudication,
      hearingId = if (event.incHearingId) adjudication.hearingIdActioned else null,
    )
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
        status = adjudication.status.name,
      ),
    )

    auditService.sendMessage(
      event.auditType,
      adjudication.chargeNumber,
      adjudication,
    )
  }
}
