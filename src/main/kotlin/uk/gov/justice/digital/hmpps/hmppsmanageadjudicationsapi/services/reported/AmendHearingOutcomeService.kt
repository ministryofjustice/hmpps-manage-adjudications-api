package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

@Transactional
@Service
class AmendHearingOutcomeService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
  private val referralService: ReferralService,
  private val completedHearingService: CompletedHearingService,
  private val reportedAdjudicationService: ReportedAdjudicationService,
) {

  fun amendHearingOutcome(
    chargeNumber: String,
    status: ReportedAdjudicationStatus,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    val currentInfo = hearingOutcomeService.getCurrentStatusAndLatestOutcome(
      chargeNumber = chargeNumber,
    )
    val currentStatus = currentInfo.first
    val latestHearingOutcome = currentInfo.second

    return if (currentStatus == status) {
      amend(
        chargeNumber = chargeNumber,
        currentStatus = currentStatus,
        amendHearingOutcomeRequest = amendHearingOutcomeRequest,
      )
    } else {
      removeAndCreate(
        chargeNumber = chargeNumber,
        toStatus = status,
        currentStatus = currentStatus,
        latestHearingOutcome = latestHearingOutcome,
        amendHearingOutcomeRequest = amendHearingOutcomeRequest,
      )
    }
  }

  private fun amend(
    chargeNumber: String,
    currentStatus: ReportedAdjudicationStatus,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    val updated = hearingOutcomeService.amendHearingOutcome(
      chargeNumber = chargeNumber,
      outcomeCodeToAmend = currentStatus.mapStatusToHearingOutcomeCode(),
      adjudicator = amendHearingOutcomeRequest.adjudicator,
      details = amendHearingOutcomeRequest.details,
      plea = amendHearingOutcomeRequest.plea,
      adjournedReason = amendHearingOutcomeRequest.adjournReason,
    )

    val outcomeCodeToAmend = currentStatus.mapStatusToOutcomeCode() ?: return updated

    return outcomeService.amendOutcomeViaService(
      chargeNumber = chargeNumber,
      outcomeCodeToAmend = outcomeCodeToAmend,
      details = amendHearingOutcomeRequest.details,
      notProceedReason = amendHearingOutcomeRequest.notProceedReason,
    )
  }

  private fun removeAndCreate(
    chargeNumber: String,
    toStatus: ReportedAdjudicationStatus,
    currentStatus: ReportedAdjudicationStatus,
    latestHearingOutcome: HearingOutcome,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    // note: this wil validate both actions, and therefore when - else branches will never be called
    currentStatus.validateCanAmend(true)
    toStatus.validateCanAmend(false)

    if ((referrals.contains(toStatus) || referrals.contains(currentStatus)) &&
      reportedAdjudicationService.lastOutcomeHasReferralOutcome(chargeNumber)
    ) {
      throw ValidationException("referral has outcome - unable to amend")
    }

    when (currentStatus) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
        referralService.removeReferral(
          chargeNumber = chargeNumber,
        )
      ReportedAdjudicationStatus.DISMISSED, ReportedAdjudicationStatus.NOT_PROCEED, ReportedAdjudicationStatus.CHARGE_PROVED ->
        completedHearingService.removeOutcome(
          chargeNumber = chargeNumber,
        )
      ReportedAdjudicationStatus.ADJOURNED ->
        hearingOutcomeService.removeAdjourn(
          chargeNumber = chargeNumber,
          recalculateStatus = false,
        )
      else -> throw RuntimeException("should not of made it to this point - fatal")
    }

    return when (toStatus) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
        referralService.createReferral(
          chargeNumber = chargeNumber,
          code = HearingOutcomeCode.valueOf(toStatus.name),
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          validate = false,
        )
      ReportedAdjudicationStatus.DISMISSED ->
        completedHearingService.createDismissed(
          chargeNumber = chargeNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          validate = false,
        )
      ReportedAdjudicationStatus.NOT_PROCEED ->
        completedHearingService.createNotProceed(
          chargeNumber = chargeNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          reason = amendHearingOutcomeRequest.notProceedReason ?: throw ValidationException("missing reason"),
          validate = false,
        )
      ReportedAdjudicationStatus.ADJOURNED ->
        hearingOutcomeService.createAdjourn(
          chargeNumber = chargeNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          reason = amendHearingOutcomeRequest.adjournReason ?: throw ValidationException("missing reason"),
        )
      ReportedAdjudicationStatus.CHARGE_PROVED ->
        completedHearingService.createChargeProved(
          chargeNumber = chargeNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          validate = false,
        )
      else -> throw RuntimeException("should not of made it to this point - fatal")
    }
  }

  companion object {

    val referrals = listOf(ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD)

    fun ReportedAdjudicationStatus.mapStatusToHearingOutcomeCode() =
      when (this) {
        ReportedAdjudicationStatus.REFER_POLICE -> HearingOutcomeCode.REFER_POLICE
        ReportedAdjudicationStatus.REFER_INAD -> HearingOutcomeCode.REFER_INAD
        ReportedAdjudicationStatus.DISMISSED -> HearingOutcomeCode.COMPLETE
        ReportedAdjudicationStatus.NOT_PROCEED -> HearingOutcomeCode.COMPLETE
        ReportedAdjudicationStatus.ADJOURNED -> HearingOutcomeCode.ADJOURN
        ReportedAdjudicationStatus.CHARGE_PROVED -> HearingOutcomeCode.COMPLETE
        else -> throw ValidationException("unable to amend from this status")
      }

    fun ReportedAdjudicationStatus.mapStatusToOutcomeCode(): OutcomeCode? =
      when (this) {
        ReportedAdjudicationStatus.REFER_POLICE -> OutcomeCode.REFER_POLICE
        ReportedAdjudicationStatus.REFER_INAD -> OutcomeCode.REFER_INAD
        ReportedAdjudicationStatus.DISMISSED -> OutcomeCode.DISMISSED
        ReportedAdjudicationStatus.NOT_PROCEED -> OutcomeCode.NOT_PROCEED
        else -> null
      }

    fun ReportedAdjudicationStatus.validateCanAmend(from: Boolean) {
      val direction = if (from) "from" else "to"
      when (this) {
        ReportedAdjudicationStatus.REFER_POLICE,
        ReportedAdjudicationStatus.REFER_INAD,
        ReportedAdjudicationStatus.DISMISSED,
        ReportedAdjudicationStatus.NOT_PROCEED,
        ReportedAdjudicationStatus.ADJOURNED,
        ReportedAdjudicationStatus.CHARGE_PROVED,
        -> {}
        else -> throw ValidationException("unable to amend $direction $this")
      }
    }
  }
}
