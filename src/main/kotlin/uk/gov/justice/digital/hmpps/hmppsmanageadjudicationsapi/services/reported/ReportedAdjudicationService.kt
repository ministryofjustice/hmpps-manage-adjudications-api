package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication.Companion.getOutcomeHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDateTime

@Transactional
@Service
class ReportedAdjudicationService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  authenticationFacade,
) {
  companion object {
    const val TELEMETRY_EVENT = "ReportedAdjudicationStatusEvent"
  }

  fun getReportedAdjudicationDetails(chargeNumber: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return reportedAdjudication.toDto(
      offenceCodeLookup = offenceCodeLookup,
      activeCaseload = authenticationFacade.activeCaseload,
      consecutiveReportsAvailable = reportedAdjudication.getPunishments().filter { it.consecutiveToChargeNumber != null }.map { it.consecutiveToChargeNumber!! },
      hasLinkedAda = hasLinkedAda(reportedAdjudication),
      linkedChargeNumbers = if (reportedAdjudication.migratedSplitRecord) {
        findMultipleOffenceCharges(
          prisonerNumber = reportedAdjudication.prisonerNumber,
          chargeNumber = chargeNumber,
        )
      } else {
        emptyList()
      },
    )
  }

  fun lastOutcomeHasReferralOutcome(chargeNumber: String): Boolean =
    findByChargeNumber(chargeNumber).getOutcomeHistory().lastOrNull()?.outcome?.referralOutcome != null

  fun setStatus(chargeNumber: String, status: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null): ReportedAdjudicationDto {
    val username = if (status == ReportedAdjudicationStatus.AWAITING_REVIEW) null else authenticationFacade.currentUsername
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val reportedAdjudicationToReturn = reportedAdjudication.let {
      it.transition(to = status, reason = statusReason, details = statusDetails, reviewUserId = username)
      saveToDto(it)
    }

    telemetryClient.trackEvent(
      TELEMETRY_EVENT,
      mapOf(
        "chargeNumber" to reportedAdjudication.chargeNumber,
        "agencyId" to reportedAdjudication.originatingAgencyId,
        "status" to status.name,
        "reason" to statusReason,
      ),
      null,
    )

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
