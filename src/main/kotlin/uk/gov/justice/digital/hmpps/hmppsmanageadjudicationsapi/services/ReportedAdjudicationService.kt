package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.ReportedAdjudicationRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageResponse
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
    val adjudicationNumbers = myAdjudications()
    if (adjudicationNumbers.isEmpty()) return emptyList()
    return prisonApi.getReportedAdjudications(adjudicationNumbers)
      .map { it.toDto() }
  }

  fun getMyReportedAdjudications(locationId: Long, pageRequest: PageRequest): PageResponse<ReportedAdjudicationDto> {
    val adjudicationNumbers = myAdjudications()
    if (adjudicationNumbers.isEmpty()) return PageResponse.emptyPageRequest(pageRequest)
    return prisonApi.getReportedAdjudications(ReportedAdjudicationRequest(locationId, pageRequest, adjudicationNumbers)).map {it.toDto()}
  }

  private fun myAdjudications(): Set<Long> {
    val username = authenticationFacade.currentUsername
    val submittedAdjudicationHistory = submittedAdjudicationHistoryRepository.findByCreatedByUserId(username!!)
    return submittedAdjudicationHistory.map { it.adjudicationNumber }.toSet()
  }
}
