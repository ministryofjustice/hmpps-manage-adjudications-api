package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import javax.persistence.EntityNotFoundException

@Service
class ReportedAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val authenticationFacade: AuthenticationFacade
) {
  companion object {
    fun throwEntityNotFoundException(id: Long): Nothing =
      throw EntityNotFoundException("ReportedAdjudication not found for $id")
  }

  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(adjudicationNumber)

    return reportedAdjudication?.toDto() ?: throwEntityNotFoundException(adjudicationNumber)
  }

  fun getAllReportedAdjudications(agencyId: String, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage = reportedAdjudicationRepository.findByAgencyId(agencyId, pageable)
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getMyReportedAdjudications(agencyId: String, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername
    val reportedAdjudicationsPage = reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyId(username!!, agencyId, pageable)
    return reportedAdjudicationsPage.map { it.toDto() }
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
      ),
      offenceDetails = toDraftOffence(reportedAdjudication.offences),
      incidentStatement = IncidentStatement(
        statement = reportedAdjudication.statement,
        completed = true
      )
    )

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  private fun toDraftOffence(offences: MutableList<ReportedOffence>?): MutableList<Offence> {
    return (offences ?: mutableListOf()).map { offence ->
      Offence(
        offenceCode = offence.offenceCode,
        victimPrisonersNumber = offence.victimPrisonersNumber
      )
    }.toMutableList()
  }
}

fun ReportedAdjudication.toDto(): ReportedAdjudicationDto = ReportedAdjudicationDto(
  adjudicationNumber = reportNumber,
  prisonerNumber = prisonerNumber,
  bookingId = bookingId,
  incidentDetails = IncidentDetailsDto(
    locationId = locationId,
    dateTimeOfIncident = dateTimeOfIncident,
    handoverDeadline = handoverDeadline
  ),
  incidentRole = IncidentRoleDto(
    roleCode = incidentRoleCode,
    associatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
  ),
  offences = toReportedOffence(offences),
  incidentStatement = IncidentStatementDto(
    statement = statement,
    completed = true,
  ),
  createdByUserId = createdByUserId!!,
  createdDateTime = createDateTime!!
)

private fun toReportedOffence(offences: MutableList<ReportedOffence>?): List<OffenceDto> {
  return (offences ?: mutableListOf()).map { offence ->
    OffenceDto(
      offenceCode = offence.offenceCode,
      victimPrisonersNumber = offence.victimPrisonersNumber
    )
  }.toList()
}
