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

  fun publishAdjudicationCreation(adjudication: ReportedAdjudicationDto) {
    snsService.publishDomainEvent(
      AdjudicationDomainEventType.ADJUDICATION_CREATED,
      "An adjudication has been created: ${adjudication.adjudicationNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        adjudicationNumber = adjudication.adjudicationNumber.toString(),
        prisonerNumber = adjudication.prisonerNumber,
      ),
    )

    auditService.sendMessage(
      AuditType.ADJUDICATION_CREATED,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }

  fun publishAdjudicationUpdate(adjudication: ReportedAdjudicationDto) {
    snsService.publishDomainEvent(
      AdjudicationDomainEventType.ADJUDICATION_UPDATED,
      "An adjudication has been updated: ${adjudication.adjudicationNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        adjudicationNumber = adjudication.adjudicationNumber.toString(),
        prisonerNumber = adjudication.prisonerNumber,
      ),
    )

    auditService.sendMessage(
      AuditType.ADJUDICATION_UPDATED,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }

  fun publishOutcomeUpdate(adjudication: ReportedAdjudicationDto) {
    snsService.publishDomainEvent(
      AdjudicationDomainEventType.ADJUDICATION_OUTCOME_UPSERT,
      "An outcome has been updated for adjudication: ${adjudication.adjudicationNumber}",
      occurredAt = LocalDateTime.now(clock),
      AdditionalInformation(
        adjudicationNumber = adjudication.adjudicationNumber.toString(),
        prisonerNumber = adjudication.prisonerNumber,
      ),
    )

    auditService.sendMessage(
      AuditType.OUTCOME_UPDATED,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }
}
