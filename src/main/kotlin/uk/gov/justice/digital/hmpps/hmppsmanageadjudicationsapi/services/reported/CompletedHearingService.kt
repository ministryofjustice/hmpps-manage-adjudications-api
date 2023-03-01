package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import javax.transaction.Transactional

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
    TODO("implement me")
  }
}
