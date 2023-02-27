package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason

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
        adjudicationNumber = 1L, adjudicator = "test", plea = HearingOutcomePlea.UNFIT, details = "details"
      )

      verify(outcomeService).createDismissed(
        adjudicationNumber = 1L, details = "details"
      )

      verify(hearingOutcomeService).createCompletedHearing(
        adjudicationNumber = 1L, adjudicator = "test", plea = HearingOutcomePlea.UNFIT
      )
    }
  }

  @Nested
  inner class CreateNotProceed {

    @Test
    fun `creates a not proceed outcome and hearing outcome of completed `() {
      completedHearingService.createNotProceed(
        adjudicationNumber = 1L, adjudicator = "test", plea = HearingOutcomePlea.UNFIT, reason = NotProceedReason.NOT_FAIR, details = "details"
      )

      verify(outcomeService).createNotProceed(
        adjudicationNumber = 1L, reason = NotProceedReason.NOT_FAIR, details = "details"
      )

      verify(hearingOutcomeService).createCompletedHearing(
        adjudicationNumber = 1L, adjudicator = "test", plea = HearingOutcomePlea.UNFIT
      )
    }
  }
}
