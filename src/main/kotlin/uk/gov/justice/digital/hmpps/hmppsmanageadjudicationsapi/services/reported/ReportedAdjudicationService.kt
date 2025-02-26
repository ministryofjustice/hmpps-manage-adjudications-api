package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.getOutcomeHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDateTime

@Transactional
@Service
class ReportedAdjudicationService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun getReportedAdjudicationDetails(chargeNumber: String, includeActivated: Boolean = false): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val hasLinkedAda = hasLinkedAda(reportedAdjudication)
    val consecutiveReportsAvailable = reportedAdjudication.getPunishments().filter { it.consecutiveToChargeNumber != null }.map { it.consecutiveToChargeNumber!! }
    return reportedAdjudication.toDto(
      offenceCodeLookupService = offenceCodeLookupService,
      activeCaseload = authenticationFacade.activeCaseload,
      consecutiveReportsAvailable = consecutiveReportsAvailable,
      hasLinkedAda = hasLinkedAda,
      linkedChargeNumbers = if (reportedAdjudication.migratedSplitRecord) {
        findMultipleOffenceCharges(
          prisonerNumber = reportedAdjudication.prisonerNumber,
          chargeNumber = chargeNumber,
        )
      } else {
        emptyList()
      },
    ).also {
      if (includeActivated) {
        it.punishments.addAll(
          getActivatedPunishments(chargeNumber = chargeNumber)
            .map { activated ->
              activated.second.toDto(
                hasLinkedAda = hasLinkedAda,
                consecutiveReportsAvailable = consecutiveReportsAvailable,
                activatedFrom = activated.first,
              )
            },
        )
      }
    }
  }

  fun lastOutcomeHasReferralOutcome(chargeNumber: String): Boolean = findByChargeNumber(chargeNumber).getOutcomeHistory().lastOrNull()?.outcome?.referralOutcome != null

  fun setStatus(chargeNumber: String, status: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null): ReportedAdjudicationDto {
    val username = if (status == ReportedAdjudicationStatus.AWAITING_REVIEW) null else authenticationFacade.currentUsername
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val reportedAdjudicationToReturn = reportedAdjudication.let {
      it.transition(to = status, reason = statusReason, details = statusDetails, reviewUserId = username)
      saveToDto(it)
    }
    return reportedAdjudicationToReturn
  }

  fun setIssued(chargeNumber: String, dateTimeOfIssue: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also { report ->
      report.dateTimeOfIssue?.let {
        report.disIssueHistory.add(
          DisIssueHistory(
            issuingOfficer = report.issuingOfficer!!,
            dateTimeOfIssue = it,
          ),
        )
      }
      report.issuingOfficer = authenticationFacade.currentUsername
      report.dateTimeOfIssue = dateTimeOfIssue
    }

    return saveToDto(reportedAdjudication)
  }

  fun setCreatedOnBehalfOf(chargeNumber: String, createdOnBehalfOfOfficer: String, createdOnBehalfOfReason: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.createdOnBehalfOfOfficer = createdOnBehalfOfOfficer
      it.createdOnBehalfOfReason = createdOnBehalfOfReason
    }

    return saveToDto(reportedAdjudication)
  }
}
