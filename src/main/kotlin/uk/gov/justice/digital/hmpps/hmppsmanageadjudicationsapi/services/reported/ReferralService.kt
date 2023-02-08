package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import javax.transaction.Transactional

@Transactional
@Service
class ReferralService(
  private val hearingOutcomeService: HearingOutcomeService,
  private val outcomeService: OutcomeService,
) {

  fun createReferral(
    adjudicationNumber: Long,
    hearingId: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto {
    hearingOutcomeService.createHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      hearingId = hearingId,
      code = code,
      adjudicator = adjudicator,
    )
    return outcomeService.createOutcome(
      adjudicationNumber = adjudicationNumber,
      code = code.outcomeCode!!,
      details = details
    )
  }

  fun updateReferral(
    adjudicationNumber: Long,
    hearingId: Long,
    code: HearingOutcomeCode,
    adjudicator: String,
    details: String,
  ): ReportedAdjudicationDto {
    return hearingOutcomeService.updateHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      hearingId = hearingId,
      code = code,
      adjudicator = adjudicator,
    )
    // TODO not implemented update outcome yet.  later tickets, plus can remove a referral too.
  }

  fun removeReferral(adjudicationNumber: Long): ReportedAdjudicationDto {
    TODO("implement me")
  }
}
