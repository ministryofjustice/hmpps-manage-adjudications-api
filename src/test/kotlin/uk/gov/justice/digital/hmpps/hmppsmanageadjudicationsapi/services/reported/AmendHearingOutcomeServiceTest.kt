package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class AmendHearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val hearingOutcomeService: HearingOutcomeService = mock()
  private val outcomeService: OutcomeService = mock()
  private val amendHearingOutcomeService = AmendHearingOutcomeService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeSame {

    @CsvSource("REFER_POLICE", "REFER_INAD", "ADJOURNED", "CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    fun `updating the same type calls correct services for simple updates `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
        }
      )

      amendHearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1L, status = status, amendHearingOutcomeRequest = AmendHearingOutcomeRequest()
      )

      verify(hearingOutcomeService, atLeastOnce()).amendHearingOutcome(adjudicationNumber = 1L)
      verify(outcomeService, atLeastOnce()).amendOutcomeViaService(adjudicationNumber = 1L)
    }
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeHasChanged {
    // focus on changing the outcome type - TODO
  }
}
