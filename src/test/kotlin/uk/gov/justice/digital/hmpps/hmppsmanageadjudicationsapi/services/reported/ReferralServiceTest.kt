package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import javax.validation.ValidationException

class ReferralServiceTest : ReportedAdjudicationTestBase() {

  private val outcomeService: OutcomeService = mock()
  private val hearingOutcomeService: HearingOutcomeService = mock()

  private var referralService = ReferralService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Test
  fun `create outcome and hearing outcome for referral`() {

    referralService.createReferral(
      1, 1, HearingOutcomeCode.REFER_POLICE, "test", "details",
    )

    verify(hearingOutcomeService, atLeastOnce()).createHearingOutcome(
      adjudicationNumber = 1, hearingId = 1, code = HearingOutcomeCode.REFER_POLICE, adjudicator = "test",
    )

    verify(outcomeService, atLeastOnce()).createOutcome(adjudicationNumber = 1, code = OutcomeCode.REFER_POLICE, details = "details")
  }

  @Test
  fun `updates outcome and hearing outcome for referral`() {

    referralService.updateReferral(
      1, 1, HearingOutcomeCode.REFER_INAD, "test 2", "details 2",
    )

    verify(hearingOutcomeService, atLeastOnce()).updateHearingOutcome(
      adjudicationNumber = 1, hearingId = 1, code = HearingOutcomeCode.REFER_INAD, adjudicator = "test 2",
    )

    // TODO not implemented yet verify(outcomeService, atLeastOnce()).createOutcome(adjudicationNumber = 1, code = OutcomeCode.REFER_POLICE, details = "details 2")
  }

  @Test
  fun `remove referral should only remove outcome`() {
    referralService.removeReferral(1,)

    verify(outcomeService, atLeastOnce()).deleteOutcome(1, 1)
    verify(hearingOutcomeService, never()).deleteHearingOutcome(1, 1)
  }

  @Test
  fun `remove referral should remove outcome and hearing outcome`() {
    referralService.removeReferral(1,)

    verify(outcomeService, atLeastOnce()).deleteOutcome(1, 1)
    verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(1, 1)
  }

  @Test
  fun `responds with bad request if no referral on adjudication`() {

    Assertions.assertThatThrownBy {
      referralService.removeReferral(1,)
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("No referral for adjudication")
  }

  @Test
  fun `remove referral should remove outcome and hearing outcome and referral outcome`() {
    TODO("implement me")
  }

  @Test
  fun `remove referral should remove outcome and referral outcome`() {
    TODO("implement me")
  }
}
