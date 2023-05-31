package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DisIssueHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDateTime

@Transactional
@Service
class ReportedAdjudicationService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val prisonApiGateway: PrisonApiGateway,
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

  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)

    return reportedAdjudication.toDto()
  }

  fun lastOutcomeHasReferralOutcome(adjudicationNumber: Long): Boolean =
    findByAdjudicationNumber(adjudicationNumber).getOutcomeHistory().lastOrNull()?.outcome?.referralOutcome != null

  fun setStatus(adjudicationNumber: Long, status: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null): ReportedAdjudicationDto {
    if (status == ReportedAdjudicationStatus.ACCEPTED) throw ValidationException("ACCEPTED is deprecated use UNSCHEDULED")

    val username = if (status == ReportedAdjudicationStatus.AWAITING_REVIEW) null else authenticationFacade.currentUsername
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber)
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
        "reportNumber" to reportedAdjudication.reportNumber.toString(),
        "agencyId" to reportedAdjudication.agencyId,
        "status" to status.name,
        "reason" to statusReason,
      ),
      null,
    )

    return reportedAdjudicationToReturn
  }

  fun setIssued(adjudicationNumber: Long, dateTimeOfIssue: LocalDateTime): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
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

  private fun saveToPrisonApi(reportedAdjudication: ReportedAdjudication) {
    prisonApiGateway.publishAdjudication(
      AdjudicationDetailsToPublish(
        offenderNo = reportedAdjudication.prisonerNumber,
        adjudicationNumber = reportedAdjudication.reportNumber,
        reporterName = reportedAdjudication.createdByUserId
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creator name not set for reported adjudication number ${reportedAdjudication.reportNumber}",
          ),
        reportedDateTime = reportedAdjudication.createDateTime
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creation time not set for reported adjudication number ${reportedAdjudication.reportNumber}",
          ),
        agencyId = reportedAdjudication.agencyId,
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
      return offenceDetails?.map { offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(it.offenceCode, isYouthOffender) }
        ?: emptyList()
    }
    return offenceDetails?.map { offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(it.offenceCode, isYouthOffender) }
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
