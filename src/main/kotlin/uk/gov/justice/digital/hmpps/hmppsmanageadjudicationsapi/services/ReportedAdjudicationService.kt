package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
class ReportedAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val prisonApiGateway: PrisonApiGateway,
  val offenceCodeLookupService: OffenceCodeLookupService,
  val authenticationFacade: AuthenticationFacade
) {
  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
    fun statuses(status: Optional<ReportedAdjudicationStatus>): List<ReportedAdjudicationStatus> = status.map { listOf(it) }.orElse(ReportedAdjudicationStatus.values().toList())
  }

  @Transactional
  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)

    return reportedAdjudication?.toDto(offenceCodeLookupService) ?: throwEntityNotFoundException(adjudicationNumber)
  }

  @Transactional
  fun getAllReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, status: Optional<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
        agencyId, reportsFrom(startDate), reportsTo(endDate), statuses(status), pageable
      )
    return reportedAdjudicationsPage.map { it.toDto(offenceCodeLookupService) }
  }

  @Transactional
  fun getMyReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, status: Optional<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername

    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
        username!!, agencyId, reportsFrom(startDate), reportsTo(endDate), statuses(status), pageable
      )
    return reportedAdjudicationsPage.map { it.toDto(offenceCodeLookupService) }
  }

  fun createDraftFromReportedAdjudication(adjudicationNumber: Long): DraftAdjudicationDto {
    val foundReportedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)

    val reportedAdjudication = foundReportedAdjudication ?: throwEntityNotFoundException(adjudicationNumber)

    val draftAdjudication = DraftAdjudication(
      reportNumber = reportedAdjudication.reportNumber,
      reportByUserId = reportedAdjudication.createdByUserId,
      prisonerNumber = reportedAdjudication.prisonerNumber,
      agencyId = reportedAdjudication.agencyId,
      incidentDetails = IncidentDetails(
        locationId = reportedAdjudication.locationId,
        dateTimeOfIncident = reportedAdjudication.dateTimeOfIncident,
        handoverDeadline = reportedAdjudication.handoverDeadline
      ),
      incidentRole = IncidentRole(
        roleCode = reportedAdjudication.incidentRoleCode,
        associatedPrisonersNumber = reportedAdjudication.incidentRoleAssociatedPrisonersNumber,
        associatedPrisonersName = reportedAdjudication.incidentRoleAssociatedPrisonersName,
      ),
      offenceDetails = toDraftOffence(reportedAdjudication.offenceDetails),
      incidentStatement = IncidentStatement(
        statement = reportedAdjudication.statement,
        completed = true
      ),
      isYouthOffender = reportedAdjudication.isYouthOffender
    )

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto(offenceCodeLookupService)
  }

  private fun toDraftOffence(offences: MutableList<ReportedOffence>?): MutableList<Offence> {
    return (offences ?: mutableListOf()).map { offence ->
      Offence(
        offenceCode = offence.offenceCode,
        victimPrisonersNumber = offence.victimPrisonersNumber,
        victimStaffUsername = offence.victimStaffUsername,
        victimOtherPersonsName = offence.victimOtherPersonsName,
      )
    }.toMutableList()
  }

  @Transactional
  fun setStatus(adjudicationNumber: Long, status: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null): ReportedAdjudicationDto {
    val username = if (status == ReportedAdjudicationStatus.AWAITING_REVIEW) null else authenticationFacade.currentUsername
    val reportedAdjudication = reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)
      ?: throw EntityNotFoundException("ReportedAdjudication not found for reported adjudication number $adjudicationNumber")
    val reportedAdjudicationToReturn = reportedAdjudication.let {
      it.transition(status, username, statusReason, statusDetails)
      reportedAdjudicationRepository.save(it).toDto(this.offenceCodeLookupService)
    }
    if (status.isAccepted()) {
      saveToPrisonApi(reportedAdjudication)
    }
    return reportedAdjudicationToReturn
  }

  private fun saveToPrisonApi(reportedAdjudication: ReportedAdjudication) {
    prisonApiGateway.publishAdjudication(
      AdjudicationDetailsToPublish(
        offenderNo = reportedAdjudication.prisonerNumber,
        adjudicationNumber = reportedAdjudication.reportNumber,
        bookingId = reportedAdjudication.bookingId,
        reporterName = reportedAdjudication.createdByUserId
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creator name not set for reported adjudication number ${reportedAdjudication.reportNumber}"
          ),
        reportedDateTime = reportedAdjudication.createDateTime
          ?: throw EntityNotFoundException(
            "ReportedAdjudication creation time not set for reported adjudication number ${reportedAdjudication.reportNumber}"
          ),
        agencyId = reportedAdjudication.agencyId,
        incidentTime = reportedAdjudication.dateTimeOfIncident,
        incidentLocationId = reportedAdjudication.locationId,
        statement = reportedAdjudication.statement,
        offenceCodes = getNomisCodes(reportedAdjudication.incidentRoleCode, reportedAdjudication.offenceDetails, reportedAdjudication.isYouthOffender),
        connectedOffenderIds = getAssociatedOffenders(reportedAdjudication.incidentRoleAssociatedPrisonersNumber),
        victimOffenderIds = getVictimOffenders(reportedAdjudication.offenceDetails),
        victimStaffUsernames = getVictimStaffUsernames(reportedAdjudication.offenceDetails),
      )
    )
  }

  private fun getNomisCodes(roleCode: String?, offenceDetails: MutableList<ReportedOffence>?, isYouthOffender: Boolean): List<String> {
    if (roleCode != null) { // Null means committed on own
      return offenceDetails?.map { offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(it.offenceCode, isYouthOffender) }
        ?: emptyList()
    }
    return offenceDetails?.flatMap { offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(it.offenceCode, isYouthOffender) }
      ?: emptyList()
  }

  private fun getAssociatedOffenders(associatedPrisonersNumber: String?): List<String> {
    if (associatedPrisonersNumber == null) {
      return emptyList()
    }
    return listOf(associatedPrisonersNumber)
  }

  private fun getVictimOffenders(offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimPrisonersNumber } ?: emptyList()
  }

  private fun getVictimStaffUsernames(offenceDetails: MutableList<ReportedOffence>?): List<String> {
    return offenceDetails?.mapNotNull { it.victimStaffUsername } ?: emptyList()
  }
}

fun ReportedAdjudication.toDto(offenceCodeLookupService: OffenceCodeLookupService): ReportedAdjudicationDto = ReportedAdjudicationDto(
  adjudicationNumber = reportNumber,
  prisonerNumber = prisonerNumber,
  bookingId = bookingId,
  incidentDetails = IncidentDetailsDto(
    locationId = locationId,
    dateTimeOfIncident = dateTimeOfIncident,
    handoverDeadline = handoverDeadline
  ),
  isYouthOffender = isYouthOffender,
  incidentRole = IncidentRoleDto(
    roleCode = incidentRoleCode,
    offenceRule = IncidentRoleRuleLookup.getOffenceRuleDetails(incidentRoleCode, isYouthOffender),
    associatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
    associatedPrisonersName = incidentRoleAssociatedPrisonersName,
  ),
  offenceDetails = toReportedOffence(offenceDetails, isYouthOffender, offenceCodeLookupService),
  incidentStatement = IncidentStatementDto(
    statement = statement,
    completed = true,
  ),
  createdByUserId = createdByUserId!!,
  createdDateTime = createDateTime!!,
  status = status,
  reviewedByUserId = reviewUserId,
  statusReason = statusReason,
  statusDetails = statusDetails,
  damages = toReportedDamages(damages)
)

private fun toReportedOffence(offences: MutableList<ReportedOffence>?, isYouthOffender: Boolean, offenceCodeLookupService: OffenceCodeLookupService): List<OffenceDto> {
  return (offences ?: mutableListOf()).map { offence ->
    OffenceDto(
      offenceCode = offence.offenceCode,
      offenceRule = OffenceRuleDto(
        paragraphNumber = offenceCodeLookupService.getParagraphNumber(offence.offenceCode, isYouthOffender),
        paragraphDescription = offenceCodeLookupService.getParagraphDescription(offence.offenceCode, isYouthOffender),
      ),
      victimPrisonersNumber = offence.victimPrisonersNumber,
      victimStaffUsername = offence.victimStaffUsername,
      victimOtherPersonsName = offence.victimOtherPersonsName,
    )
  }.toList()
}

private fun toReportedDamages(damages: MutableList<ReportedDamage>?): List<ReportedDamageDto> {
  return (damages ?: mutableListOf()).map {
    ReportedDamageDto(
      code = it.code,
      details = it.details,
      reporter = it.createdByUserId
    )
  }.toList()
}
