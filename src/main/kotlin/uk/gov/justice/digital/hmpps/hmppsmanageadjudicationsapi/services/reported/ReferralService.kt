package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode

@Transactional
@Service
class ReferralService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createReferral(
    chargeNumber: String,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createReferral(
      chargeNumber = chargeNumber,
      code = code,
      adjudicator = adjudicator,
      details = details,
    )
    return outcomeService.createReferral(
      chargeNumber = chargeNumber,
      code = code.outcomeCode!!,
      details = details,
      validate = validate,
    )
  }

  fun removeReferral(chargeNumber: String): ReportedAdjudicationDto {
    val outcomes = outcomeService.getOutcomes(chargeNumber).validateHasReferral().validateReferralIsLatest()
    val outcomeToRemove = outcomes.last()
    val outcomeIndex = outcomes.filter { it.outcome.code == outcomeToRemove.outcome.code }.indexOf(outcomeToRemove)

    if (outcomeToRemove.referralOutcome != null) {
      return outcomeService.deleteOutcome(chargeNumber = chargeNumber, id = outcomeToRemove.referralOutcome.id!!)
    }

    hearingOutcomeService.getHearingOutcomeForReferral(
      chargeNumber = chargeNumber,
      code = outcomeToRemove.outcome.code,
      outcomeIndex = outcomeIndex,
    )?.let {
      hearingOutcomeService.deleteHearingOutcome(chargeNumber = chargeNumber)
    }

    return outcomeService.deleteOutcome(
      chargeNumber = chargeNumber,
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
