package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
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

  fun createOutcome(
    adjudicationNumber: Long,
    code: OutcomeCode,
    details: String? = null,
    reason: NotProceedReason? = null
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateTransition(code.status)
      it.status = code.status
    }

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

  fun deleteOutcome(adjudicationNumber: Long, id: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    val outcomeToDelete = reportedAdjudication.getOutcome(id)

    reportedAdjudication.outcomes.remove(outcomeToDelete)

    return saveToDto(reportedAdjudication)
  }

  fun getOutcomes(adjudicationNumber: Long): List<CombinedOutcomeDto> {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
    return reportedAdjudication.outcomes.createCombinedOutcomes()
  }

  fun updateReferral(adjudicationNumber: Long, code: OutcomeCode, details: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    reportedAdjudication.getReferral(code).also {
      it.details = details
    }

    return saveToDto(reportedAdjudication)
  }

  companion object {
    private fun validateDetails(details: String?) = details ?: throw ValidationException("details are required")

    fun ReportedAdjudication.getReferral(code: OutcomeCode) =
      this.outcomes.filter { it.code == code }.sortedByDescending { it.createDateTime }.firstOrNull() ?: throw EntityNotFoundException("Referral not found for ${this.reportNumber}")

    fun ReportedAdjudication.getOutcome(id: Long) =
      this.outcomes.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Outcome not found for $id")
  }
}
