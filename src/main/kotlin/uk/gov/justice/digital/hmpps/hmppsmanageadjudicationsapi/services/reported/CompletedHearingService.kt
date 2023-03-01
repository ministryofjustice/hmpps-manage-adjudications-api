package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import javax.transaction.Transactional
import javax.validation.ValidationException

@Transactional
@Service
class CompletedHearingService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createDismissed(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
    details: String,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber, adjudicator = adjudicator, plea = plea
    )

    return outcomeService.createDismissed(
      adjudicationNumber = adjudicationNumber, details = details
    )
  }

  fun createNotProceed(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
    reason: NotProceedReason,
    details: String,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber, adjudicator = adjudicator, plea = plea
    )

    return outcomeService.createNotProceed(
      adjudicationNumber = adjudicationNumber, reason = reason, details = details
    )
  }

  fun createChargeProved(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
    amount: Double? = null,
    caution: Boolean,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createCompletedHearing(
      adjudicationNumber = adjudicationNumber, adjudicator = adjudicator, plea = plea
    )

    return outcomeService.createChargeProved(
      adjudicationNumber = adjudicationNumber, amount = amount, caution = caution
    )
  }

  fun removeOutcome(
    adjudicationNumber: Long
  ): ReportedAdjudicationDto {
    val idToRemove = outcomeService.getLatestOutcome(adjudicationNumber = adjudicationNumber).validateCanRemove().id!!

    outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber, id = idToRemove
    )

    return hearingOutcomeService.deleteHearingOutcome(
      adjudicationNumber = adjudicationNumber
    )
  }

  companion object {
    fun Outcome?.validateCanRemove(): Outcome {
      this ?: throw ValidationException("No completed hearing outcome to remove")
      if (OutcomeCode.completedHearings().none { it == this.code })
        throw ValidationException("No completed hearing outcome to remove")

      return this
    }
  }
}
