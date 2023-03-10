package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

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
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional
import javax.validation.ValidationException

@Transactional
@Service
class OutcomeService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
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
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.DISMISSED,
    details = details,
  )

  fun createNotProceed(
    adjudicationNumber: Long,
    reason: NotProceedReason,
    details: String,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.NOT_PROCEED,
    reason = reason,
    details = details,
  )

  fun createReferral(
    adjudicationNumber: Long,
    code: OutcomeCode,
    details: String,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = code.validateReferral(),
    details = details,
  )

  fun createChargeProved(
    adjudicationNumber: Long,
    amount: Double? = null,
    caution: Boolean,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.CHARGE_PROVED,
    amount = amount,
    caution = caution
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
      quashedReason = reason
    )
  }

  fun amendOutcomeViaApi(
    adjudicationNumber: Long,
    details: String,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    reportedAdjudication.latestOutcome().canAmendViaApi(reportedAdjudication.hearings.isNotEmpty())

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
    amount: Double? = null,
    caution: Boolean? = null,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    reportedAdjudication.latestOutcome()
      .canAmendViaService(reportedAdjudication.hearings.isNotEmpty())
      .isLatestSameAsAmendRequest(outcomeCodeToAmend)

    return amendOutcome(
      adjudicationNumber = adjudicationNumber,
      details = details,
      reason = notProceedReason,
      amount = amount,
      caution = caution,
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
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateTransition(code.status)
      it.status = code.status
    }

    if (reportedAdjudication.lastOutcomeIsRefer())
      reportedAdjudication.latestOutcome()!!.code.validateReferralTransition(code)

    reportedAdjudication.outcomes.add(
      Outcome(
        code = code,
        details = details,
        reason = reason,
        amount = amount,
        caution = caution,
        quashedReason = quashedReason,
      )
    )

    return saveToDto(reportedAdjudication)
  }

  private fun amendOutcome(
    adjudicationNumber: Long,
    details: String? = null,
    reason: NotProceedReason? = null,
    quashedReason: QuashedReason? = null,
    amount: Double? = null,
    caution: Boolean? = null,
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
        OutcomeCode.CHARGE_PROVED -> {
          amount?.let { updated -> it.amount = updated }
          caution?.let { updated -> it.caution = updated }
        }
        OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.DISMISSED -> details?.let { updated -> it.details = updated }
        else -> {}
      }
    }
    return saveToDto(reportedAdjudication)
  }

  fun deleteOutcome(adjudicationNumber: Long, id: Long? = null): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val outcomeToDelete = when (id) {
      null -> reportedAdjudication.latestOutcome()?.canDelete(reportedAdjudication.hearings.isNotEmpty()) ?: throw EntityNotFoundException("Outcome not found for $adjudicationNumber")
      else -> reportedAdjudication.getOutcome(id)
    }

    reportedAdjudication.outcomes.remove(outcomeToDelete)
    reportedAdjudication.calculateStatus()

    return saveToDto(reportedAdjudication)
  }

  fun getOutcomes(adjudicationNumber: Long): List<CombinedOutcomeDto> {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    return reportedAdjudication.outcomes.createCombinedOutcomes()
  }

  fun getLatestOutcome(adjudicationNumber: Long): Outcome? = findByAdjudicationNumber(adjudicationNumber).latestOutcome()

  companion object {
    fun ReportedAdjudication.latestOutcome(): Outcome? = this.outcomes.maxByOrNull { it.createDateTime!! }

    fun ReportedAdjudication.getOutcome(id: Long) =
      this.outcomes.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Outcome not found for $id")

    fun OutcomeCode.validateReferralTransition(to: OutcomeCode) {
      if (!this.canTransitionTo(to))
        throw ValidationException("Invalid referral transition")
    }

    fun Outcome.canDelete(hasHearings: Boolean): Outcome {
      val acceptableItems = if (!hasHearings) listOf(OutcomeCode.NOT_PROCEED) else listOf(OutcomeCode.QUASHED)
      if (acceptableItems.none { it == this.code }) throw ValidationException("Unable to delete via api - DEL/outcome")

      return this
    }

    fun ReportedAdjudication.lastOutcomeIsRefer() =
      OutcomeCode.referrals().contains(this.outcomes.maxByOrNull { it.createDateTime!! }?.code)

    fun Outcome?.canQuash() {
      if (this?.code != OutcomeCode.CHARGE_PROVED)
        throw ValidationException("unable to quash this outcome")
    }

    fun Outcome?.canAmendViaApi(hasHearings: Boolean) {
      if ((hasHearings && this?.code != OutcomeCode.QUASHED) ||
        (!hasHearings && !listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED).contains(this?.code))
      )
        throw ValidationException("unable to amend this outcome")
    }

    fun Outcome?.canAmendViaService(hasHearings: Boolean): Outcome {
      this ?: throw EntityNotFoundException("no latest outcome to amend")

      if (listOf(OutcomeCode.QUASHED, OutcomeCode.SCHEDULE_HEARING).any { it == this.code } ||
        (!hasHearings && listOf(OutcomeCode.NOT_PROCEED, OutcomeCode.REFER_POLICE).any { it == this.code })
      )
        throw ValidationException("unable to amend via this function")

      return this
    }

    fun Outcome.isLatestSameAsAmendRequest(outcomeCodeToAmend: OutcomeCode) {
      if (this.code != outcomeCodeToAmend) throw ValidationException("latest outcome is not of same type")
    }
  }
}
