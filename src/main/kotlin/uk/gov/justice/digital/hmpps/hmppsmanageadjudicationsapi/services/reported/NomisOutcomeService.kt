package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingResultRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import java.time.LocalDateTime

@Transactional
@Service
class NomisOutcomeService(
  private val prisonApiGateway: PrisonApiGateway,
) {

  fun createHearingResultIfApplicable(adjudicationNumber: Long, hearing: Hearing?, outcome: Outcome): Long? {
    if (hearing == null && outcome.doNotCallApi() || outcome.ignore()) return null

    hearing?.let {
      if (outcome.createHearingAndOutcome() || isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)) {
        val oicHearingId = prisonApiGateway.createHearing(
          adjudicationNumber = adjudicationNumber,
          oicHearingRequest = OicHearingRequest(
            dateTimeOfHearing = LocalDateTime.now(),
            oicHearingType = it.oicHearingType,
            hearingLocationId = it.locationId,
          ),
        )
        createHearingResult(
          adjudicationNumber = adjudicationNumber,
          hearing = it,
          outcome = outcome,
          oicHearingId = oicHearingId,
        )

        return oicHearingId
      }

      if (outcome.createOutcome()) createHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome)
    }
    return null
  }

  fun amendHearingResultIfApplicable(adjudicationNumber: Long, hearing: Hearing?, outcome: Outcome) {
    if (hearing == null && outcome.doNotCallApi() || outcome.ignore()) return

    hearing?.let {
      val isPoliceReferralOutcome = isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)
      if (outcome.canAmendOutcome() || isPoliceReferralOutcome) {
        prisonApiGateway.amendHearingResult(
          adjudicationNumber = adjudicationNumber,
          oicHearingId = if (outcome.forceValidationOfOicHearingId(isPoliceReferralOutcome)) outcome.validateOicHearingId() else it.oicHearingId,
          oicHearingResultRequest = OicHearingResultRequest(
            pleaFindingCode = it.validateHearingOutcome().validatePlea().plea,
            findingCode = outcome.code.validateFinding(),
          ),
        )
      }
    }
  }

  fun deleteHearingResultIfApplicable(adjudicationNumber: Long, hearing: Hearing?, outcome: Outcome) {
    if (hearing == null && outcome.doNotCallApi() || outcome.ignore()) return

    hearing?.let {
      if (outcome.canDeleteOutcome() || isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)) {
        deleteHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome).run {
          prisonApiGateway.deleteHearing(adjudicationNumber = adjudicationNumber, oicHearingId = outcome.validateOicHearingId())
        }
        return
      }
      if (outcome.createOutcome()) deleteHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome)
    }
  }

  private fun createHearingResult(adjudicationNumber: Long, hearing: Hearing, outcome: Outcome, oicHearingId: Long? = null) {
    prisonApiGateway.createHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId ?: hearing.oicHearingId,
      oicHearingResultRequest = OicHearingResultRequest(
        pleaFindingCode = hearing.validateHearingOutcome().validatePlea().plea,
        findingCode = outcome.code.validateFinding(),
      ),
    )
  }

  private fun deleteHearingResult(adjudicationNumber: Long, hearing: Hearing, outcome: Outcome) {
    prisonApiGateway.deleteHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = outcome.oicHearingId ?: hearing.oicHearingId,
    )
  }

  companion object {
    private val NO_OUTCOME_WITHOUT_HEARING = listOf(OutcomeCode.PROSECUTION, OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED)
    private val CREATE_HEARING_AND_OUTCOME = listOf(OutcomeCode.QUASHED)
    private val CREATE_OUTCOME = listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED, OutcomeCode.CHARGE_PROVED, OutcomeCode.DISMISSED)
    private val POLICE_REFERRAL_OUTCOMES = listOf(OutcomeCode.PROSECUTION, OutcomeCode.NOT_PROCEED)

    fun Outcome.ignore(): Boolean = this.code == OutcomeCode.REFER_INAD

    fun Outcome.forceValidationOfOicHearingId(policeReferralOutcome: Boolean): Boolean = CREATE_HEARING_AND_OUTCOME.contains(this.code) ||
      policeReferralOutcome && POLICE_REFERRAL_OUTCOMES.contains(this.code)

    fun Outcome.doNotCallApi(): Boolean = NO_OUTCOME_WITHOUT_HEARING.contains(this.code)

    fun Outcome.createHearingAndOutcome(): Boolean = CREATE_HEARING_AND_OUTCOME.contains(this.code)

    fun Outcome.createOutcome(): Boolean = CREATE_OUTCOME.contains(this.code)

    fun Outcome.canAmendOutcome(): Boolean = CREATE_HEARING_AND_OUTCOME.contains(this.code) || CREATE_OUTCOME.contains(this.code)

    fun Outcome.canDeleteOutcome(): Boolean = CREATE_HEARING_AND_OUTCOME.contains(this.code)

    fun Hearing.validateHearingOutcome(): HearingOutcome = this.hearingOutcome ?: throw ValidationException("missing hearing outcome")

    fun HearingOutcome.validatePlea(): HearingOutcomePlea = this.plea ?: HearingOutcomePlea.NOT_ASKED

    fun OutcomeCode.validateFinding(): Finding = this.finding ?: throw ValidationException("invalid call to api")

    fun Outcome.validateOicHearingId(): Long = this.oicHearingId ?: throw ValidationException("oic hearing id not linked to outcome")

    fun isPoliceReferralOutcomeFromHearing(hearing: Hearing, outcome: Outcome): Boolean =
      hearing.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE && POLICE_REFERRAL_OUTCOMES.contains(outcome.code)
  }
}