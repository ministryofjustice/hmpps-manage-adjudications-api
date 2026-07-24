package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
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
      chargeNumber = adjudication.chargeNumber,
      agencyId = adjudication.originatingAgencyId,
      prisonerNumber = adjudication.prisonerNumber,
      status = adjudication.status,
      hearingId = if (event.incHearingId) adjudication.hearingIdActioned else null,
    )

    adjudication.suspendedPunishmentEvents?.let {
      it.forEach { event ->
        publish(
          event = AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
          chargeNumber = event.chargeNumber,
          agencyId = event.agencyId,
          prisonerNumber = adjudication.prisonerNumber,
          status = event.status,
        )
      }
    }
  }

  fun publishLossOfVisitsEvent(event: LossOfVisitsEventDto) = publish(
    event = AdjudicationDomainEventType.LOSS_OF_VISITS,
    chargeNumber = event.chargeNumber,
    agencyId = event.prisonId,
    prisonerNumber = event.prisonerNumber,
    status = event.status,
    lossOfVisits = event.details,
  )

  private fun publish(
    event: AdjudicationDomainEventType,
    chargeNumber: String,
    agencyId: String,
    prisonerNumber: String,
    status: ReportedAdjudicationStatus,
    hearingId: Long? = null,
    lossOfVisits: LossOfVisitsDetailsDto? = null,
  ) {
    val additionalInformation = AdditionalInformation(
      chargeNumber = chargeNumber,
      prisonId = agencyId,
      prisonerNumber = prisonerNumber,
      hearingId = hearingId,
      status = status.name,
      lossOfVisits = lossOfVisits,
    )
    snsService.publishDomainEvent(
      event,
      "${event.description} $chargeNumber",
      occurredAt = LocalDateTime.now(clock),
      additionalInformation = additionalInformation,
    )

    auditService.sendMessage(
      event.auditType,
      chargeNumber,
      additionalInformation,
    )
  }
}
