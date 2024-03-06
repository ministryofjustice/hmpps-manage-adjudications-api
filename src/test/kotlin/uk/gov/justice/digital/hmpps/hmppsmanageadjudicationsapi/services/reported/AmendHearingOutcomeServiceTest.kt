package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase.Companion.REPORTED_ADJUDICATION_DTO
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.CombinedOutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferToGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.AmendHearingOutcomeService.Companion.mapStatusToOutcomeCode

class AmendHearingOutcomeServiceTest : ReportedAdjudicationTestBase() {

  private val hearingOutcomeService: HearingOutcomeService = mock()
  private val outcomeService: OutcomeService = mock()
  private val referralService: ReferralService = mock()
  private val completedHearingService: CompletedHearingService = mock()
  private val reportedAdjudicationService: ReportedAdjudicationService = mock()
  private val amendHearingOutcomeService = AmendHearingOutcomeService(
    hearingOutcomeService,
    outcomeService,
    referralService,
    completedHearingService,
    reportedAdjudicationService,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not applicable
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeSame {

    @CsvSource("REFER_POLICE, REFER_POLICE", "REFER_INAD, REFER_INAD", "ADJOURNED, ADJOURN", "CHARGE_PROVED, COMPLETE", "DISMISSED, COMPLETE", "NOT_PROCEED, COMPLETE", "REFER_GOV, REFER_GOV")
    @ParameterizedTest
    fun `updating the same type calls correct services for simple updates `(status: ReportedAdjudicationStatus, code: HearingOutcomeCode) {
      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
        Pair(status, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
      )

      val request = createRequest(status)

      whenever(outcomeService.amendOutcomeViaService(any(), any(), anyOrNull(), anyOrNull())).thenReturn(
        REPORTED_ADJUDICATION_DTO.also {
          it.punishmentsRemoved = false
        },
      )
      whenever(hearingOutcomeService.amendHearingOutcome(any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(REPORTED_ADJUDICATION_DTO)

      val response = amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = status,
        amendHearingOutcomeRequest = request,
      )

      verify(hearingOutcomeService, atLeastOnce()).getCurrentStatusAndLatestOutcome(chargeNumber = "1")
      verify(hearingOutcomeService, atLeastOnce()).amendHearingOutcome(
        chargeNumber = "1",
        outcomeCodeToAmend = code,
        adjudicator = request.adjudicator,
        details = request.details,
        plea = request.plea,
        adjournedReason = request.adjournReason,
      )

      if (code != HearingOutcomeCode.ADJOURN && status != ReportedAdjudicationStatus.CHARGE_PROVED) {
        verify(outcomeService, atLeastOnce()).amendOutcomeViaService(
          chargeNumber = "1",
          outcomeCodeToAmend = status.mapStatusToOutcomeCode()!!,
          details = request.details,
          notProceedReason = request.notProceedReason,
          referToGovReason = request.referToGovReason,
        )
      }

      Assertions.assertThat(response.punishmentsRemoved).isFalse
    }

    @CsvSource("ACCEPTED", "SCHEDULED", "UNSCHEDULED", "REJECTED", "RETURNED", "PROSECUTION", "QUASHED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `throws validation exception if status is not editable `(status: ReportedAdjudicationStatus) {
      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
        Pair(status, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
      )

      Assertions.assertThatThrownBy {
        amendHearingOutcomeService.amendHearingOutcome(
          chargeNumber = "1",
          status = status,
          amendHearingOutcomeRequest = AmendHearingOutcomeRequest(),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend from this status")
    }
  }

  @Nested
  inner class AmendHearingOutcomeWhenTypeHasChanged {

    @CsvSource(
      "REFER_POLICE, REFER_INAD", "REFER_POLICE, ADJOURNED", "REFER_POLICE, DISMISSED", "REFER_POLICE, NOT_PROCEED", "REFER_POLICE, CHARGE_PROVED",
      "REFER_INAD, REFER_POLICE", "REFER_INAD, ADJOURNED", "REFER_INAD, DISMISSED", "REFER_INAD, NOT_PROCEED", "REFER_INAD, CHARGE_PROVED",
      "ADJOURNED, REFER_POLICE", "ADJOURNED, REFER_INAD", "ADJOURNED, DISMISSED", "ADJOURNED, NOT_PROCEED", "ADJOURNED, CHARGE_PROVED",
      "DISMISSED, REFER_POLICE", "DISMISSED, REFER_INAD", "DISMISSED, ADJOURNED", "DISMISSED, NOT_PROCEED", "DISMISSED, CHARGE_PROVED",
      "NOT_PROCEED, REFER_POLICE", "NOT_PROCEED, REFER_INAD", "NOT_PROCEED, ADJOURNED", "NOT_PROCEED, DISMISSED", "NOT_PROCEED, CHARGE_PROVED",
      "CHARGE_PROVED, REFER_POLICE", "CHARGE_PROVED, REFER_INAD", "CHARGE_PROVED, ADJOURNED", "CHARGE_PROVED, DISMISSED", "CHARGE_PROVED, NOT_PROCEED",
      "REFER_INAD, REFER_GOV",
    )
    @ParameterizedTest
    fun `amending hearing outcome to a new type calls correct services`(from: ReportedAdjudicationStatus, to: ReportedAdjudicationStatus) {
      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
        Pair(from, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
      )

      whenever(outcomeService.getOutcomes(any())).thenReturn(
        listOf(
          CombinedOutcomeDto(
            outcome = OutcomeDto(id = 1, code = OutcomeCode.PROSECUTION),
          ),
        ),
      )

      whenever(referralService.createReferral(any(), any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      whenever(hearingOutcomeService.createAdjourn(any(), any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      whenever(completedHearingService.createDismissed(any(), any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      whenever(completedHearingService.createChargeProved(any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)
      whenever(completedHearingService.createNotProceed(any(), any(), any(), any(), any(), any())).thenReturn(REPORTED_ADJUDICATION_DTO)

      val request = createRequest(to)

      val response = amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = to,
        amendHearingOutcomeRequest = request,
      )

      when (from) {
        ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
          verify(referralService, atLeastOnce()).removeReferral("1")
        ReportedAdjudicationStatus.DISMISSED, ReportedAdjudicationStatus.CHARGE_PROVED, ReportedAdjudicationStatus.NOT_PROCEED ->
          verify(completedHearingService, atLeastOnce()).removeOutcome("1")
        ReportedAdjudicationStatus.ADJOURNED ->
          verify(hearingOutcomeService, atLeastOnce()).removeAdjourn("1", false)
        else -> {}
      }

      when (to) {
        ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
          verify(referralService, atLeastOnce()).createReferral(
            chargeNumber = "1",
            code = HearingOutcomeCode.valueOf(to.name),
            adjudicator = request.adjudicator!!,
            details = request.details!!,
            validate = false,
          )
        ReportedAdjudicationStatus.REFER_GOV ->
          verify(referralService, atLeastOnce()).createReferral(
            chargeNumber = "1",
            code = HearingOutcomeCode.valueOf(to.name),
            adjudicator = request.adjudicator!!,
            details = request.details!!,
            referToGovReason = ReferToGovReason.OTHER,
            validate = false,
          )
        ReportedAdjudicationStatus.DISMISSED ->
          verify(completedHearingService, atLeastOnce()).createDismissed("1", request.adjudicator!!, request.plea!!, request.details!!, false)
        ReportedAdjudicationStatus.NOT_PROCEED ->
          verify(completedHearingService, atLeastOnce()).createNotProceed("1", request.adjudicator!!, request.plea!!, request.notProceedReason!!, request.details!!, false)
        ReportedAdjudicationStatus.ADJOURNED ->
          verify(hearingOutcomeService, atLeastOnce()).createAdjourn("1", request.adjudicator!!, request.adjournReason!!, request.details!!, request.plea!!)
        ReportedAdjudicationStatus.CHARGE_PROVED ->
          verify(completedHearingService, atLeastOnce()).createChargeProved("1", request.adjudicator!!, request.plea!!, false)

        else -> {}
      }

      if (from == ReportedAdjudicationStatus.CHARGE_PROVED) {
        Assertions.assertThat(response.punishmentsRemoved).isTrue
      } else {
        Assertions.assertThat(response.punishmentsRemoved).isFalse
      }
    }

    @CsvSource(
      "ACCEPTED,CHARGE_PROVED", "RETURNED, CHARGE_PROVED", "REJECTED, CHARGE_PROVED", "SCHEDULED, CHARGE_PROVED",
      "UNSCHEDULED, CHARGE_PROVED", "QUASHED, CHARGE_PROVED", "PROSECUTION, CHARGE_PROVED", "AWAITING_REVIEW, CHARGE_PROVED",
      "CHARGE_PROVED, ACCEPTED", "CHARGE_PROVED, RETURNED", "CHARGE_PROVED, REJECTED", "CHARGE_PROVED, SCHEDULED",
      "CHARGE_PROVED, UNSCHEDULED", "CHARGE_PROVED, QUASHED", "CHARGE_PROVED, PROSECUTION", "CHARGE_PROVED, AWAITING_REVIEW",
    )
    @ParameterizedTest
    fun `throws validation exception if status is not editable `(from: ReportedAdjudicationStatus, to: ReportedAdjudicationStatus) {
      whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
        Pair(from, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
      )

      val direction = if (from == ReportedAdjudicationStatus.CHARGE_PROVED) "to $to" else "from $from"

      Assertions.assertThatThrownBy {
        amendHearingOutcomeService.amendHearingOutcome(
          chargeNumber = "1",
          status = to,
          amendHearingOutcomeRequest = AmendHearingOutcomeRequest(),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend $direction")
    }
  }

  @CsvSource("REFER_POLICE", "REFER_INAD", "NOT_PROCEED", "ADJOURNED", "DISMISSED", "REFER_GOV")
  @ParameterizedTest
  fun `throws missing details exception `(to: ReportedAdjudicationStatus) {
    whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
      Pair(ReportedAdjudicationStatus.CHARGE_PROVED, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
    )

    Assertions.assertThatThrownBy {
      amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = to,
        amendHearingOutcomeRequest = AmendHearingOutcomeRequest(),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("missing details")
  }

  @CsvSource("CHARGE_PROVED", "NOT_PROCEED", "ADJOURNED", "DISMISSED")
  @ParameterizedTest
  fun `throws missing plea exception `(to: ReportedAdjudicationStatus) {
    whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
      Pair(ReportedAdjudicationStatus.REFER_POLICE, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
    )

    Assertions.assertThatThrownBy {
      amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = to,
        amendHearingOutcomeRequest = AmendHearingOutcomeRequest(
          details = "",
        ),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("missing plea")
  }

  @Test
  fun `throws missing adjourn reason `() {
    whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
      Pair(ReportedAdjudicationStatus.CHARGE_PROVED, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
    )

    Assertions.assertThatThrownBy {
      amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = ReportedAdjudicationStatus.ADJOURNED,
        amendHearingOutcomeRequest = AmendHearingOutcomeRequest(
          details = "",
          plea = HearingOutcomePlea.GUILTY,
        ),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("missing reason")
  }

  @Test
  fun `throws missing not proceed reason `() {
    whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
      Pair(ReportedAdjudicationStatus.CHARGE_PROVED, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
    )

    Assertions.assertThatThrownBy {
      amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = ReportedAdjudicationStatus.NOT_PROCEED,
        amendHearingOutcomeRequest = AmendHearingOutcomeRequest(
          details = "",
          plea = HearingOutcomePlea.GUILTY,
        ),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("missing reason")
  }

  @CsvSource("REFER_INAD, ADJOURNED", "REFER_POLICE, ADJOURNED", "ADJOURNED, REFER_INAD", "ADJOURNED, REFER_POLICE", "REFER_GOV, ADJOURNED", "ADJOURNED, REFER_GOV")
  @ParameterizedTest
  fun `throws validation exception if referral has outcome `(from: ReportedAdjudicationStatus, to: ReportedAdjudicationStatus) {
    whenever(hearingOutcomeService.getCurrentStatusAndLatestOutcome("1")).thenReturn(
      Pair(from, HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")),
    )

    whenever(reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")).thenReturn(true)

    Assertions.assertThatThrownBy {
      amendHearingOutcomeService.amendHearingOutcome(
        chargeNumber = "1",
        status = to,
        amendHearingOutcomeRequest = AmendHearingOutcomeRequest(
          details = "",
          plea = HearingOutcomePlea.GUILTY,
        ),
      )
    }.isInstanceOf(ValidationException::class.java)
      .hasMessageContaining("referral has outcome - unable to amend")
  }

  companion object {

    fun createRequest(status: ReportedAdjudicationStatus): AmendHearingOutcomeRequest =
      when (status) {
        ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD -> AmendHearingOutcomeRequest(adjudicator = "test", details = "details")
        ReportedAdjudicationStatus.REFER_GOV -> AmendHearingOutcomeRequest(adjudicator = "test", details = "details", referToGovReason = ReferToGovReason.OTHER)
        ReportedAdjudicationStatus.DISMISSED -> AmendHearingOutcomeRequest(adjudicator = "test", details = "details", plea = HearingOutcomePlea.GUILTY)
        ReportedAdjudicationStatus.NOT_PROCEED -> AmendHearingOutcomeRequest(adjudicator = "test", details = "details", notProceedReason = NotProceedReason.NOT_FAIR, plea = HearingOutcomePlea.GUILTY)
        ReportedAdjudicationStatus.ADJOURNED -> AmendHearingOutcomeRequest(adjudicator = "test", details = "details", adjournReason = HearingOutcomeAdjournReason.HELP, plea = HearingOutcomePlea.GUILTY)
        ReportedAdjudicationStatus.CHARGE_PROVED -> AmendHearingOutcomeRequest(adjudicator = "test", plea = HearingOutcomePlea.GUILTY)
        else -> throw RuntimeException("not supported")
      }
  }
}
