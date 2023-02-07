package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode

class ReferralServiceTest : ReportedAdjudicationTestBase() {

  private val outcomeService: OutcomeService = mock()
  private val hearingOutcomeService: HearingOutcomeService = mock()

  private var referralService = ReferralService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class CreateReferral {

    @Test
    fun `create outcome and hearing outcome for referral`() {

      referralService.createReferral(
        1, 1, HearingOutcomeCode.REFER_POLICE, "test", "details",
      )

      verify(hearingOutcomeService, atLeastOnce()).createHearingOutcome(
        adjudicationNumber = 1, hearingId = 1, code = HearingOutcomeCode.REFER_POLICE, adjudicator = "test", details = "details"
      )

      verify(outcomeService, atLeastOnce()).createOutcome(adjudicationNumber = 1, code = OutcomeCode.REFER_POLICE, details = "details")
    }
  }

  @Nested
  inner class UpdateReferral {

    @Test
    fun `updates outcome and hearing outcome for referral`() {

      referralService.updateReferral(
        1, 1, HearingOutcomeCode.REFER_INAD, "test 2", "details 2",
      )

      verify(hearingOutcomeService, atLeastOnce()).updateHearingOutcome(
        adjudicationNumber = 1, hearingId = 1, code = HearingOutcomeCode.REFER_INAD, adjudicator = "test 2", details = "details 2"
      )

      // TODO not implemented yet verify(outcomeService, atLeastOnce()).createOutcome(adjudicationNumber = 1, code = OutcomeCode.REFER_POLICE, details = "details 2")
    }
  }
}
