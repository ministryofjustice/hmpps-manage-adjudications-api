package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import javax.transaction.Transactional
import javax.validation.ValidationException

@Transactional
@Service
class AmendHearingOutcomeService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
  private val referralService: ReferralService,
  private val completedHearingService: CompletedHearingService,
) {

  fun amendHearingOutcome(
    adjudicationNumber: Long,
    status: ReportedAdjudicationStatus,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    val currentInfo = hearingOutcomeService.getCurrentStatusAndLatestOutcome(
      adjudicationNumber = adjudicationNumber
    )
    val currentStatus = currentInfo.first
    val latestHearingOutcome = currentInfo.second

    return if (currentStatus == status) amend(
      adjudicationNumber = adjudicationNumber,
      currentStatus = currentStatus,
      amendHearingOutcomeRequest = amendHearingOutcomeRequest,
    ) else removeAndCreate(
      adjudicationNumber = adjudicationNumber,
      toStatus = status,
      currentStatus = currentStatus,
      latestHearingOutcome = latestHearingOutcome,
      amendHearingOutcomeRequest = amendHearingOutcomeRequest,
    )
  }

  private fun amend(
    adjudicationNumber: Long,
    currentStatus: ReportedAdjudicationStatus,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    val updated = hearingOutcomeService.amendHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      outcomeCodeToAmend = currentStatus.mapStatusToHearingOutcomeCode(),
      adjudicator = amendHearingOutcomeRequest.adjudicator,
      details = amendHearingOutcomeRequest.details,
      plea = amendHearingOutcomeRequest.plea,
      adjournedReason = amendHearingOutcomeRequest.adjournReason,
    )

    val outcomeCodeToAmend = currentStatus.mapStatusToOutcomeCode() ?: return updated

    return outcomeService.amendOutcomeViaService(
      adjudicationNumber = adjudicationNumber,
      outcomeCodeToAmend = outcomeCodeToAmend,
      details = amendHearingOutcomeRequest.details,
      notProceedReason = amendHearingOutcomeRequest.notProceedReason,
      amount = amendHearingOutcomeRequest.amount,
      damagesOwed = amendHearingOutcomeRequest.damagesOwed,
      caution = amendHearingOutcomeRequest.caution,
    )
  }

  private fun removeAndCreate(
    adjudicationNumber: Long,
    toStatus: ReportedAdjudicationStatus,
    currentStatus: ReportedAdjudicationStatus,
    latestHearingOutcome: HearingOutcome,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    // note: this wil validate both actions, and therefore when - else branches will never be called
    currentStatus.validateCanAmend(true)
    toStatus.validateCanAmend(false)

    when (currentStatus) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
        referralService.removeReferral(
          adjudicationNumber = adjudicationNumber
        )
      ReportedAdjudicationStatus.DISMISSED, ReportedAdjudicationStatus.NOT_PROCEED, ReportedAdjudicationStatus.CHARGE_PROVED ->
        completedHearingService.removeOutcome(
          adjudicationNumber = adjudicationNumber
        )
      ReportedAdjudicationStatus.ADJOURNED ->
        hearingOutcomeService.removeAdjourn(
          adjudicationNumber = adjudicationNumber,
          recalculateStatus = false,
        )
      else -> throw RuntimeException("should not of made it to this point - fatal")
    }

    return when (toStatus) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
        referralService.createReferral(
          adjudicationNumber = adjudicationNumber,
          code = HearingOutcomeCode.valueOf(toStatus.name),
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          validate = false,
        )
      ReportedAdjudicationStatus.DISMISSED ->
        completedHearingService.createDismissed(
          adjudicationNumber = adjudicationNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          validate = false,
        )
      ReportedAdjudicationStatus.NOT_PROCEED ->
        completedHearingService.createNotProceed(
          adjudicationNumber = adjudicationNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          reason = amendHearingOutcomeRequest.notProceedReason ?: throw ValidationException("missing reason"),
          validate = false,
        )
      ReportedAdjudicationStatus.ADJOURNED ->
        hearingOutcomeService.createAdjourn(
          adjudicationNumber = adjudicationNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          details = amendHearingOutcomeRequest.details ?: throw ValidationException("missing details"),
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          reason = amendHearingOutcomeRequest.adjournReason ?: throw ValidationException("missing reason"),
        )
      ReportedAdjudicationStatus.CHARGE_PROVED ->
        completedHearingService.createChargeProved(
          adjudicationNumber = adjudicationNumber,
          adjudicator = amendHearingOutcomeRequest.adjudicator ?: latestHearingOutcome.adjudicator,
          plea = amendHearingOutcomeRequest.plea ?: throw ValidationException("missing plea"),
          amount = amendHearingOutcomeRequest.amount,
          caution = amendHearingOutcomeRequest.caution ?: throw ValidationException("missing caution"),
          validate = false,
        )
      else -> throw RuntimeException("should not of made it to this point - fatal")
    }
  }

  companion object {

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
        ReportedAdjudicationStatus.CHARGE_PROVED -> OutcomeCode.CHARGE_PROVED
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
        ReportedAdjudicationStatus.CHARGE_PROVED -> {}
        else -> throw ValidationException("unable to amend $direction $this")
      }
    }
  }
}
