package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudicationRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

@Service
class ReportedAdjudicationService(
  val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository,
  val authenticationFacade: AuthenticationFacade,
  val prisonApi: PrisonApiGateway,
  val dateCalculationService: DateCalculationService
) {
  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      prisonApi.getReportedAdjudication(adjudicationNumber)

    val expirationDateTime = dateCalculationService.calculate48WorkingHoursFrom(reportedAdjudication.incidentTime)

    return reportedAdjudication.toDto(expirationDateTime)
  }

  fun getMyReportedAdjudications(agencyId: String, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val adjudicationNumbers = myAdjudications() // listOf(1524279L, 1524280L).toSet()
    if (adjudicationNumbers.isEmpty()) return Page.empty(pageable)
    return prisonApi.search(
      ReportedAdjudicationRequest(agencyId, adjudicationNumbers),
      pageable
    ).map { it.toDto(dateCalculationService.calculate48WorkingHoursFrom(it.incidentTime)) }
  }

  private fun myAdjudications(): Set<Long> {
    val username = authenticationFacade.currentUsername
    val submittedAdjudicationHistory = submittedAdjudicationHistoryRepository.findByCreatedByUserId(username!!)
    return submittedAdjudicationHistory.map { it.adjudicationNumber }.toSet()
  }
}
