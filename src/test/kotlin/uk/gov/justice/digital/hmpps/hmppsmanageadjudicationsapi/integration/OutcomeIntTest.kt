package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

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
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `delete an outcome - not proceed `() {
    initDataForOutcome().createOutcomeNotProceed().expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
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
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
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
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
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
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.DISMISSED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.DISMISSED.name)
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
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").isEqualTo(HearingOutcomePlea.NOT_GUILTY.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").isEqualTo(100.50)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.caution").isEqualTo(true)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
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
        ),
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
        ),
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
        ),
      )
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `remove completed hearing outcome `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-completed-hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome").doesNotExist()
  }

  @Test
  fun `quash completed hearing outcome `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome/quashed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "reason" to QuashedReason.APPEAL_UPHELD,
          "details" to "details",
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.QUASHED.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.quashedReason").isEqualTo(QuashedReason.APPEAL_UPHELD.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.QUASHED.name)
  }

  @Test
  fun `remove quashed outcome `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved().createQuashed()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()")
      .isEqualTo(1)
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
  }

  @Test
  fun `amend outcome - not proceed without hearing `() {
    initDataForOutcome().createOutcomeNotProceed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "updated",
          "reason" to NotProceedReason.WITNESS_NOT_ATTEND,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.reason")
      .isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND.name)
  }

  @Test
  fun `amend outcome - refer police without hearing `() {
    initDataForOutcome().createOutcomeReferPolice()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "updated",
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details")
      .isEqualTo("updated")
  }

  @Test
  fun `amend outcome - quashed `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved().createQuashed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "updated",
          "quashedReason" to QuashedReason.JUDICIAL_REVIEW,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.QUASHED.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.details")
      .isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.quashedReason")
      .isEqualTo(QuashedReason.JUDICIAL_REVIEW.name)
  }

  @Test
  fun `amend outcome - not proceed when its a referral outcome`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createReferral(HearingOutcomeCode.REFER_POLICE).createOutcomeNotProceed()

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "details" to "updated",
          "reason" to NotProceedReason.WITNESS_NOT_ATTEND,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.details")
      .isEqualTo("updated")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.reason")
      .isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND.name)
  }

  @Test
  fun `refer police, schedule hearing, adjourn, scheduled hearing, refer inad, then remove referral and hearing `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    initDataForOutcome().createOutcomeReferPolice()
      .createHearing(dateTimeOfHearing = LocalDateTime.now())
      .createAdjourn()
      .createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1))
      .createReferral(code = HearingOutcomeCode.REFER_INAD)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.ADJOURNED.name)
  }

  @Test
  fun `issue around removing the scheduled hearing outcome on delete hearing `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    initDataForOutcome()
      .createOutcomeReferPolice()
      .createHearing(dateTimeOfHearing = LocalDateTime.now())
      .createAdjourn()
      .createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1))
      .createReferral(code = HearingOutcomeCode.REFER_INAD)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.ADJOURNED.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
  }
}
