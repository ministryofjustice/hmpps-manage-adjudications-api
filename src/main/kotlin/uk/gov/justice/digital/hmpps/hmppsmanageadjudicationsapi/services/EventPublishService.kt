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
    when (event) {
      AdjudicationDomainEventType.ADJUDICATION_CREATED ->
        snsService.publishDomainEvent(
          event,
          "${event.description} ${adjudication.adjudicationNumber}",
          occurredAt = LocalDateTime.now(clock),
          AdditionalInformation(
            adjudicationNumber = adjudication.adjudicationNumber.toString(),
            prisonerNumber = adjudication.prisonerNumber,
          ),
        )

      AdjudicationDomainEventType.ADJUDICATION_HEARING_CREATED ->
        snsService.publishDomainEvent(
          event,
          "${event.description} ${adjudication.adjudicationNumber}",
          occurredAt = LocalDateTime.now(clock),
          AdditionalInformation(
            adjudicationNumber = adjudication.adjudicationNumber.toString(),
            hearingNumber = adjudication.hearings.maxByOrNull { it.dateTimeOfHearing }?.id.toString(),
          ),
        )

      AdjudicationDomainEventType.ADJUDICATION_OUTCOME_UPSERT ->
        snsService.publishDomainEvent(
          event,
          "${event.description} ${adjudication.adjudicationNumber}",
          occurredAt = LocalDateTime.now(clock),
          AdditionalInformation(
            adjudicationNumber = adjudication.adjudicationNumber.toString(),
            outcomeNumber = adjudication.hearings.maxByOrNull { it.dateTimeOfHearing }?.outcome?.id.toString(),
          ),
        )

      AdjudicationDomainEventType.ADJUDICATION_PUNISHMENT_CREATED ->
        snsService.publishDomainEvent(
          event,
          "${event.description} ${adjudication.adjudicationNumber}",
          occurredAt = LocalDateTime.now(clock),
          AdditionalInformation(
            adjudicationNumber = adjudication.adjudicationNumber.toString(),
            punishmentNumber = adjudication.punishments.last().id.toString(),
          ),
        )
    }

    auditService.sendMessage(
      event.auditType,
      adjudication.adjudicationNumber.toString(),
      adjudication,
    )
  }
}
