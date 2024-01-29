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
      AdjudicationDomainEventType.ADJUDICATION_CREATED,
      -> publish(event = event, adjudication = adjudication)
      AdjudicationDomainEventType.HEARING_CREATED,
      AdjudicationDomainEventType.HEARING_UPDATED,
      AdjudicationDomainEventType.HEARING_DELETED,
      -> publish(event = event, adjudication = adjudication, hearingId = adjudication.hearingIdActioned)
      AdjudicationDomainEventType.PUNISHMENTS_CREATED,
      AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
      AdjudicationDomainEventType.PUNISHMENTS_DELETED,
      AdjudicationDomainEventType.QUASHED,
      AdjudicationDomainEventType.UNQUASHED,
      -> publish(event = event, adjudication = adjudication)
      AdjudicationDomainEventType.HEARING_COMPLETED_CREATED,
      AdjudicationDomainEventType.HEARING_COMPLETED_DELETED,
      AdjudicationDomainEventType.HEARING_OUTCOME_UPDATED,
      AdjudicationDomainEventType.HEARING_ADJOURN_CREATED,
      AdjudicationDomainEventType.HEARING_ADJOURN_DELETED,
      AdjudicationDomainEventType.HEARING_REFERRAL_CREATED,
      AdjudicationDomainEventType.HEARING_REFERRAL_DELETED,
      AdjudicationDomainEventType.PROSECUTION_REFERRAL_OUTCOME,
      AdjudicationDomainEventType.NOT_PROCEED_REFERRAL_OUTCOME,
      AdjudicationDomainEventType.REFERRAL_OUTCOME_DELETED,
      -> publish(event = event, adjudication = adjudication, hearingId = adjudication.hearingIdActioned)
      AdjudicationDomainEventType.REFERRAL_OUTCOME_REFER_GOV,
      AdjudicationDomainEventType.NOT_PROCEED_OUTCOME_DELETED,
      AdjudicationDomainEventType.OUTCOME_UPDATED,
      AdjudicationDomainEventType.NOT_PROCEED_OUTCOME,
      AdjudicationDomainEventType.REF_POLICE_OUTCOME,
      AdjudicationDomainEventType.REFERRAL_DELETED,
      -> publish(event = event, adjudication = adjudication)
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
