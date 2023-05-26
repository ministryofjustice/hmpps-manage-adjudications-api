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
    adjudicationNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createDismissed(
      adjudicationNumber = adjudicationNumber,
      details = details,
      validate = validate,
    )
  }

  fun createNotProceed(
    adjudicationNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    reason: NotProceedReason,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createNotProceed(
      adjudicationNumber = adjudicationNumber,
      reason = reason,
      details = details,
      validate = validate,
    )
  }

  fun createChargeProved(
    adjudicationNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
    amount: Double? = null,
    caution: Boolean,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber,
      adjudicator = adjudicator,
      plea = plea,
    )

    return outcomeService.createChargeProved(
      adjudicationNumber = adjudicationNumber,
      amount = amount,
      caution = caution,
      validate = validate,
    )
  }

  fun removeOutcome(
    adjudicationNumber: String,
  ): ReportedAdjudicationDto {
    val idToRemove = outcomeService.getLatestOutcome(adjudicationNumber = adjudicationNumber).validateCanRemove().id!!

    outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber,
      id = idToRemove,
    )

    return hearingOutcomeService.deleteHearingOutcome(
      adjudicationNumber = adjudicationNumber,
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
