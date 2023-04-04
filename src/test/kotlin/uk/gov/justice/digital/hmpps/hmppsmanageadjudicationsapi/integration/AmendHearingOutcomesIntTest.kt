package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class AmendHearingOutcomesIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `amend hearing outcome test - before - refer police, after - refer police`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createReferral(code = HearingOutcomeCode.REFER_POLICE)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.REFER_POLICE.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - refer inad, after - refer inad`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createReferral(code = HearingOutcomeCode.REFER_INAD)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.REFER_INAD.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - adjourn, after - adjourn`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createAdjourn()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.ADJOURNED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "plea" to HearingOutcomePlea.NOT_ASKED,
          "adjournReason" to HearingOutcomeAdjournReason.LEGAL_REPRESENTATION,
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.ADJOURNED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
      .isEqualTo(HearingOutcomePlea.NOT_ASKED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.reason")
      .isEqualTo(HearingOutcomeAdjournReason.LEGAL_REPRESENTATION.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - charge proved, after - charge proved`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createChargeProved()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.CHARGE_PROVED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "plea" to HearingOutcomePlea.NOT_ASKED,
          "amount" to 100.99,
          "damagesOwed" to true,
          "caution" to false,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
      .isEqualTo(HearingOutcomePlea.NOT_ASKED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount")
      .isEqualTo(100.99)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.caution")
      .isEqualTo(false)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - not proceed, after - not proceed`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createNotProceed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.NOT_PROCEED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "plea" to HearingOutcomePlea.NOT_ASKED,
          "notProceedReason" to NotProceedReason.ANOTHER_WAY,
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
      .isEqualTo(HearingOutcomePlea.NOT_ASKED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason")
      .isEqualTo(NotProceedReason.ANOTHER_WAY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated details")
  }

  @Test
  fun `amend hearing outcome test - before - dismissed, after - dismissed`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createDismissed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.DISMISSED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "plea" to HearingOutcomePlea.NOT_ASKED,
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.DISMISSED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
      .isEqualTo(HearingOutcomePlea.NOT_ASKED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated details")
  }

  @CsvSource(
    "REFER_POLICE, REFER_INAD", "REFER_POLICE, ADJOURNED", "REFER_POLICE, DISMISSED", "REFER_POLICE, NOT_PROCEED", "REFER_POLICE, CHARGE_PROVED",
    "REFER_INAD, REFER_POLICE", "REFER_INAD, ADJOURNED", "REFER_INAD, DISMISSED", "REFER_INAD, NOT_PROCEED", "REFER_INAD, CHARGE_PROVED",
    "ADJOURNED, REFER_POLICE", "ADJOURNED, REFER_INAD", "ADJOURNED, DISMISSED", "ADJOURNED, NOT_PROCEED", "ADJOURNED, CHARGE_PROVED",
    "DISMISSED, REFER_POLICE", "DISMISSED, REFER_INAD", "DISMISSED, ADJOURNED", "DISMISSED, NOT_PROCEED", "DISMISSED, CHARGE_PROVED",
    "NOT_PROCEED, REFER_POLICE", "NOT_PROCEED, REFER_INAD", "NOT_PROCEED, ADJOURNED", "NOT_PROCEED, DISMISSED", "NOT_PROCEED, CHARGE_PROVED",
    "CHARGE_PROVED, REFER_POLICE", "CHARGE_PROVED, REFER_INAD", "CHARGE_PROVED, ADJOURNED", "CHARGE_PROVED, DISMISSED", "CHARGE_PROVED, NOT_PROCEED",
  )
  @ParameterizedTest
  fun `amend hearing outcome from {0} to {1}`(from: ReportedAdjudicationStatus, to: ReportedAdjudicationStatus) {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().also {
      when (from) {
        ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD -> it.createReferral(HearingOutcomeCode.valueOf(from.name))
        ReportedAdjudicationStatus.DISMISSED -> it.createDismissed()
        ReportedAdjudicationStatus.NOT_PROCEED -> it.createNotProceed()
        ReportedAdjudicationStatus.ADJOURNED -> it.createAdjourn()
        ReportedAdjudicationStatus.CHARGE_PROVED -> it.createChargeProved()
        else -> throw RuntimeException("not valid test data")
      }
    }

    when (to) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD -> amendOutcomeRequest(
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
        ),
        to,
      )
      ReportedAdjudicationStatus.DISMISSED -> amendOutcomeRequest(
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
        ),
        to,
      )
      ReportedAdjudicationStatus.NOT_PROCEED -> amendOutcomeRequest(
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
          notProceedReason = NotProceedReason.EXPIRED_NOTICE,
        ),
        to,
      )
      ReportedAdjudicationStatus.ADJOURNED -> amendOutcomeRequest(
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
          adjournReason = HearingOutcomeAdjournReason.MCKENZIE,
        ),
        to,
      )
      ReportedAdjudicationStatus.CHARGE_PROVED -> amendOutcomeRequest(
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          plea = HearingOutcomePlea.GUILTY,
          amount = 999.99,
          caution = false,
          damagesOwed = true,
        ),
        to,
      )
      else -> throw RuntimeException("invalid")
    }.expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator").isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(to.name).also {
        when (to) {
          ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details").isEqualTo("updated details")
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(to.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(to.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")
          ReportedAdjudicationStatus.DISMISSED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea").isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.DISMISSED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")
          ReportedAdjudicationStatus.NOT_PROCEED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea").isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason").isEqualTo(NotProceedReason.EXPIRED_NOTICE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")
          ReportedAdjudicationStatus.ADJOURNED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea").isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.ADJOURN.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details").isEqualTo("updated details")
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.reason").isEqualTo(HearingOutcomeAdjournReason.MCKENZIE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()
          ReportedAdjudicationStatus.CHARGE_PROVED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea").isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").isEqualTo(999.99)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.caution").isEqualTo(false)
          else -> {}
        }
      }
  }

  @Test
  fun `amend hearing outcome - charge proved where the amount is no longer required `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createChargeProved()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.CHARGE_PROVED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "caution" to false,
          "damagesOwed" to false,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.caution").isEqualTo(false)
  }

  @Test
  fun `amend outcome - police refer without hearing, schedule hearing, adjourn then amend to dismissed `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createOutcomeReferPolice().createHearing().createAdjourn()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.DISMISSED.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "its now dismissed",
          "plea" to HearingOutcomePlea.GUILTY,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.DISMISSED.name)
  }

  @Test
  fun `attempt to edit referral when an outcome is present - expected to fail currently`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createReferral(HearingOutcomeCode.REFER_POLICE).createOutcomeNotProceed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/${ReportedAdjudicationStatus.REFER_POLICE.name}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "details" to "updated details",
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  private fun amendOutcomeRequest(request: AmendHearingOutcomeRequest, to: ReportedAdjudicationStatus): WebTestClient.ResponseSpec =
    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/outcome/$to")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        objectMapper.writeValueAsString(request),
      )
      .exchange()
}
