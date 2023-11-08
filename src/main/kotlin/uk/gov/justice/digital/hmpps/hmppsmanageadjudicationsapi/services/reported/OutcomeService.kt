package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus.Companion.validateTransition
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getLatestHearingId

@Transactional
@Service
class OutcomeService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val nomisOutcomeService: NomisOutcomeService,
  private val punishmentsService: PunishmentsService,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun createProsecution(
    chargeNumber: String,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    code = OutcomeCode.PROSECUTION,
  ).also {
    it.hearingIdActioned = it.hearings.getLatestHearingId()
  }

  fun createReferGov(
    chargeNumber: String,
    details: String,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    details = details,
    code = OutcomeCode.REFER_GOV,
  )

  fun createDismissed(
    chargeNumber: String,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    code = OutcomeCode.DISMISSED,
    details = details,
    validate = validate,
  )

  fun createNotProceed(
    chargeNumber: String,
    reason: NotProceedReason,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    code = OutcomeCode.NOT_PROCEED,
    reason = reason,
    details = details,
    validate = validate,
  ).also {
    it.hearingIdActioned = it.hearings.getLatestHearingId()
  }

  fun createReferral(
    chargeNumber: String,
    code: OutcomeCode,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    code = code.validateReferral(),
    details = details,
    validate = validate,
  )

  fun createChargeProved(
    chargeNumber: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    chargeNumber = chargeNumber,
    code = OutcomeCode.CHARGE_PROVED,
    validate = validate,
  )

  fun createQuashed(
    chargeNumber: String,
    reason: QuashedReason,
    details: String,
  ): ReportedAdjudicationDto {
    findByChargeNumber(chargeNumber).latestOutcome().canQuash()

    return createOutcome(
      chargeNumber = chargeNumber,
      code = OutcomeCode.QUASHED,
      details = details,
      quashedReason = reason,
    )
  }

  fun amendOutcomeViaApi(
    chargeNumber: String,
    details: String,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val isReferralOutcome = if (reportedAdjudication.hearings.isNotEmpty()) getOutcomes(chargeNumber).isLatestReferralOutcome() else false
    reportedAdjudication.latestOutcome().canAmendViaApi(
      hasHearings = reportedAdjudication.hearings.isNotEmpty(),
      isReferralOutcome = isReferralOutcome,
      outcomeReferGovReferral = reportedAdjudication.previousOutcomeIsReferGovReferral(),
    )

    return amendOutcome(
      chargeNumber = chargeNumber,
      details = details,
      reason = reason,
      quashedReason = quashedReason,
    )
  }

  fun amendOutcomeViaService(
    chargeNumber: String,
    outcomeCodeToAmend: OutcomeCode,
    details: String? = null,
    notProceedReason: NotProceedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    reportedAdjudication.latestOutcome()
      .canAmendViaService(reportedAdjudication.hearings.isNotEmpty())
      .isLatestSameAsAmendRequest(outcomeCodeToAmend)

    return amendOutcome(
      chargeNumber = chargeNumber,
      details = details,
      reason = notProceedReason,
    )
  }

  private fun createOutcome(
    chargeNumber: String,
    code: OutcomeCode,
    details: String? = null,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      if (validate) it.status.validateTransition(code.status)
      it.status = code.status
    }

    if (validate && reportedAdjudication.lastOutcomeIsRefer()) {
      reportedAdjudication.latestOutcome()!!.code.validateReferralTransition(code)
    }

    val outcomeToCreate = Outcome(
      code = code,
      details = details,
      reason = reason,
      quashedReason = quashedReason,
    ).also {
      it.oicHearingId = nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = chargeNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = it,
      )
    }

    reportedAdjudication.addOutcome(outcomeToCreate)

    return saveToDto(reportedAdjudication)
  }

  private fun amendOutcome(
    chargeNumber: String,
    details: String? = null,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    reportedAdjudication.latestOutcome()!!.let {
      when (it.code) {
        OutcomeCode.NOT_PROCEED -> {
          details?.let { updated -> it.details = updated }
          reason?.let { updated -> it.reason = updated }
        }

        OutcomeCode.QUASHED -> {
          details?.let { updated -> it.details = updated }
          quashedReason?.let { updated -> it.quashedReason = updated }
        }

        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV, OutcomeCode.DISMISSED -> details?.let { updated -> it.details = updated }
        else -> {}
      }
    }

    nomisOutcomeService.amendHearingResultIfApplicable(
      adjudicationNumber = chargeNumber,
      hearing = reportedAdjudication.getLatestHearing(),
      outcome = reportedAdjudication.latestOutcome()!!,
    )

    return saveToDto(reportedAdjudication)
  }

  fun deleteOutcome(chargeNumber: String, id: Long? = null): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    val outcomeToDelete = when (id) {
      null -> {
        reportedAdjudication.latestOutcome()?.canDelete(
          hasHearings = reportedAdjudication.hearings.isNotEmpty(),
          outcomeReferGovReferral = reportedAdjudication.previousOutcomeIsReferGovReferral(),
        ) ?: throw EntityNotFoundException("Outcome not found for $chargeNumber")
      }
      else -> reportedAdjudication.getOutcome(id)
    }.also {
      it.deleted = true
    }

    reportedAdjudication.calculateStatus()

    when (outcomeToDelete.code) {
      OutcomeCode.CHARGE_PROVED -> {
        if (this.isLinkedToReport(chargeNumber, PunishmentType.additionalDays())) throw ValidationException("Unable to remove: $chargeNumber is linked to another report")
        reportedAdjudication.clearPunishments()
        reportedAdjudication.punishmentComments.clear()
      }
      OutcomeCode.QUASHED -> punishmentsService.removeQuashedFinding(reportedAdjudication)
      else -> {}
    }

    nomisOutcomeService.deleteHearingResultIfApplicable(
      adjudicationNumber = chargeNumber,
      hearing = reportedAdjudication.getLatestHearing(),
      outcome = outcomeToDelete,
    )

    return saveToDto(reportedAdjudication)
  }

  fun getOutcomes(chargeNumber: String): List<CombinedOutcomeDto> {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    return reportedAdjudication.getOutcomes().createCombinedOutcomes(false)
  }

  fun getLatestOutcome(chargeNumber: String): Outcome? = findByChargeNumber(chargeNumber).latestOutcome()

  private fun ReportedAdjudication.previousOutcomeIsReferGovReferral(): Boolean {
    val outcomeHistory = this.getOutcomeHistory()
    val indexOfReferralOutcome = outcomeHistory.indexOfLast { it.outcome?.referralOutcome?.code == OutcomeCode.REFER_GOV }
    return indexOfReferralOutcome != -1 && indexOfReferralOutcome == outcomeHistory.size - 2
  }

  companion object {
    fun ReportedAdjudication.latestOutcome(): Outcome? = this.getOutcomes().maxByOrNull { it.getCreatedDateTime()!! }

    fun ReportedAdjudication.getOutcome(id: Long) =
      this.getOutcomes().firstOrNull { it.id == id } ?: throw EntityNotFoundException("Outcome not found for $id")

    fun OutcomeCode.validateReferralTransition(to: OutcomeCode) {
      if (!this.canTransitionTo(to)) {
        throw ValidationException("Invalid referral transition")
      }
    }

    fun Outcome.canDelete(hasHearings: Boolean, outcomeReferGovReferral: Boolean): Outcome {
      val acceptableCode = if (!hasHearings || outcomeReferGovReferral) OutcomeCode.NOT_PROCEED else OutcomeCode.QUASHED
      if (acceptableCode != this.code) throw ValidationException("Unable to delete via api - DEL/outcome")

      return this
    }

    fun ReportedAdjudication.lastOutcomeIsRefer() =
      OutcomeCode.referrals().contains(this.getOutcomes().maxByOrNull { it.getCreatedDateTime()!! }?.code)

    fun Outcome?.canQuash() {
      if (this?.code != OutcomeCode.CHARGE_PROVED) {
        throw ValidationException("unable to quash this outcome")
      }
    }

    fun Outcome?.canAmendViaApi(hasHearings: Boolean, isReferralOutcome: Boolean, outcomeReferGovReferral: Boolean) {
      if ((hasHearings && !isReferralOutcome && !outcomeReferGovReferral && OutcomeCode.QUASHED != this?.code) ||
        (hasHearings && !isReferralOutcome && outcomeReferGovReferral && OutcomeCode.NOT_PROCEED != this?.code) ||
        (hasHearings && isReferralOutcome && !listOf(OutcomeCode.NOT_PROCEED, OutcomeCode.REFER_GOV).contains(this?.code)) ||
        (!hasHearings && !listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED).contains(this?.code))
      ) {
        throw ValidationException("unable to amend this outcome")
      }
    }

    fun Outcome?.canAmendViaService(hasHearings: Boolean): Outcome {
      this ?: throw EntityNotFoundException("no latest outcome to amend")

      if (listOf(OutcomeCode.QUASHED, OutcomeCode.SCHEDULE_HEARING, OutcomeCode.CHARGE_PROVED).any { it == this.code } ||
        (!hasHearings && listOf(OutcomeCode.NOT_PROCEED, OutcomeCode.REFER_POLICE).any { it == this.code })
      ) {
        throw ValidationException("unable to amend via this function")
      }

      return this
    }

    fun Outcome.isLatestSameAsAmendRequest(outcomeCodeToAmend: OutcomeCode) {
      if (this.code != outcomeCodeToAmend) throw ValidationException("latest outcome is not of same type")
    }

    fun List<CombinedOutcomeDto>.isLatestReferralOutcome() =
      this.lastOrNull()?.referralOutcome != null
  }
}
