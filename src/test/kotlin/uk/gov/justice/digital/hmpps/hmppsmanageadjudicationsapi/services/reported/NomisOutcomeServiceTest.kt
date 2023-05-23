package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingResultRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome
import java.time.LocalDateTime

class NomisOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val prisonApiGateway: PrisonApiGateway = mock()
  private val nomisOutcomeService = NomisOutcomeService(prisonApiGateway)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class CreateHearingResult {

    @Test
    fun `hearing without outcome throws exception `() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.addOutcome(Outcome(code = OutcomeCode.QUASHED))
      }

      Assertions.assertThatThrownBy {
        nomisOutcomeService.createHearingResultIfApplicable(
          adjudicationNumber = reportedAdjudication.reportNumber,
          hearing = reportedAdjudication.getLatestHearing(),
          outcome = reportedAdjudication.latestOutcome()!!,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing hearing outcome")
    }

    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )
      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, never()).createHearingResult(anyOrNull(), any(), any())
    }

    @Test
    fun `quashed creates hearing and result`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = 122
        it.addOutcome(
          Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              o ->
            o.createDateTime = LocalDateTime.now()
          },
        )
        it.addOutcome(
          Outcome(code = OutcomeCode.QUASHED).also {
              o ->
            o.createDateTime = LocalDateTime.now().plusDays(1)
          },
        )
      }

      whenever(prisonApiGateway.createHearing(any(), any())).thenReturn(123)

      val hearingId = nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      assertThat(hearingId).isNotNull
      verify(prisonApiGateway, atLeastOnce()).quashSanctions(any())
      verify(prisonApiGateway, atLeastOnce()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(
        reportedAdjudication.reportNumber,
        123,
        OicHearingResultRequest(
          pleaFindingCode = Plea.GUILTY,
          findingCode = Finding.QUASHED,
          adjudicator = "testing",
        ),
      )
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun ` {0} from hearing - POLICE REFER creates hearing and result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.hearings.first().oicHearingId = 122
        it.addOutcome(Outcome(code = code))
      }

      whenever(prisonApiGateway.createHearing(any(), any())).thenReturn(123)

      val hearingId = nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      assertThat(hearingId).isNotNull
      verify(prisonApiGateway, atLeastOnce()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(
        reportedAdjudication.reportNumber,
        123,
        OicHearingResultRequest(
          pleaFindingCode = Plea.NOT_ASKED,
          findingCode = code.finding!!,
          adjudicator = "testing",
        ),
      )
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} creates hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = if (code == OutcomeCode.REFER_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE, adjudicator = "testing")
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(any(), any(), any())
    }

    @EnumSource(OicHearingType::class)
    @ParameterizedTest
    fun `adjudicator should only be sent if GOV_UK, GOV_ADULT, GOV_YOI`(oicHearingType: OicHearingType) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().oicHearingType = oicHearingType
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED))
      }

      nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      val hearing = reportedAdjudication.hearings.first()
      val request = createHearingResultRequestForVerify(reportedAdjudication, hearing)

      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, atLeastOnce()).createHearingResult(
        adjudicationNumber = reportedAdjudication.reportNumber,
        oicHearingId = hearing.oicHearingId,
        oicHearingResultRequest = request,
      )
    }

    @Test
    fun `REFER_INAD calls prison api to update hearing`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = 1
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )
      verify(prisonApiGateway, never()).createHearing(any(), any())
      verify(prisonApiGateway, never()).createHearingResult(anyOrNull(), any(), any())
      verify(prisonApiGateway, atLeastOnce()).amendHearing(
        reportedAdjudication.reportNumber,
        1,
        OicHearingRequest(
          reportedAdjudication.hearings.first().dateTimeOfHearing,
          reportedAdjudication.hearings.first().oicHearingType,
          reportedAdjudication.hearings.first().locationId,
          "testing",
          "REFER_INAD",
        ),
      )
    }
  }

  @Nested
  inner class AmendHearingResult {

    @CsvSource("QUASHED", "NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun `oic hearing id not present on outcome exception `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.addOutcome(Outcome(code = code))
      }

      Assertions.assertThatThrownBy {
        nomisOutcomeService.amendHearingResultIfApplicable(
          adjudicationNumber = reportedAdjudication.reportNumber,
          hearing = reportedAdjudication.getLatestHearing(),
          outcome = reportedAdjudication.latestOutcome()!!,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("oic hearing id not linked to outcome")
    }

    @Test
    fun `hearing without outcome throws exception `() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.addOutcome(Outcome(code = OutcomeCode.QUASHED, oicHearingId = 1))
      }

      Assertions.assertThatThrownBy {
        nomisOutcomeService.amendHearingResultIfApplicable(
          adjudicationNumber = reportedAdjudication.reportNumber,
          hearing = reportedAdjudication.getLatestHearing(),
          outcome = reportedAdjudication.latestOutcome()!!,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing hearing outcome")
    }

    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )
      verify(prisonApiGateway, never()).amendHearingResult(any(), any(), any())
    }

    @Test
    fun `quashed amend`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = 122
        it.addOutcome(
          Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              o ->
            o.createDateTime = LocalDateTime.now()
          },
        )
        it.addOutcome(
          Outcome(code = OutcomeCode.QUASHED, oicHearingId = 123).also {
              o ->
            o.createDateTime = LocalDateTime.now().plusDays(1)
          },
        )
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).createHearing(anyOrNull(), any())
      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(
        reportedAdjudication.reportNumber,
        123L,
        OicHearingResultRequest(
          findingCode = Finding.QUASHED,
          pleaFindingCode = Plea.GUILTY,
          adjudicator = "testing",
        ),
      )
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun `{0} from hearing - police refer amends hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.hearings.first().oicHearingId = 122L
        it.addOutcome(Outcome(code = code, oicHearingId = 123L))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).createHearing(anyOrNull(), any())
      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(
        reportedAdjudication.reportNumber,
        123L,
        OicHearingResultRequest(
          pleaFindingCode = Plea.NOT_ASKED,
          findingCode = code.finding!!,
          adjudicator = "testing",
        ),
      )
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} amends hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = if (code == OutcomeCode.REFER_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE, adjudicator = "testing")
        it.addOutcome(Outcome(code = code, oicHearingId = 1))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(anyOrNull(), any(), any())
    }

    @EnumSource(OicHearingType::class)
    @ParameterizedTest
    fun `adjudicator should only be sent if GOV_UK, GOV_ADULT, GOV_YOI`(oicHearingType: OicHearingType) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().oicHearingType = oicHearingType
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      val hearing = reportedAdjudication.hearings.first()
      val request = createHearingResultRequestForVerify(reportedAdjudication, hearing)

      verify(prisonApiGateway, atLeastOnce()).amendHearingResult(
        adjudicationNumber = reportedAdjudication.reportNumber,
        oicHearingId = hearing.oicHearingId,
        oicHearingResultRequest = request,
      )
    }

    @Test
    fun `REFER_INAD calls prison api to update hearing only`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = 1
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).amendHearingResult(anyOrNull(), any(), any())

      verify(prisonApiGateway, atLeastOnce()).amendHearing(
        reportedAdjudication.reportNumber,
        1,
        OicHearingRequest(
          reportedAdjudication.hearings.first().dateTimeOfHearing,
          reportedAdjudication.hearings.first().oicHearingType,
          reportedAdjudication.hearings.first().locationId,
          "testing",
          "REFER_INAD",
        ),
      )
    }
  }

  @Nested
  inner class DeleteHearingResult {

    @CsvSource("QUASHED", "NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun `oic hearing id not present on outcome exception `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.addOutcome(Outcome(code = code))
      }

      Assertions.assertThatThrownBy {
        nomisOutcomeService.deleteHearingResultIfApplicable(
          adjudicationNumber = reportedAdjudication.reportNumber,
          hearing = reportedAdjudication.getLatestHearing(),
          outcome = reportedAdjudication.latestOutcome()!!,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("oic hearing id not linked to outcome")
    }

    @CsvSource("PROSECUTION", "REFER_POLICE", "NOT_PROCEED")
    @ParameterizedTest
    fun `no hearing and outcome {0} does not call prison api `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.clear()
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).deleteHearing(any(), any())
      verify(prisonApiGateway, never()).deleteHearingResult(any(), any())
    }

    @Test
    fun `quashed delete deletes hearing and result`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = 122
        it.addOutcome(
          Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              o ->
            o.createDateTime = LocalDateTime.now()
          },
        )
        it.addOutcome(
          Outcome(code = OutcomeCode.QUASHED, oicHearingId = 123L).also {
              o ->
            o.createDateTime = LocalDateTime.now().plusDays(1)
          },
        )
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, atLeastOnce()).deleteHearing(reportedAdjudication.reportNumber, 123L)
      verify(prisonApiGateway, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, 123L)
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun `{0} from hearing deletes hearing result and hearing `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.hearings.first().oicHearingId = 100L
        it.addOutcome(Outcome(code = code, oicHearingId = 122L))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, atLeastOnce()).deleteHearing(reportedAdjudication.reportNumber, 122L)
      verify(prisonApiGateway, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, 122L)
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} deletes hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = if (code == OutcomeCode.REFER_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE, adjudicator = "testing")
        it.hearings.first().oicHearingId = 100L
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      if (code == OutcomeCode.CHARGE_PROVED) {
        verify(prisonApiGateway, atLeastOnce()).deleteSanctions(any())
      }

      verify(prisonApiGateway, never()).deleteHearing(reportedAdjudication.reportNumber, 100L)
      verify(prisonApiGateway, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, 100L)
    }

    @Test
    fun `REFER_INAD calls does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = 1
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(prisonApiGateway, never()).deleteHearing(any(), any())
      verify(prisonApiGateway, never()).deleteHearingResult(any(), any())
    }
  }

  companion object {
    fun createHearingResultRequestForVerify(reportedAdjudication: ReportedAdjudication, hearing: Hearing): OicHearingResultRequest =
      when (hearing.oicHearingType) {
        OicHearingType.GOV_ADULT, OicHearingType.GOV_YOI, OicHearingType.GOV ->
          OicHearingResultRequest(
            pleaFindingCode = hearing.hearingOutcome!!.plea!!.plea,
            findingCode = reportedAdjudication.getOutcomes().first().code.finding!!,
            adjudicator = hearing.hearingOutcome!!.adjudicator,
          )
        OicHearingType.INAD_YOI, OicHearingType.INAD_ADULT ->
          OicHearingResultRequest(
            pleaFindingCode = hearing.hearingOutcome!!.plea!!.plea,
            findingCode = reportedAdjudication.getOutcomes().first().code.finding!!,
          )
      }
  }
}
