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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingResultRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventWrapperService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome
import java.time.LocalDateTime

class NomisOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val eventWrapperService: EventWrapperService = mock()
  private val nomisOutcomeService = NomisOutcomeService(eventWrapperService)

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
      verify(eventWrapperService, never()).createHearing(any(), any())
      verify(eventWrapperService, never()).createHearingResult(anyOrNull(), any(), any())
    }

    @Test
    fun `quashed creates hearing and result`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = "122"
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

      whenever(eventWrapperService.createHearing(any(), any())).thenReturn("123")

      val hearingId = nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      assertThat(hearingId).isNotNull
      verify(eventWrapperService, atLeastOnce()).quashSanctions(any())
      verify(eventWrapperService, atLeastOnce()).createHearing(any(), any())
      verify(eventWrapperService, atLeastOnce()).createHearingResult(
        reportedAdjudication.reportNumber,
        "123",
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
        it.hearings.first().oicHearingId = "122"
        it.addOutcome(Outcome(code = code))
      }

      whenever(eventWrapperService.createHearing(any(), any())).thenReturn("123")

      val hearingId = nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      assertThat(hearingId).isNotNull
      verify(eventWrapperService, atLeastOnce()).createHearing(any(), any())
      verify(eventWrapperService, atLeastOnce()).createHearingResult(
        reportedAdjudication.reportNumber,
        "123",
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

      verify(eventWrapperService, never()).createHearing(any(), any())
      verify(eventWrapperService, atLeastOnce()).createHearingResult(any(), any(), any())
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

      verify(eventWrapperService, never()).createHearing(any(), any())
      verify(eventWrapperService, atLeastOnce()).createHearingResult(
        adjudicationNumber = reportedAdjudication.reportNumber,
        oicHearingId = hearing.oicHearingId,
        oicHearingResultRequest = request,
      )
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = "1"
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.createHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )
      verify(eventWrapperService, never()).createHearing(any(), any())
      verify(eventWrapperService, never()).createHearingResult(anyOrNull(), any(), any())
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
        it.addOutcome(Outcome(code = OutcomeCode.QUASHED, oicHearingId = "1"))
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
      verify(eventWrapperService, never()).amendHearingResult(any(), any(), any())
    }

    @Test
    fun `quashed amend`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = "122"
        it.addOutcome(
          Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              o ->
            o.createDateTime = LocalDateTime.now()
          },
        )
        it.addOutcome(
          Outcome(code = OutcomeCode.QUASHED, oicHearingId = "123").also {
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

      verify(eventWrapperService, never()).createHearing(anyOrNull(), any())
      verify(eventWrapperService, atLeastOnce()).amendHearingResult(
        reportedAdjudication.reportNumber,
        "123",
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
        it.hearings.first().oicHearingId = "122"
        it.addOutcome(Outcome(code = code, oicHearingId = "123"))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(eventWrapperService, never()).createHearing(anyOrNull(), any())
      verify(eventWrapperService, atLeastOnce()).amendHearingResult(
        reportedAdjudication.reportNumber,
        "123",
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
        it.addOutcome(Outcome(code = code, oicHearingId = "1"))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(eventWrapperService, atLeastOnce()).amendHearingResult(anyOrNull(), any(), any())
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

      verify(eventWrapperService, atLeastOnce()).amendHearingResult(
        adjudicationNumber = reportedAdjudication.reportNumber,
        oicHearingId = hearing.oicHearingId,
        oicHearingResultRequest = request,
      )
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = "1"
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.amendHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(eventWrapperService, never()).amendHearingResult(anyOrNull(), any(), any())
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

      verify(eventWrapperService, never()).deleteHearing(any(), any())
      verify(eventWrapperService, never()).deleteHearingResult(any(), any())
    }

    @Test
    fun `quashed delete deletes hearing and result`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "testing", plea = HearingOutcomePlea.GUILTY)
        it.hearings.first().oicHearingId = "122"
        it.addOutcome(
          Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              o ->
            o.createDateTime = LocalDateTime.now()
          },
        )
        it.addOutcome(
          Outcome(code = OutcomeCode.QUASHED, oicHearingId = "123").also {
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

      verify(eventWrapperService, atLeastOnce()).deleteHearing(reportedAdjudication.reportNumber, "123")
      verify(eventWrapperService, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, "123")
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION")
    @ParameterizedTest
    fun `{0} from hearing deletes hearing result and hearing `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "testing")
        it.hearings.first().oicHearingId = "100"
        it.addOutcome(Outcome(code = code, oicHearingId = "122"))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(eventWrapperService, atLeastOnce()).deleteHearing(reportedAdjudication.reportNumber, "122")
      verify(eventWrapperService, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, "122")
    }

    @CsvSource("REFER_POLICE", "NOT_PROCEED", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `hearing with outcome {0} deletes hearing result `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = if (code == OutcomeCode.REFER_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE, adjudicator = "testing")
        it.hearings.first().oicHearingId = "100"
        it.addOutcome(Outcome(code = code))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      if (code == OutcomeCode.CHARGE_PROVED) {
        verify(eventWrapperService, atLeastOnce()).deleteSanctions(any())
      }

      verify(eventWrapperService, never()).deleteHearing(reportedAdjudication.reportNumber, "100")
      verify(eventWrapperService, atLeastOnce()).deleteHearingResult(reportedAdjudication.reportNumber, "100")
    }

    @Test
    fun `REFER_INAD does not call prison api`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "testing")
        it.hearings.first().oicHearingId = "1"
        it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD))
      }

      nomisOutcomeService.deleteHearingResultIfApplicable(
        adjudicationNumber = reportedAdjudication.reportNumber,
        hearing = reportedAdjudication.getLatestHearing(),
        outcome = reportedAdjudication.latestOutcome()!!,
      )

      verify(eventWrapperService, never()).deleteHearing(any(), any())
      verify(eventWrapperService, never()).deleteHearingResult(any(), any())
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
