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
    snsService.publishDomainEvent(
      event,
      "${event.description} ${adjudication.adjudicationNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = adjudication.adjudicationNumber.toString(),
        prisonId = adjudication.originatingAgencyId,
      ),
    )

    auditService.sendMessage(
      event.auditType,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }
}
