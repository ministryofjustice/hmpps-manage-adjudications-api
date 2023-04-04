package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

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
import javax.validation.ValidationException

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
        adjudicationNumber = 1L,
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        details = "details",
      )

      verify(outcomeService, atLeastOnce()).createDismissed(
        adjudicationNumber = 1L,
        details = "details",
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        adjudicationNumber = 1L,
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
        adjudicationNumber = 1L,
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        reason = NotProceedReason.NOT_FAIR,
        details = "details",
      )

      verify(outcomeService, atLeastOnce()).createNotProceed(
        adjudicationNumber = 1L,
        reason = NotProceedReason.NOT_FAIR,
        details = "details",
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        adjudicationNumber = 1L,
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )
    }
  }

  @Nested
  inner class CreateChargeProved {
    @Test
    fun `creates a charge proved outcome and hearing outcome of completed `() {
      completedHearingService.createChargeProved(
        adjudicationNumber = 1L,
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        amount = 0.0,
        caution = false,
      )

      verify(outcomeService, atLeastOnce()).createChargeProved(
        adjudicationNumber = 1L,
        amount = 0.0,
        caution = false,
      )

      verify(hearingOutcomeService, atLeastOnce()).createCompletedHearing(
        adjudicationNumber = 1L,
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )
    }
  }

  @Nested
  inner class RemoveCompletedHearingOutcome {

    @Test
    fun `remove a completed hearing outcome removes outcome and hearing outcome `() {
      whenever(outcomeService.getLatestOutcome(1L)).thenReturn(Outcome(id = 1L, code = OutcomeCode.CHARGE_PROVED))
      completedHearingService.removeOutcome(adjudicationNumber = 1L)

      verify(outcomeService, atLeastOnce()).deleteOutcome(adjudicationNumber = 1L, id = 1L)
      verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(adjudicationNumber = 1L)
    }

    @Test
    fun `remove a completed hearing outcome throws validation exception if not a completed outcome type `() {
      Assertions.assertThatThrownBy {
        completedHearingService.removeOutcome(adjudicationNumber = 1L)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("No completed hearing outcome to remove")

      whenever(outcomeService.getLatestOutcome(1L)).thenReturn(null)
      verify(outcomeService, never()).deleteOutcome(adjudicationNumber = 1L, id = 1L)
      verify(hearingOutcomeService, never()).deleteHearingOutcome(adjudicationNumber = 1L)
    }
  }
}
