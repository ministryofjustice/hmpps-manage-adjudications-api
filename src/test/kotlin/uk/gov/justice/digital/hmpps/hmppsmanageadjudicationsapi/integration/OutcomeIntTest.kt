package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class OutcomeIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }
  @Test
  fun `create outcome - not proceed`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `delete an outcome - not proceed `() {
    initDataForOutcome().createOutcomeNotProceed().expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()")
      .isEqualTo(0)
      .jsonPath("$.reportedAdjudication.outcomes.size()")
      .isEqualTo(0)
  }

  @Test
  fun `refer to police leads to police prosecution`() {
    initDataForOutcome().createOutcomeReferPolice()

    integrationTestData().createOutcomeProsecution(
      IntegrationTestData.DEFAULT_ADJUDICATION,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `create completed hearing outcome - not proceed`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `create completed hearing outcome - dismissed`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/dismissed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "details" to "details",
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.DISMISSED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.DISMISSED.name)
  }

  @Test
  fun `create completed hearing outcome - charge proved`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/charge-proved")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "amount" to 100.50,
          "caution" to true,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.amount").isEqualTo(100.50)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.caution").isEqualTo(true)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
  }

  @Test
  fun `create completed hearing outcome - dismissed throws exception when hearing is missing`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/dismissed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "details" to "details",
        )
      )
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `create completed hearing outcome - not proceed throws exception when hearing is missing`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "reason" to NotProceedReason.NOT_FAIR,
          "details" to "details",
        )
      )
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `create completed hearing outcome - charge proved throws exception when hearing is missing`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/complete-hearing/charge-proved")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
          "caution" to false,
        )
      )
      .exchange()
      .expectStatus().isNotFound
  }

  protected fun initDataForOutcome(): IntegrationTestScenario {
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()
    val draftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(intTestData, this, draftUserHeaders)

    return draftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()
      .acceptReport(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())
  }
}
