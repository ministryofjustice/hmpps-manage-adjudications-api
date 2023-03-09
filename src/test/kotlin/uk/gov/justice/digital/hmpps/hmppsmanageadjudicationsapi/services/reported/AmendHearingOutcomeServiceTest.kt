package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import javax.validation.ValidationException

class AmendHearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val hearingOutcomeService: HearingOutcomeService = mock()
  private val outcomeService: OutcomeService = mock()
  private val amendHearingOutcomeService = AmendHearingOutcomeService(hearingOutcomeService, outcomeService)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeSame {

    @CsvSource("REFER_POLICE, REFER_POLICE", "REFER_INAD, REFER_INAD", "ADJOURNED, ADJOURN", "CHARGE_PROVED, COMPLETE", "DISMISSED, COMPLETE", "NOT_PROCEED, COMPLETE")
    @ParameterizedTest
    fun `updating the same type calls correct services for simple updates `(status: ReportedAdjudicationStatus, code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
        }
      )

      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome(1L)).thenReturn(
        Pair(status, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""))
      )

      amendHearingOutcomeService.amendHearingOutcome(
        adjudicationNumber = 1L, status = status, amendHearingOutcomeRequest = AmendHearingOutcomeRequest()
      )

      verify(hearingOutcomeService, atLeastOnce()).getCurrentStatusAndLatestOutcome(adjudicationNumber = 1L)
      verify(hearingOutcomeService, atLeastOnce()).amendHearingOutcome(adjudicationNumber = 1L, outcomeCodeToAmend = code)
      verify(outcomeService, atLeastOnce()).amendOutcomeViaService(adjudicationNumber = 1L)
    }

    @CsvSource("ACCEPTED", "SCHEDULED", "UNSCHEDULED", "REJECTED", "RETURNED", "PROSECUTION", "QUASHED")
    @ParameterizedTest
    fun `throws validation exception if status is not editable `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
        }
      )

      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome(1L)).thenReturn(
        Pair(status, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""))
      )

      Assertions.assertThatThrownBy {
        amendHearingOutcomeService.amendHearingOutcome(
          adjudicationNumber = 1, status = status, amendHearingOutcomeRequest = AmendHearingOutcomeRequest()
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend from this status")
    }
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeHasChanged {
    // focus on changing the outcome type - TODO
  }
}
