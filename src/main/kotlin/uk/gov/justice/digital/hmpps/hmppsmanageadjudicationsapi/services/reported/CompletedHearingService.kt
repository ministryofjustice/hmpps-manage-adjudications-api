package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getLatestHearingId

@Transactional
@Service
class CompletedHearingService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createDismissed(
    chargeNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      chargeNumber = chargeNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createDismissed(
      chargeNumber = chargeNumber,
      details = details,
      validate = validate,
    ).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }
  }

  fun createNotProceed(
    chargeNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    reason: NotProceedReason,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      chargeNumber = chargeNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createNotProceed(
      chargeNumber = chargeNumber,
      reason = reason,
      details = details,
      validate = validate,
    ).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }
  }

  fun createChargeProved(
    chargeNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      chargeNumber = chargeNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createChargeProved(
      chargeNumber = chargeNumber,
      validate = validate,
    ).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }
  }

  fun removeOutcome(
    chargeNumber: String,
  ): ReportedAdjudicationDto {
    val outcomeToRemove = outcomeService.getLatestOutcome(chargeNumber = chargeNumber).validateCanRemove()

    outcomeService.deleteOutcome(
      chargeNumber = chargeNumber,
      id = outcomeToRemove.id!!,
    )

    return hearingOutcomeService.deleteHearingOutcome(
      chargeNumber = chargeNumber,
    ).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
      it.punishmentsRemoved = outcomeToRemove.code == OutcomeCode.CHARGE_PROVED
    }
  }

  companion object {
    fun Outcome?.validateCanRemove(): Outcome {
      this ?: throw ValidationException("No completed hearing outcome to remove")
      if (OutcomeCode.completedHearings().none { it == this.code }) {
        throw ValidationException("No completed hearing outcome to remove")
      }

      return this
    }
  }
}
