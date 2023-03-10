package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
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
      adjudicationNumber = adjudicationNumber
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
      caution = amendHearingOutcomeRequest.caution,
    )
  }

  private fun removeAndCreate(
    adjudicationNumber: Long,
  ): ReportedAdjudicationDto {
    TODO("implement me")
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
  }
}
