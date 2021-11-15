package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import javax.persistence.EntityNotFoundException

@Service
class ReportedAdjudicationService(
  val submittedAdjudicationHistoryRepository: SubmittedAdjudicationHistoryRepository,
  val authenticationFacade: AuthenticationFacade,
  val prisonApi: PrisonApiGateway
) {
  fun getReportedAdjudicationDetails(adjudicationNumber: Long): ReportedAdjudicationDto {
    val reportedAdjudication =
      prisonApi.getReportedAdjudication(adjudicationNumber)
    val submittedAdjudicationHistory = submittedAdjudicationHistoryRepository.findByAdjudicationNumber(adjudicationNumber)

    return reportedAdjudication.toDto(submittedAdjudicationHistory ?:
      throw EntityNotFoundException(String.format("Adjudication not found: Number: %d", adjudicationNumber)))
  }

  fun getMyReportedAdjudications(): List<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername
    val submittedAdjudicationHistories = submittedAdjudicationHistoryRepository.findByCreatedByUserId(username!!)
    val submittedAdjudicationHistoriesByAdjudicationNumber = submittedAdjudicationHistories.associateBy {it.adjudicationNumber}

    if (submittedAdjudicationHistoriesByAdjudicationNumber.isEmpty()) return emptyList()

    return prisonApi.getReportedAdjudications(submittedAdjudicationHistoriesByAdjudicationNumber.keys)
      .map { it.toDto(submittedAdjudicationHistoriesByAdjudicationNumber[it.adjudicationNumber]!!) }
  }
}
