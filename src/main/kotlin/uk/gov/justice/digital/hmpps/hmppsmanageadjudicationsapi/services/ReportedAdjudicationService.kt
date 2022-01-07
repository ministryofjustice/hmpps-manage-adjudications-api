package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

@Service
class ReportedAdjudicationService(
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository,
  val authenticationFacade: AuthenticationFacade,
  val prisonApi: PrisonApiGateway,
  val dateCalculationService: DateCalculationService
) {
  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val nomisAdjudication =
      prisonApi.getReportedAdjudication(adjudicationNumber)

    val expirationDateTime = dateCalculationService.calculate48WorkingHoursFrom(nomisAdjudication.incidentTime)

    return nomisAdjudication.toDto(expirationDateTime)
  }

  fun getAllReportedAdjudications(agencyId: String, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val submittedAdjudicationsPage = submittedAdjudicationHistoryRepository.findByAgencyId(agencyId, pageable)
    return getAdjudicationDetailsPage(submittedAdjudicationsPage, pageable)
  }

  fun getMyReportedAdjudications(agencyId: String, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername
    val submittedAdjudicationsPage = submittedAdjudicationHistoryRepository.findByCreatedByUserIdAndAgencyId(username!!, agencyId, pageable)
    return getAdjudicationDetailsPage(submittedAdjudicationsPage, pageable)
  }

  fun createDraftFromReportedAdjudication(adjudicationNumber: Long): DraftAdjudicationDto {
    val nomisAdjudication =
      prisonApi.getReportedAdjudication(adjudicationNumber)

    val draftAdjudication = DraftAdjudication(
      reportNumber = nomisAdjudication.adjudicationNumber,
      reportByUserId = nomisAdjudication.createdByUserId,
      prisonerNumber = nomisAdjudication.offenderNo,
      agencyId = nomisAdjudication.agencyId,
      incidentDetails = IncidentDetails(
        locationId = nomisAdjudication.incidentLocationId,
        dateTimeOfIncident = nomisAdjudication.incidentTime,
        handoverDeadline = dateCalculationService.calculate48WorkingHoursFrom(nomisAdjudication.incidentTime)
      ),
      incidentStatement = IncidentStatement(
        statement = nomisAdjudication.statement,
        completed = true
      )
    )

    return draftAdjudicationRepository
      .save(draftAdjudication)
      .toDto()
  }

  private fun getAdjudicationDetailsPage(submittedAdjudicationsPage: Page<SubmittedAdjudicationHistory>, defaultPageable: Pageable): Page<ReportedAdjudicationDto> {
    val adjudicationNumbers = submittedAdjudicationsPage.map { it.adjudicationNumber }.toList()
    if (adjudicationNumbers.isEmpty()) return Page.empty(defaultPageable)

    val nomisAdjudicationDetailsByNumber = prisonApi.getReportedAdjudications(adjudicationNumbers).groupBy { it.adjudicationNumber }
    return submittedAdjudicationsPage.map { toDto(nomisAdjudicationDetailsByNumber[it.adjudicationNumber]!![0]) }
  }

  private fun toDto(adjudication: NomisAdjudication): ReportedAdjudicationDto =
    adjudication.toDto(dateCalculationService.calculate48WorkingHoursFrom(adjudication.incidentTime))
}
