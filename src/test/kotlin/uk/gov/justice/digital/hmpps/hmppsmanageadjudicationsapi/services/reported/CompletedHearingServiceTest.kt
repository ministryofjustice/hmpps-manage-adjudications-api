package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode

class CompletedHearingServiceTest : ReportedAdjudicationTestBase() {

  private val outcomeService: OutcomeService = mock()
  private val hearingOutcomeService: HearingOutcomeService = mock()

  private val completedHearingService = CompletedHearingService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class CreateDismissed {

    @Test
    fun `creates a dismissed outcome and hearing outcome of completed `() {
      completedHearingService.createDismissed(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        details = "details",
      )

      verify(outcomeService, atLeastOnce()).createDismissed(
        chargeNumber = "1",
        details = "details",
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )
    }
  }

  @Nested
  inner class CreateNotProceed {

    @Test
    fun `creates a not proceed outcome and hearing outcome of completed `() {
      completedHearingService.createNotProceed(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        reason = NotProceedReason.NOT_FAIR,
        details = "details",
      )

      verify(outcomeService, atLeastOnce()).createNotProceed(
        chargeNumber = "1",
        reason = NotProceedReason.NOT_FAIR,
        details = "details",
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )
    }
  }

  @Nested
  inner class CreateChargeProved {
    @Test
    fun `creates a charge proved outcome and hearing outcome of completed `() {
      completedHearingService.createChargeProvedV2(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )

      verify(outcomeService, atLeastOnce()).createChargeProvedV2(
        chargeNumber = "1",
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )
    }
  }

  @Nested
  inner class RemoveCompletedHearingOutcome {

    @Test
    fun `remove a completed hearing outcome removes outcome and hearing outcome `() {
      whenever(outcomeService.getLatestOutcome("1")).thenReturn(Outcome(id = 1L, code = OutcomeCode.CHARGE_PROVED))
      completedHearingService.removeOutcome(chargeNumber = "1")

      verify(outcomeService, atLeastOnce()).deleteOutcome(chargeNumber = "1", id = 1L)
      verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(chargeNumber = "1")
    }

    @Test
    fun `remove a completed hearing outcome throws validation exception if not a completed outcome type `() {
      Assertions.assertThatThrownBy {
        completedHearingService.removeOutcome(chargeNumber = "1")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("No completed hearing outcome to remove")

      whenever(outcomeService.getLatestOutcome("1")).thenReturn(null)
      verify(outcomeService, never()).deleteOutcome(chargeNumber = "1", id = 1L)
      verify(hearingOutcomeService, never()).deleteHearingOutcome(chargeNumber = "1")
    }
  }
}
