package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import javax.transaction.Transactional

@Transactional
@Service
class AmendHearingOutcomeService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun amendHearingOutcome(
    adjudicationNumber: Long,
    status: ReportedAdjudicationStatus,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    val currentInfo = hearingOutcomeService.getCurrentStatusAndLatestOutcome(
      adjudicationNumber = adjudicationNumber
    )
    return if (currentInfo.first == status) amend(
      adjudicationNumber = adjudicationNumber,
      amendHearingOutcomeRequest = amendHearingOutcomeRequest,
    ) else removeAndCreate(
      adjudicationNumber = adjudicationNumber
    )
  }

  private fun amend(
    adjudicationNumber: Long,
    amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.amendHearingOutcome(
      adjudicationNumber = adjudicationNumber,
    )

    return outcomeService.amendOutcomeViaService(
      adjudicationNumber = adjudicationNumber
    )
  }

  private fun removeAndCreate(
    adjudicationNumber: Long,
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }
}
