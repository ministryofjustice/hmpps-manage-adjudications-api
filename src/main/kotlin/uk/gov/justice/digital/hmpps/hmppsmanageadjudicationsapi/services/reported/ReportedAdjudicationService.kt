package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDateTime

@Transactional
@Service
class ReportedAdjudicationService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val legacySyncService: LegacySyncService,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
  private val telemetryClient: TelemetryClient,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {
  companion object {
    const val TELEMETRY_EVENT = "ReportedAdjudicationStatusEvent"
  }

  fun getReportedAdjudicationDetails(chargeNumber: String): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return reportedAdjudication.toDto(
      activeCaseload = authenticationFacade.activeCaseload,
      consecutiveReportsAvailable = reportedAdjudication.getConsecutiveReportsAvailable(),
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
    if (status.isAccepted()) {
      saveToPrisonApi(reportedAdjudication)
    }

    telemetryClient.trackEvent(
      TELEMETRY_EVENT,
      mapOf(
        "reportNumber" to reportedAdjudication.chargeNumber,
        "agencyId" to reportedAdjudication.originatingAgencyId,
        "status" to status.name,
        "reason" to statusReason,
      ),
      null,
    )

    return reportedAdjudicationToReturn
  }

  fun setIssued(chargeNumber: String, dateTimeOfIssue: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.status.canBeIssuedValidation()
      if (it.dateTimeOfIssue != null) {
        it.disIssueHistory.add(
          DisIssueHistory(
            issuingOfficer = it.issuingOfficer!!,
            dateTimeOfIssue = it.dateTimeOfIssue!!,
          ),
        )
      }
      it.issuingOfficer = authenticationFacade.currentUsername
      it.dateTimeOfIssue = dateTimeOfIssue
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

  @Deprecated("this should be removed once data migration is complete - all reports will be available")
  private fun ReportedAdjudication.getConsecutiveReportsAvailable(): List<String> {
    val consecutiveReportsToFind = this.getPunishments().filter { it.consecutiveChargeNumber != null }.map { it.consecutiveChargeNumber!! }
    if (consecutiveReportsToFind.isNotEmpty()) {
      return findByReportNumberIn(consecutiveReportsToFind).map { it.chargeNumber }
    }

    return emptyList()
  }

  private fun saveToPrisonApi(reportedAdjudication: ReportedAdjudication) {
    legacySyncService.publishAdjudication(
      AdjudicationDetailsToPublish(
        offenderNo = reportedAdjudication.prisonerNumber,
        adjudicationNumber = reportedAdjudication.chargeNumber.toLong(),
        reporterName = reportedAdjudication.createdByUserId
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creator name not set for reported adjudication number ${reportedAdjudication.chargeNumber}",
          ),
        reportedDateTime = reportedAdjudication.createDateTime
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creation time not set for reported adjudication number ${reportedAdjudication.chargeNumber}",
          ),
        agencyId = reportedAdjudication.originatingAgencyId,
        incidentTime = reportedAdjudication.dateTimeOfDiscovery,
        incidentLocationId = reportedAdjudication.locationId,
        statement = reportedAdjudication.statement,
        offenceCodes = getNomisCodes(reportedAdjudication.incidentRoleCode, reportedAdjudication.offenceDetails, reportedAdjudication.isYouthOffender),
        connectedOffenderIds = getAssociatedOffenders(
          associatedPrisonersNumber = reportedAdjudication.incidentRoleAssociatedPrisonersNumber,
        ),
        victimOffenderIds = getVictimOffenders(
          prisonerNumber = reportedAdjudication.prisonerNumber,
          offenceDetails = reportedAdjudication.offenceDetails,
        ),
        victimStaffUsernames = getVictimStaffUsernames(reportedAdjudication.offenceDetails),
      ),
    )
  }

  private fun getNomisCodes(roleCode: String?, offenceDetails: MutableList<ReportedOffence>?, isYouthOffender: Boolean): List<String> {
    if (roleCode != null) { // Null means committed on own
      return offenceDetails?.map { offenceCodeLookupService.getOffenceCode(it.offenceCode, isYouthOffender).getNomisCodeWithOthers() }
        ?: emptyList()
    }
    return offenceDetails?.map { offenceCodeLookupService.getOffenceCode(it.offenceCode, isYouthOffender).getNomisCode() }
      ?: emptyList()
  }

  private fun getAssociatedOffenders(associatedPrisonersNumber: String?): List<String> {
    if (associatedPrisonersNumber == null) {
      return emptyList()
    }
    return listOf(associatedPrisonersNumber)
  }

  private fun getVictimOffenders(prisonerNumber: String, offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.filter { it.victimPrisonersNumber != prisonerNumber }?.mapNotNull { it.victimPrisonersNumber } ?: emptyList()
  }

  private fun getVictimStaffUsernames(offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimStaffUsername } ?: emptyList()
  }
}
