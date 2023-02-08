package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus.Companion.validateTransition
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
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

  fun createOutcome(adjudicationNumber: Long, code: OutcomeCode, details: String? = null, reason: NotProceedReason? = null): ReportedAdjudicationDto {
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

  fun deleteOutcome(adjudicationNumber: Long): ReportedAdjudicationDto {
    TODO("implement me")
  }

  fun isReferral(adjudicationNumber: Long): Boolean {
    TODO("implement me")
  }

  companion object {
    private fun validateDetails(details: String?) = details ?: throw ValidationException("details are required")
  }
}
