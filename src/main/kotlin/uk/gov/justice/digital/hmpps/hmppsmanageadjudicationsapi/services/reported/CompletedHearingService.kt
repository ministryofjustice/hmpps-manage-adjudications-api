package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode

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
    )
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
    )
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
    )
  }

  fun removeOutcome(
    chargeNumber: String,
  ): ReportedAdjudicationDto {
    val idToRemove = outcomeService.getLatestOutcome(chargeNumber = chargeNumber).validateCanRemove().id!!

    outcomeService.deleteOutcome(
      chargeNumber = chargeNumber,
      id = idToRemove,
    )

    return hearingOutcomeService.deleteHearingOutcome(
      chargeNumber = chargeNumber,
    )
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
