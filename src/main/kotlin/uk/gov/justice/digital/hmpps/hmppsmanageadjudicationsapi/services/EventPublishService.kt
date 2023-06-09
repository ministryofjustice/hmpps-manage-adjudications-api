package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService
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
    snsService.publishDomainEvent(
      event,
      "${event.description} ${adjudication.adjudicationNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        adjudicationNumber = adjudication.adjudicationNumber.toString(),
        prisonerNumber = adjudication.prisonerNumber,
      ),
    )

    auditService.sendMessage(
      event.auditType,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }
}
