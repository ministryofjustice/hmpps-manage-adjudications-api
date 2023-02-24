package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
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
    details: String,
  ): ReportedAdjudicationDto = createOutcome(
    adjudicationNumber = adjudicationNumber,
    code = OutcomeCode.PROSECUTION,
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

  private fun createOutcome(
    adjudicationNumber: Long,
    code: OutcomeCode,
    details: String? = null,
    reason: NotProceedReason? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateTransition(code.status)
      it.status = code.status
    }

    if (reportedAdjudication.lastOutcomeIsRefer())
      reportedAdjudication.latestOutcome()!!.code.validateReferralTransition(code)

    when (code) {
      OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD -> validateDetails(details)
      OutcomeCode.NOT_PROCEED -> {
        validateDetails(details)
        reason ?: throw ValidationException("a reason is required")
      }

      else -> {} // TODO(" currently referral outcome PROSECUTION, SCHEDULE_HEARING, nothing to do at present")
    }

    reportedAdjudication.outcomes.add(
      Outcome(
        code = code,
        details = details,
        reason = reason,
      )
    )

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

  companion object {
    private fun validateDetails(details: String?) = details ?: throw ValidationException("details are required")

    fun ReportedAdjudication.latestOutcome(): Outcome? = this.outcomes.maxByOrNull { it.createDateTime!! }

    fun ReportedAdjudication.getOutcome(id: Long) =
      this.outcomes.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Outcome not found for $id")

    fun OutcomeCode.validateReferralTransition(to: OutcomeCode) {
      if (!this.canTransitionTo(to))
        throw ValidationException("Invalid referral transition")
    }

    fun Outcome.canDelete(hasHearings: Boolean): Outcome {
      val acceptableItems = if (!hasHearings) listOf(OutcomeCode.NOT_PROCEED) else emptyList()
      if (acceptableItems.none { it == this.code }) throw ValidationException("Unable to delete via api - DEL/outcome")

      return this
    }

    fun ReportedAdjudication.calculateStatus() {
      this.status = when (this.outcomes.isEmpty()) {
        true ->
          when (this.hearings.isEmpty()) {
            true -> ReportedAdjudicationStatus.UNSCHEDULED
            false -> ReportedAdjudicationStatus.SCHEDULED
          }

        false ->
          // TODO review at later point.  for now, it can just be the previous outcome status
          this.outcomes.sortedByDescending { it.createDateTime }.first().code.status
      }
    }

    fun ReportedAdjudication.lastOutcomeIsRefer() =
      OutcomeCode.referrals().contains(this.outcomes.maxByOrNull { it.createDateTime!! }?.code)
  }
}
