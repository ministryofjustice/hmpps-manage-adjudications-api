package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getLatestHearingId
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.NomisOutcomeService.Companion.getAdjudicator

@Service
@Transactional
class HearingOutcomeService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val legacySyncService: LegacySyncService,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  fun createReferral(
    chargeNumber: String,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto =
    createHearingOutcome(
      chargeNumber = chargeNumber,
      code = code.validateReferral(),
      adjudicator = adjudicator,
      details = details,
    )

  fun createAdjourn(
    chargeNumber: String,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason,
    details: String,
    plea: HearingOutcomePlea,
  ): ReportedAdjudicationDto =
    createHearingOutcome(
      chargeNumber = chargeNumber,
      code = HearingOutcomeCode.ADJOURN,
      adjudicator = adjudicator,
      reason = reason,
      plea = plea,
      details = details,
    ).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }

  fun createCompletedHearing(
    chargeNumber: String,
    adjudicator: String,
    plea: HearingOutcomePlea,
  ): ReportedAdjudicationDto = createHearingOutcome(
    chargeNumber = chargeNumber,
    code = HearingOutcomeCode.COMPLETE,
    adjudicator = adjudicator,
    plea = plea,
  )

  fun removeAdjourn(
    chargeNumber: String,
    recalculateStatus: Boolean = true,
  ): ReportedAdjudicationDto {
    findByChargeNumber(chargeNumber).latestOutcomeIsAdjourn()

    return deleteHearingOutcome(chargeNumber = chargeNumber, recalculateStatus = recalculateStatus).also {
      it.hearingIdActioned = it.hearings.getLatestHearingId()
    }
  }

  private fun createHearingOutcome(
    chargeNumber: String,
    code: HearingOutcomeCode,
    adjudicator: String,
    reason: HearingOutcomeAdjournReason? = null,
    details: String? = null,
    plea: HearingOutcomePlea? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.getHearing().validateCanRefer(code)
    }

    if (reportedAdjudication.getHearing().hearingOutcome != null) throw RuntimeException("back key detected")

    reportedAdjudication.getHearing().hearingOutcome = HearingOutcome(
      code = code,
      reason = reason,
      details = details,
      adjudicator = adjudicator,
      plea = plea,
    )

    if (code.updateOicHearingDetails()) updateOicHearingDetails(reportedAdjudication)

    return saveToDto(reportedAdjudication.also { if (code.shouldRecalculateStatus()) it.calculateStatus() })
  }

  fun deleteHearingOutcome(chargeNumber: String, recalculateStatus: Boolean = true): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val hearingToRemoveOutcome = reportedAdjudication.getHearing()

    val code = hearingToRemoveOutcome.hearingOutcome.hearingOutcomeExists().code
    hearingToRemoveOutcome.hearingOutcome = null

    if (code.updateOicHearingDetails()) removeOicHearingDetails(reportedAdjudication)

    return saveToDto(reportedAdjudication.also { if (code.shouldRecalculateStatus() && recalculateStatus) it.calculateStatus() })
  }

  fun getCurrentStatusAndLatestOutcome(
    chargeNumber: String,
  ): Pair<ReportedAdjudicationStatus, HearingOutcome> {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return Pair(reportedAdjudication.status, reportedAdjudication.latestHearingOutcome())
  }

  fun amendHearingOutcome(
    chargeNumber: String,
    outcomeCodeToAmend: HearingOutcomeCode,
    adjudicator: String? = null,
    details: String? = null,
    plea: HearingOutcomePlea? = null,
    adjournedReason: HearingOutcomeAdjournReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val hearingOutcomeToAmend = reportedAdjudication.latestHearingOutcome().isLatestSameAsAmendRequest(outcomeCodeToAmend)

    adjudicator?.let { hearingOutcomeToAmend.adjudicator = it }

    when (outcomeCodeToAmend) {
      HearingOutcomeCode.COMPLETE -> plea?.let { hearingOutcomeToAmend.plea = it }
      HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.REFER_INAD, HearingOutcomeCode.REFER_GOV -> details?.let { hearingOutcomeToAmend.details = it }
      HearingOutcomeCode.ADJOURN -> {
        details?.let { hearingOutcomeToAmend.details = it }
        plea?.let { hearingOutcomeToAmend.plea = it }
        adjournedReason?.let { hearingOutcomeToAmend.reason = it }
      }

      HearingOutcomeCode.NOMIS -> throw RuntimeException("unable to amend a NOMIS hearing outcome")
    }

    if (outcomeCodeToAmend.updateOicHearingDetails()) updateOicHearingDetails(reportedAdjudication)

    return saveToDto(reportedAdjudication)
  }

  fun getHearingOutcomeForReferral(chargeNumber: String, code: OutcomeCode, outcomeIndex: Int): HearingOutcome? {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    if (reportedAdjudication.hearings.none { it.hearingOutcome?.code?.outcomeCode == code }) return null
    val matched = reportedAdjudication.hearings.filter { it.hearingOutcome?.code?.outcomeCode == code }.sortedBy { it.dateTimeOfHearing }
    // note: you can only REFER_POLICE once without a hearing
    val actualIndex = if (matched.size > outcomeIndex) outcomeIndex else outcomeIndex - 1

    return matched[actualIndex].hearingOutcome
  }

  private fun updateOicHearingDetails(reportedAdjudication: ReportedAdjudication) {
    val hearing = reportedAdjudication.getHearing()
    legacySyncService.amendHearing(
      adjudicationNumber = reportedAdjudication.chargeNumber,
      oicHearingId = hearing.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = hearing.dateTimeOfHearing,
        hearingLocationId = hearing.locationId,
        oicHearingType = hearing.oicHearingType,
        adjudicator = hearing.getAdjudicator(),
        commentText = hearing.hearingOutcome?.code.toString(),
      ),
    )
  }

  private fun removeOicHearingDetails(reportedAdjudication: ReportedAdjudication) {
    val hearing = reportedAdjudication.getHearing()
    legacySyncService.amendHearing(
      adjudicationNumber = reportedAdjudication.chargeNumber,
      oicHearingId = hearing.oicHearingId,
      oicHearingRequest = OicHearingRequest(
        dateTimeOfHearing = hearing.dateTimeOfHearing,
        hearingLocationId = hearing.locationId,
        oicHearingType = hearing.oicHearingType,
      ),
    )
  }

  companion object {
    fun HearingOutcome?.hearingOutcomeExists() = this ?: throw EntityNotFoundException("outcome not found for hearing")

    fun HearingOutcomeCode.updateOicHearingDetails(): Boolean = listOf(HearingOutcomeCode.REFER_GOV, HearingOutcomeCode.REFER_INAD, HearingOutcomeCode.ADJOURN).contains(this)

    fun ReportedAdjudication.latestOutcomeIsAdjourn() {
      val latest = this.latestHearingOutcome()
      if (latest.code != HearingOutcomeCode.ADJOURN) {
        throw ValidationException("latest outcome is not an adjourn")
      }
    }

    fun ReportedAdjudication.latestHearingOutcome(): HearingOutcome =
      this.getHearing().hearingOutcome ?: throw EntityNotFoundException("outcome not found for hearing")
  }

  fun HearingOutcome.isLatestSameAsAmendRequest(outcomeCodeToAmend: HearingOutcomeCode): HearingOutcome {
    if (this.code != outcomeCodeToAmend) throw ValidationException("latest outcome is not of same type")
    return this
  }

  fun HearingOutcomeCode.shouldRecalculateStatus() = this == HearingOutcomeCode.ADJOURN

  fun Hearing.validateCanRefer(hearingOutcomeCode: HearingOutcomeCode) {
    val exceptionMsg = "hearing type ${this.oicHearingType} can not $hearingOutcomeCode"
    when (hearingOutcomeCode) {
      HearingOutcomeCode.REFER_INAD -> if (OicHearingType.inadTypes().contains(this.oicHearingType)) throw ValidationException(exceptionMsg)
      HearingOutcomeCode.REFER_GOV -> if (OicHearingType.govTypes().contains(this.oicHearingType)) throw ValidationException(exceptionMsg)
      else -> {}
    }
  }
}
