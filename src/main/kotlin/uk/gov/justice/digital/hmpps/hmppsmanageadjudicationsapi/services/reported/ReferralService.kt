package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import javax.transaction.Transactional
import javax.validation.ValidationException

@Transactional
@Service
class ReferralService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createReferral(
    adjudicationNumber: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createReferral(
      adjudicationNumber = adjudicationNumber,
      code = code,
      adjudicator = adjudicator,
      details = details,
    )
    return outcomeService.createOutcome(
      adjudicationNumber = adjudicationNumber,
      code = code.outcomeCode!!,
      details = details
    )
  }

  fun removeReferral(adjudicationNumber: Long): ReportedAdjudicationDto {
    val outcomes = outcomeService.getOutcomes(adjudicationNumber).validateHasReferral()
    val outcomeToRemove = outcomes.last()
    val outcomeIndex = outcomes.filter { it.outcome.code == outcomeToRemove.outcome.code }.indexOf(outcomeToRemove)

    outcomeToRemove.referralOutcome?.let {
      outcomeService.deleteOutcome(adjudicationNumber = adjudicationNumber, id = it.id!!)
    }

    hearingOutcomeService.getHearingOutcomeForReferral(
      adjudicationNumber = adjudicationNumber, code = outcomeToRemove.outcome.code, outcomeIndex = outcomeIndex
    )?.let {
      hearingOutcomeService.deleteHearingOutcome(
        adjudicationNumber = adjudicationNumber, hearingId = it.id!!
      )
    }

    return outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber, id = outcomeToRemove.outcome.id!!
    )
  }

  companion object {
    fun List<CombinedOutcomeDto>.validateHasReferral(): List<CombinedOutcomeDto> {
      if (this.none { OutcomeCode.referrals().contains(it.outcome.code) }) throw ValidationException("No referral for adjudication")

      return this
    }
  }
}
