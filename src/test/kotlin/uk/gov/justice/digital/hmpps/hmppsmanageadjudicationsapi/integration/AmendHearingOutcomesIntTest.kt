package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AmendHearingOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

@Import(TestOAuth2Config::class)
class AmendHearingOutcomesIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `amend hearing outcome test - before - refer police, after - refer police`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createReferral(code = HearingOutcomeCode.REFER_POLICE)
        .getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.REFER_POLICE.name}/v2")
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
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createReferral(code = HearingOutcomeCode.REFER_INAD)
        .getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.REFER_INAD.name}/v2")
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
  fun `amend hearing outcome test - before - refer gov, after - refer gov`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT)
        .createReferral(code = HearingOutcomeCode.REFER_GOV).getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.REFER_GOV.name}/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "details" to "updated details",
          "referGovReason" to ReferGovReason.REVIEW_FOR_REFER_POLICE,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.referGovReason")
      .isEqualTo(ReferGovReason.REVIEW_FOR_REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details")
      .isEqualTo("updated details")
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - adjourn, after - adjourn`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createAdjourn().getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.ADJOURNED.name}/v2")
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
  fun `amend hearing outcome test - before - charge proved, after - charge proved v2`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createChargeProved().getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.CHARGE_PROVED.name}/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "updated adjudicator",
          "plea" to HearingOutcomePlea.NOT_ASKED,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
      .isEqualTo(HearingOutcomePlea.NOT_ASKED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }

  @Test
  fun `amend hearing outcome test - before - not proceed, after - not proceed`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createNotProceed().getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.NOT_PROCEED.name}/v2")
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
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createHearing().createDismissed().getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.DISMISSED.name}/v2")
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
    "REFER_POLICE, REFER_INAD",
    "REFER_POLICE, ADJOURNED",
    "REFER_POLICE, DISMISSED",
    "REFER_POLICE, NOT_PROCEED",
    "REFER_POLICE, CHARGE_PROVED",
    "REFER_INAD, ADJOURNED",
    "REFER_INAD, DISMISSED",
    "REFER_INAD, NOT_PROCEED",
    "REFER_INAD, CHARGE_PROVED",
    "ADJOURNED, REFER_POLICE",
    "ADJOURNED, REFER_INAD",
    "ADJOURNED, DISMISSED",
    "ADJOURNED, NOT_PROCEED",
    "ADJOURNED, CHARGE_PROVED",
    "DISMISSED, REFER_POLICE",
    "DISMISSED, REFER_INAD",
    "DISMISSED, ADJOURNED",
    "DISMISSED, NOT_PROCEED",
    "DISMISSED, CHARGE_PROVED",
    "NOT_PROCEED, REFER_POLICE",
    "NOT_PROCEED, REFER_INAD",
    "NOT_PROCEED, ADJOURNED",
    "NOT_PROCEED, DISMISSED",
    "NOT_PROCEED, CHARGE_PROVED",
    "CHARGE_PROVED, REFER_POLICE",
    "CHARGE_PROVED, REFER_INAD",
    "CHARGE_PROVED, ADJOURNED",
    "CHARGE_PROVED, DISMISSED",
    "CHARGE_PROVED, NOT_PROCEED",
    "REFER_GOV, REFER_POLICE",
    "REFER_GOV, ADJOURNED",
    "REFER_GOV, DISMISSED",
    "REFER_GOV, NOT_PROCEED",
    "REFER_GOV, CHARGE_PROVED",
  )
  @ParameterizedTest
  fun `amend hearing outcome v2 from {0} to {1}`(from: ReportedAdjudicationStatus, to: ReportedAdjudicationStatus) {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario =
      initDataForUnScheduled(testData = testData).createHearing(oicHearingType = if (from == ReportedAdjudicationStatus.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT)
        .also {
          when (from) {
            ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD, ReportedAdjudicationStatus.REFER_GOV -> it.createReferral(
              HearingOutcomeCode.valueOf(from.name),
            )

            ReportedAdjudicationStatus.DISMISSED -> it.createDismissed()
            ReportedAdjudicationStatus.NOT_PROCEED -> it.createNotProceed()
            ReportedAdjudicationStatus.ADJOURNED -> it.createAdjourn()
            ReportedAdjudicationStatus.CHARGE_PROVED -> it.createChargeProved()
            else -> throw RuntimeException("not valid test data")
          }
        }

    when (to) {
      ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_INAD -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
        ),
        to,
      )

      ReportedAdjudicationStatus.REFER_GOV -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          referGovReason = ReferGovReason.GOV_INQUIRY,
        ),
        to,
      )

      ReportedAdjudicationStatus.DISMISSED -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
        ),
        to,
      )

      ReportedAdjudicationStatus.NOT_PROCEED -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
          notProceedReason = NotProceedReason.EXPIRED_NOTICE,
        ),
        to,
      )

      ReportedAdjudicationStatus.ADJOURNED -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          details = "updated details",
          plea = HearingOutcomePlea.GUILTY,
          adjournReason = HearingOutcomeAdjournReason.MCKENZIE,
        ),
        to,
      )

      ReportedAdjudicationStatus.CHARGE_PROVED -> amendOutcomeRequest(
        chargeNumber = scenario.getGeneratedChargeNumber(),
        AmendHearingOutcomeRequest(
          adjudicator = "updated",
          plea = HearingOutcomePlea.GUILTY,
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

          ReportedAdjudicationStatus.REFER_GOV ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details").isEqualTo("updated details")
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(to.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(to.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.referGovReason")
              .isEqualTo(ReferGovReason.GOV_INQUIRY.name)

          ReportedAdjudicationStatus.DISMISSED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
              .isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code")
              .isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.DISMISSED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")

          ReportedAdjudicationStatus.NOT_PROCEED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
              .isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code")
              .isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code")
              .isEqualTo(OutcomeCode.NOT_PROCEED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason")
              .isEqualTo(NotProceedReason.EXPIRED_NOTICE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("updated details")

          ReportedAdjudicationStatus.ADJOURNED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
              .isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code")
              .isEqualTo(HearingOutcomeCode.ADJOURN.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.details").isEqualTo("updated details")
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.reason")
              .isEqualTo(HearingOutcomeAdjournReason.MCKENZIE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()

          ReportedAdjudicationStatus.CHARGE_PROVED ->
            it.jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.plea")
              .isEqualTo(HearingOutcomePlea.GUILTY.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code")
              .isEqualTo(HearingOutcomeCode.COMPLETE.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code")
              .isEqualTo(OutcomeCode.CHARGE_PROVED.name)
              .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").doesNotExist()

          else -> {}
        }
      }
  }

  @Test
  fun `amend outcome - police refer without hearing, schedule hearing, adjourn then amend to dismissed `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber =
      initDataForUnScheduled(testData = testData).createOutcomeReferPolice().createHearing().createAdjourn()
        .getGeneratedChargeNumber()

    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/${ReportedAdjudicationStatus.DISMISSED.name}/v2")
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
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario =
      initDataForUnScheduled(testData = testData).createHearing().createReferral(HearingOutcomeCode.REFER_POLICE)
        .createOutcomeNotProceed()

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/outcome/${ReportedAdjudicationStatus.REFER_POLICE.name}/v2")
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

  @Test
  fun `amend charge proved with punishments to dismissed removes punishments `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().createPunishments()

    amendOutcomeRequest(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      request = AmendHearingOutcomeRequest(adjudicator = "", plea = HearingOutcomePlea.GUILTY, details = ""),
      to = ReportedAdjudicationStatus.DISMISSED,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)
  }

  private fun amendOutcomeRequest(
    chargeNumber: String,
    request: AmendHearingOutcomeRequest,
    to: ReportedAdjudicationStatus,
  ): WebTestClient.ResponseSpec = webTestClient.put()
    .uri("/reported-adjudications/$chargeNumber/hearing/outcome/$to/v2")
    .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
    .bodyValue(
      objectMapper.writeValueAsString(request),
    )
    .exchange()
}
