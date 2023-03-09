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
    TODO("implement me")
  }
}
