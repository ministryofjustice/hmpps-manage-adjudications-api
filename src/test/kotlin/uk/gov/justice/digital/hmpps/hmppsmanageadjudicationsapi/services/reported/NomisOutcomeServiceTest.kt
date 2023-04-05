package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisHearingResultRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome

class NomisOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val prisonApiGateway: PrisonApiGateway = mock()
  private val nomisOutcomeService = NomisOutcomeService(prisonApiGateway)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class CreateHearingResult {

    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.createHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)
      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, never()).createHearingResult(anyOrNull(), any(), any())
    }

    @Test
    fun `prosecution from hearing creates hearing result and hearing `() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "", plea = HearingOutcomePlea.GUILTY)
        it.outcomes.add(Outcome(code = OutcomeCode.PROSECUTION))
      }

      whenever(prisonApiGateway.createHearing(any(), any())).thenReturn(123)

      val hearingId = nomisOutcomeService.createHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      assertThat(hearingId).isNotNull
      verify(prisonApiGateway, atLeastOnce()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(
        reportedAdjudication.reportNumber,
        123,
        NomisHearingResultRequest(
          plea = Plea.GUILTY,
          adjudicator = "",
          finding = Finding.PROSECUTED,
        ),
      )
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} creates hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.valueOf(code.toString()), adjudicator = "")
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.createHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(any(), any(), any())
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
        it.outcomes.add(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.createHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)
      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, never()).createHearingResult(anyOrNull(), any(), any())
    }
  }

  @Nested
  inner class AmendHearingResult {
    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)
      verify(prisonApiGateway, never()).amendHearingResult(any(), any(), any())
    }

    @Test
    fun `prosecution from hearing amends hearing result `() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
        it.outcomes.add(Outcome(code = OutcomeCode.PROSECUTION))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).createHearing(anyOrNull(), any())
      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(any(), any(), any())
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} amends hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.valueOf(code.toString()), adjudicator = "")
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(anyOrNull(), any(), any())
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
        it.outcomes.add(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).amendHearingResult(anyOrNull(), any(), any())
    }
  }

  @Nested
  inner class DeleteHearingResult {
    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).deleteHearing(any(), any())
      verify(prisonApiGateway, never()).deleteHearingResult(any(), any())
    }

    @Test
    fun `prosecution from hearing deletes hearing result and hearing `() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
        it.hearings.first().oicHearingId = 100L
        it.outcomes.add(Outcome(code = OutcomeCode.PROSECUTION))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, atLeastOnce()).deleteHearing(reportedAdjudication.reportNumber, 100L)
      verify(prisonApiGateway, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, 100L)
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} deletes hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.valueOf(code.toString()), adjudicator = "")
        it.hearings.first().oicHearingId = 100L
        it.outcomes.add(Outcome(code = code))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).deleteHearing(reportedAdjudication.reportNumber, 100L)
      verify(prisonApiGateway, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, 100L)
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
        it.outcomes.add(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(reportedAdjudication.getLatestHearing(), reportedAdjudication.latestOutcome()!!)

      verify(prisonApiGateway, never()).deleteHearing(any(), any())
      verify(prisonApiGateway, never()).deleteHearingResult(any(), any())
    }
  }
}
