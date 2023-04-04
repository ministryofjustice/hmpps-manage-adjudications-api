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
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createReferral(
      adjudicationNumber = adjudicationNumber,
      code = code,
      adjudicator = adjudicator,
      details = details,
    )
    return outcomeService.createReferral(
      adjudicationNumber = adjudicationNumber,
      code = code.outcomeCode!!,
      details = details,
      validate = validate,
    )
  }

  fun removeReferral(adjudicationNumber: Long): ReportedAdjudicationDto {
    val outcomes = outcomeService.getOutcomes(adjudicationNumber).validateHasReferral().validateReferralIsLatest()
    val outcomeToRemove = outcomes.last()
    val outcomeIndex = outcomes.filter { it.outcome.code == outcomeToRemove.outcome.code }.indexOf(outcomeToRemove)

    if (outcomeToRemove.referralOutcome != null) {
      return outcomeService.deleteOutcome(adjudicationNumber = adjudicationNumber, id = outcomeToRemove.referralOutcome.id!!)
    }

    hearingOutcomeService.getHearingOutcomeForReferral(
      adjudicationNumber = adjudicationNumber,
      code = outcomeToRemove.outcome.code,
      outcomeIndex = outcomeIndex,
    )?.let {
      hearingOutcomeService.deleteHearingOutcome(adjudicationNumber = adjudicationNumber)
    }

    return outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber,
      id = outcomeToRemove.outcome.id!!,
    )
  }

  companion object {
    fun List<CombinedOutcomeDto>.validateHasReferral(): List<CombinedOutcomeDto> {
      if (this.none { OutcomeCode.referrals().contains(it.outcome.code) }) {
        throw ValidationException("No referral for adjudication")
      }

      return this
    }

    fun List<CombinedOutcomeDto>.validateReferralIsLatest(): List<CombinedOutcomeDto> {
      if (OutcomeCode.referrals().none { it == this.last().outcome.code }) {
        throw ValidationException("Referral can not be removed as its not the latest outcome")
      }

      return this
    }
  }
}
