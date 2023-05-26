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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventWrapperService
import java.time.LocalDateTime
import java.util.UUID

@Transactional
@Service
class NomisOutcomeService(
  private val eventWrapperService: EventWrapperService,
) {

  fun createHearingResultIfApplicable(adjudicationNumber: String, hearing: Hearing?, outcome: Outcome): String? {
    if (hearing == null && outcome.doNotCallApi()) return null

    hearing?.let {
      if (outcome.createHearingAndOutcome() || isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)) {
        val oicHearingId = eventWrapperService.createHearing(
          adjudicationNumber = adjudicationNumber,
          oicHearingRequest = OicHearingRequest(
            oicHearingId = UUID.randomUUID().toString(),
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

        if (outcome.code == OutcomeCode.QUASHED) eventWrapperService.quashSanctions(adjudicationNumber = adjudicationNumber)

        return oicHearingId
      }

      if (outcome.createOutcome()) createHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome)
      if (outcome.updateHearing()) updateOicHearingDetails(adjudicationNumber = adjudicationNumber, hearing = it)
    }
    return null
  }

  fun amendHearingResultIfApplicable(adjudicationNumber: String, hearing: Hearing?, outcome: Outcome) {
    if (hearing == null && outcome.doNotCallApi()) return

    hearing?.let {
      val isPoliceReferralOutcome = isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)
      if (outcome.canAmendOutcome() || isPoliceReferralOutcome) {
        eventWrapperService.amendHearingResult(
          adjudicationNumber = adjudicationNumber,
          oicHearingId = if (outcome.forceValidationOfOicHearingId(isPoliceReferralOutcome)) outcome.validateOicHearingId() else it.oicHearingId,
          oicHearingResultRequest = OicHearingResultRequest(
            pleaFindingCode = it.validateHearingOutcome().validatePlea().plea,
            findingCode = outcome.code.validateFinding(),
            adjudicator = hearing.getAdjudicator(),
          ),
        )
      }
      if (outcome.updateHearing()) updateOicHearingDetails(adjudicationNumber = adjudicationNumber, hearing = it)
    }
  }

  fun deleteHearingResultIfApplicable(adjudicationNumber: String, hearing: Hearing?, outcome: Outcome) {
    if (hearing == null && outcome.doNotCallApi()) return

    hearing?.let {
      if (outcome.canDeleteOutcome() || isPoliceReferralOutcomeFromHearing(hearing = it, outcome = outcome)) {
        deleteHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome).run {
          eventWrapperService.deleteHearing(adjudicationNumber = adjudicationNumber, oicHearingId = outcome.validateOicHearingId())
        }
        return
      }
      if (outcome.code == OutcomeCode.CHARGE_PROVED) eventWrapperService.deleteSanctions(adjudicationNumber = adjudicationNumber)
      if (outcome.createOutcome()) deleteHearingResult(adjudicationNumber = adjudicationNumber, hearing = it, outcome = outcome)
      if (outcome.updateHearing()) {
        eventWrapperService.amendHearing(
          adjudicationNumber = adjudicationNumber,
          oicHearingRequest = OicHearingRequest(
            oicHearingId = it.oicHearingId,
            dateTimeOfHearing = it.dateTimeOfHearing,
            hearingLocationId = it.locationId,
            oicHearingType = it.oicHearingType,
          ),
        )
      }
    }
  }

  private fun createHearingResult(adjudicationNumber: String, hearing: Hearing, outcome: Outcome, oicHearingId: String? = null) {
    eventWrapperService.createHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = oicHearingId ?: hearing.oicHearingId,
      oicHearingResultRequest = OicHearingResultRequest(
        pleaFindingCode = hearing.validateHearingOutcome().validatePlea().plea,
        findingCode = outcome.code.validateFinding(),
        adjudicator = hearing.getAdjudicator(),
      ),
    )
  }

  private fun deleteHearingResult(adjudicationNumber: String, hearing: Hearing, outcome: Outcome) {
    eventWrapperService.deleteHearingResult(
      adjudicationNumber = adjudicationNumber,
      oicHearingId = outcome.oicHearingId ?: hearing.oicHearingId,
    )
  }

  private fun updateOicHearingDetails(adjudicationNumber: String, hearing: Hearing) {
    eventWrapperService.amendHearing(
      adjudicationNumber = adjudicationNumber,
      oicHearingRequest = OicHearingRequest(
        oicHearingId = hearing.oicHearingId,
        dateTimeOfHearing = hearing.dateTimeOfHearing,
        hearingLocationId = hearing.locationId,
        oicHearingType = hearing.oicHearingType,
        adjudicator = hearing.getAdjudicator(),
        commentText = hearing.hearingOutcome?.code.toString(),
      ),
    )
  }

  companion object {
    private val NO_OUTCOME_WITHOUT_HEARING = listOf(OutcomeCode.PROSECUTION, OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED)
    private val CREATE_OUTCOME = listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED, OutcomeCode.CHARGE_PROVED, OutcomeCode.DISMISSED)
    private val POLICE_REFERRAL_OUTCOMES = listOf(OutcomeCode.PROSECUTION, OutcomeCode.NOT_PROCEED)

    fun Outcome.forceValidationOfOicHearingId(policeReferralOutcome: Boolean): Boolean = this.code == OutcomeCode.QUASHED ||
      policeReferralOutcome && POLICE_REFERRAL_OUTCOMES.contains(this.code)

    fun Outcome.doNotCallApi(): Boolean = NO_OUTCOME_WITHOUT_HEARING.contains(this.code)

    fun Outcome.createHearingAndOutcome(): Boolean = this.code == OutcomeCode.QUASHED

    fun Outcome.createOutcome(): Boolean = CREATE_OUTCOME.contains(this.code)

    fun Outcome.updateHearing(): Boolean = this.code == OutcomeCode.REFER_INAD

    fun Outcome.canAmendOutcome(): Boolean = this.code == OutcomeCode.QUASHED || CREATE_OUTCOME.contains(this.code)

    fun Outcome.canDeleteOutcome(): Boolean = this.code == OutcomeCode.QUASHED

    fun Hearing.validateHearingOutcome(): HearingOutcome = this.hearingOutcome ?: throw ValidationException("missing hearing outcome")

    fun HearingOutcome.validatePlea(): HearingOutcomePlea = this.plea ?: HearingOutcomePlea.NOT_ASKED

    fun OutcomeCode.validateFinding(): Finding = this.finding ?: throw ValidationException("invalid call to api")

    fun Outcome.validateOicHearingId(): String = this.oicHearingId ?: throw ValidationException("oic hearing id not linked to outcome")

    fun isPoliceReferralOutcomeFromHearing(hearing: Hearing, outcome: Outcome): Boolean =
      hearing.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE && POLICE_REFERRAL_OUTCOMES.contains(outcome.code)

    fun Hearing.getAdjudicator(): String? =
      when (this.oicHearingType) {
        OicHearingType.GOV_ADULT, OicHearingType.GOV_YOI, OicHearingType.GOV -> this.hearingOutcome?.adjudicator
        OicHearingType.INAD_YOI, OicHearingType.INAD_ADULT -> null
      }
  }
}
