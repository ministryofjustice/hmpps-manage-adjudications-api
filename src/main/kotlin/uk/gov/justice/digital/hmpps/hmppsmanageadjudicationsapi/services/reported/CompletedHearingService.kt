package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import javax.transaction.Transactional

@Transactional
@Service
class CompletedHearingService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createDismissed(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
    details: String,
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }

  fun createNotProceed(
    adjudicationNumber: Long,
    adjudicator: String,
    plea: HearingOutcomePlea,
    reason: NotProceedReason,
    details: String,
  ): ReportedAdjudicationDto {
    TODO("implement me")
  }
}
