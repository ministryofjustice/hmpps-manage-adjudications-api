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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus.Companion.validateTransition
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

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
    adjudicationNumber: Long,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.PROSECUTION,
  )

  fun createDismissed(
    adjudicationNumber: Long,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.DISMISSED,
    details = details,
    validate = validate,
  )

  fun createNotProceed(
    adjudicationNumber: Long,
    reason: NotProceedReason,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.NOT_PROCEED,
    reason = reason,
    details = details,
    validate = validate,
  )

  fun createReferral(
    adjudicationNumber: Long,
    code: OutcomeCode,
    details: String,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = code.validateReferral(),
    details = details,
    validate = validate,
  )

  fun createChargeProved(
    adjudicationNumber: Long,
    amount: Double? = null,
    caution: Boolean,
    validate: Boolean = true,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.CHARGE_PROVED,
    amount = amount,
    caution = caution,
    validate = validate,
  )

  fun createQuashed(
    adjudicationNumber: Long,
    reason: QuashedReason,
    details: String,
  ): ReportedAdjudicationDto {
    findByAdjudicationNumber(adjudicationNumber).latestOutcome().canQuash()

    return createOutcome(
      adjudicationNumber = adjudicationNumber,
      code = OutcomeCode.QUASHED,
      details = details,
      quashedReason = reason,
    )
  }

  fun amendOutcomeViaApi(
    adjudicationNumber: Long,
    details: String,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val isReferralOutcome = if (reportedAdjudication.hearings.isNotEmpty()) getOutcomes(adjudicationNumber).isLatestReferralOutcome() else false
    reportedAdjudication.latestOutcome().canAmendViaApi(reportedAdjudication.hearings.isNotEmpty(), isReferralOutcome)

    return amendOutcome(
      adjudicationNumber = adjudicationNumber,
      details = details,
      reason = reason,
      quashedReason = quashedReason,
    )
  }

  fun amendOutcomeViaService(
    adjudicationNumber: Long,
    outcomeCodeToAmend: OutcomeCode,
    details: String? = null,
    notProceedReason: NotProceedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    reportedAdjudication.latestOutcome()
      .canAmendViaService(reportedAdjudication.hearings.isNotEmpty())
      .isLatestSameAsAmendRequest(outcomeCodeToAmend)

    return amendOutcome(
      adjudicationNumber = adjudicationNumber,
      details = details,
      reason = notProceedReason,
    )
  }

  private fun createOutcome(
    adjudicationNumber: Long,
    code: OutcomeCode,
    details: String? = null,
    reason: NotProceedReason? = null,
    amount: Double? = null,
    caution: Boolean? = null,
    quashedReason: QuashedReason? = null,
    validate: Boolean = true,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
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
        adjudicationNumber = adjudicationNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = it,
      )
    }

    reportedAdjudication.addOutcome(outcomeToCreate)

    if (outcomeToCreate.code == OutcomeCode.CHARGE_PROVED) {
      punishmentsService.createPunishmentsFromChargeProvedIfApplicable(
        reportedAdjudication = reportedAdjudication,
        caution = caution!!,
        amount = amount,
      )
    }

    return saveToDto(reportedAdjudication)
  }

  private fun amendOutcome(
    adjudicationNumber: Long,
    details: String? = null,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

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

        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.DISMISSED -> details?.let { updated -> it.details = updated }
        else -> {}
      }
    }

    nomisOutcomeService.amendHearingResultIfApplicable(
      adjudicationNumber = adjudicationNumber,
      hearing = reportedAdjudication.getLatestHearing(),
      outcome = reportedAdjudication.latestOutcome()!!,
    )

    return saveToDto(reportedAdjudication)
  }

  fun deleteOutcome(adjudicationNumber: Long, id: Long? = null): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    val outcomeToDelete = when (id) {
      null -> reportedAdjudication.latestOutcome()?.canDelete(reportedAdjudication.hearings.isNotEmpty()) ?: throw EntityNotFoundException("Outcome not found for $adjudicationNumber")
      else -> reportedAdjudication.getOutcome(id)
    }.also {
      it.deleted = true
    }

    reportedAdjudication.calculateStatus()

    when (outcomeToDelete.code) {
      OutcomeCode.CHARGE_PROVED -> {
        reportedAdjudication.clearPunishments()
        reportedAdjudication.punishmentComments.clear()
      }
      OutcomeCode.QUASHED -> punishmentsService.removeQuashedFinding(reportedAdjudication)
      else -> {}
    }

    nomisOutcomeService.deleteHearingResultIfApplicable(
      adjudicationNumber = adjudicationNumber,
      hearing = reportedAdjudication.getLatestHearing(),
      outcome = outcomeToDelete,
    )

    return saveToDto(reportedAdjudication)
  }

  fun getOutcomes(adjudicationNumber: Long): List<CombinedOutcomeDto> {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    return reportedAdjudication.getOutcomes().createCombinedOutcomes(reportedAdjudication.getPunishments())
  }

  fun getLatestOutcome(adjudicationNumber: Long): Outcome? = findByAdjudicationNumber(adjudicationNumber).latestOutcome()

  companion object {
    fun ReportedAdjudication.latestOutcome(): Outcome? = this.getOutcomes().maxByOrNull { it.createDateTime!! }

    fun ReportedAdjudication.getOutcome(id: Long) =
      this.getOutcomes().firstOrNull { it.id == id } ?: throw EntityNotFoundException("Outcome not found for $id")

    fun OutcomeCode.validateReferralTransition(to: OutcomeCode) {
      if (!this.canTransitionTo(to)) {
        throw ValidationException("Invalid referral transition")
      }
    }

    fun Outcome.canDelete(hasHearings: Boolean): Outcome {
      val acceptableItems = if (!hasHearings) listOf(OutcomeCode.NOT_PROCEED) else listOf(OutcomeCode.QUASHED)
      if (acceptableItems.none { it == this.code }) throw ValidationException("Unable to delete via api - DEL/outcome")

      return this
    }

    fun ReportedAdjudication.lastOutcomeIsRefer() =
      OutcomeCode.referrals().contains(this.getOutcomes().maxByOrNull { it.createDateTime!! }?.code)

    fun Outcome?.canQuash() {
      if (this?.code != OutcomeCode.CHARGE_PROVED) {
        throw ValidationException("unable to quash this outcome")
      }
    }

    fun Outcome?.canAmendViaApi(hasHearings: Boolean, isReferralOutcome: Boolean) {
      if ((hasHearings && !isReferralOutcome && this?.code != OutcomeCode.QUASHED) ||
        (hasHearings && isReferralOutcome && this?.code != OutcomeCode.NOT_PROCEED) ||
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
