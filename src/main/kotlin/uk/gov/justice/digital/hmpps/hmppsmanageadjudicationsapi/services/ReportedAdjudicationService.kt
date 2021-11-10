package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade

@Service
class ReportedAdjudicationService(
  val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository,
  val authenticationFacade: AuthenticationFacade,
  val prisonApi: PrisonApiGateway
) {
  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      prisonApi.getReportedAdjudication(adjudicationNumber)

    return reportedAdjudication.toDto()
  }

  fun getMyReportedAdjudications(): List<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername
    val submittedAdjudicationHistory = submittedAdjudicationHistoryRepository.findByCreatedByUserId(username!!)
    val adjudicationNumbers = submittedAdjudicationHistory.map { it.adjudicationNumber }.toSet()

    if (adjudicationNumbers.isEmpty()) return emptyList()

    return prisonApi.getReportedAdjudications(adjudicationNumbers)
      .map { it.toDto() }
  }
}
