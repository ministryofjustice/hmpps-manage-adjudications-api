package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase.Companion.REPORTED_ADJUDICATION_DTO
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
      whenever(outcomeService.createDismissed(any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)

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
      whenever(outcomeService.createNotProceed(any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)

      completedHearingService.createNotProceed(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
        notProceedReason = NotProceedReason.NOT_FAIR,
        details = "details",
      )

      verify(outcomeService, atLeastOnce()).createNotProceed(
        chargeNumber = "1",
        notProceedReason = NotProceedReason.NOT_FAIR,
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
      whenever(outcomeService.createChargeProved(any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      completedHearingService.createChargeProved(
        chargeNumber = "1",
        adjudicator = "test",
        plea = HearingOutcomePlea.UNFIT,
      )

      verify(outcomeService, atLeastOnce()).createChargeProved(
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

    @CsvSource("CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    fun `remove a completed hearing outcome removes outcome and hearing outcome `(outcomeCode: OutcomeCode) {
      whenever(hearingOutcomeService.deleteHearingOutcome(any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      whenever(outcomeService.getLatestOutcome("1")).thenReturn(Outcome(id = 1L, code = outcomeCode))
      val response = completedHearingService.removeOutcome(chargeNumber = "1")

      verify(outcomeService, atLeastOnce()).deleteOutcome(chargeNumber = "1", id = 1L)
      verify(hearingOutcomeService, atLeastOnce()).deleteHearingOutcome(chargeNumber = "1")

      if (outcomeCode == OutcomeCode.CHARGE_PROVED) {
        Assertions.assertThat(response.punishmentsRemoved).isTrue
      } else {
        Assertions.assertThat(response.punishmentsRemoved).isFalse
      }
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
