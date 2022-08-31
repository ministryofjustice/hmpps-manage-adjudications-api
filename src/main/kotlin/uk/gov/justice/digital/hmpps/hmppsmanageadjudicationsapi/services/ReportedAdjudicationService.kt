package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedDamageDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedEvidenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
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

@Transactional
@Service
class ReportedAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val prisonApiGateway: PrisonApiGateway,
  val offenceCodeLookupService: OffenceCodeLookupService,
  val authenticationFacade: AuthenticationFacade,
  val telemetryClient: TelemetryClient
) {
  companion object {
    const val TELEMETRY_EVENT = "ReportedAdjudicationStatusEvent"
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
    fun statuses(reportedAdjudicationStatus: Optional<ReportedAdjudicationStatus>): List<ReportedAdjudicationStatus> = reportedAdjudicationStatus.map { listOf(it) }.orElse(ReportedAdjudicationStatus.values().toList())
  }

  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)

    return reportedAdjudication?.toDto(offenceCodeLookupService) ?: throwEntityNotFoundException(adjudicationNumber)
  }

  fun getAllReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, status: Optional<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
        agencyId, reportsFrom(startDate), reportsTo(endDate), statuses(status), pageable
      )
    return reportedAdjudicationsPage.map { it.toDto(offenceCodeLookupService) }
  }

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
      isYouthOffender = reportedAdjudication.isYouthOffender,
      damages = toDraftDamages(reportedAdjudication.damages),
      evidence = toDraftEvidence(reportedAdjudication.evidence),
      witnesses = toDraftWitnesses(reportedAdjudication.witnesses)
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

  private fun toDraftDamages(damages: MutableList<ReportedDamage>?): MutableList<Damage> {
    return (damages ?: mutableListOf()).map {
      Damage(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  private fun toDraftEvidence(evidence: MutableList<ReportedEvidence>?): MutableList<Evidence> {
    return (evidence ?: mutableListOf()).map {
      Evidence(
        code = it.code,
        details = it.details,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  private fun toDraftWitnesses(witnesses: MutableList<ReportedWitness>?): MutableList<Witness> {
    return (witnesses ?: mutableListOf()).map {
      Witness(
        code = it.code,
        firstName = it.firstName,
        lastName = it.lastName,
        reporter = it.reporter
      )
    }.toMutableList()
  }

  fun setStatus(adjudicationNumber: Long, reportedAdjudicationStatus: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null): ReportedAdjudicationDto {
    val username = if (reportedAdjudicationStatus == ReportedAdjudicationStatus.AWAITING_REVIEW) null else authenticationFacade.currentUsername
    val reportedAdjudication = reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)
      ?: throw EntityNotFoundException("ReportedAdjudication not found for reported adjudication number $adjudicationNumber")
    val reportedAdjudicationToReturn = reportedAdjudication.let {
      it.transition(to = reportedAdjudicationStatus, reason = statusReason, details = statusDetails, reviewUserId = username)
      reportedAdjudicationRepository.save(it).toDto(this.offenceCodeLookupService)
    }
    if (reportedAdjudicationStatus.isAccepted()) {
      saveToPrisonApi(reportedAdjudication)
    }

    telemetryClient.trackEvent(
      TELEMETRY_EVENT,
      mapOf(
        "reportNumber" to reportedAdjudication.reportNumber.toString(),
        "agencyId" to reportedAdjudication.agencyId,
        "status" to reportedAdjudicationStatus.name,
        "reason" to statusReason
      ),
      null
    )

    return reportedAdjudicationToReturn
  }

  fun updateDamages(adjudicationNumber: Long, damages: List<DamageRequestItem>): ReportedAdjudicationDto {
    val reportedAdjudication = reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)
      ?: throwEntityNotFoundException(adjudicationNumber)
    val reporter = authenticationFacade.currentUsername!!
    val toPreserve = reportedAdjudication.damages.filter { it.reporter != reporter }

    reportedAdjudication.damages.clear()
    reportedAdjudication.damages.addAll(toPreserve)
    reportedAdjudication.damages.addAll(
      damages.filter { it.reporter == reporter }.map {
        ReportedDamage(
          code = it.code,
          details = it.details,
          reporter = reporter
        )
      }
    )

    return reportedAdjudicationRepository.save(reportedAdjudication).toDto(offenceCodeLookupService)
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
  reviewedByUserId = reviewUserId,
  damages = toReportedDamages(damages),
  evidence = toReportedEvidence(evidence),
  witnesses = toReportedWitnesses(witnesses),
  status = status,
  statusReason = statusReason,
  statusDetails = statusDetails
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
      reporter = it.reporter
    )
  }.toList()
}

private fun toReportedEvidence(evidence: MutableList<ReportedEvidence>?): List<ReportedEvidenceDto> {
  return (evidence ?: mutableListOf()).map {
    ReportedEvidenceDto(
      code = it.code,
      identifier = it.identifier,
      details = it.details,
      reporter = it.reporter
    )
  }.toList()
}

private fun toReportedWitnesses(witnesses: MutableList<ReportedWitness>?): List<ReportedWitnessDto> {
  return (witnesses ?: mutableListOf()).map {
    ReportedWitnessDto(
      code = it.code,
      firstName = it.firstName,
      lastName = it.lastName,
      reporter = it.reporter
    )
  }.toList()
}
