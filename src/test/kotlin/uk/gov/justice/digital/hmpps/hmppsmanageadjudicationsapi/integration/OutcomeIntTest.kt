package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

@Import(TestOAuth2Config::class)
class OutcomeIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Nested
  inner class NotProceed {
    @Test
    fun `create outcome - not proceed`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/outcome/not-proceed")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createOutcomeNotProceed()

      webTestClient.delete()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.outcomes.size()")
        .isEqualTo(0)
    }

    @Test
    fun `amend outcome - not proceed without hearing `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createOutcomeNotProceed()

      webTestClient.put()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome")
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
  }

  @Nested
  inner class ProsecutionAndPolice {
    @Test
    fun `refer to police leads to police prosecution`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      initDataForUnScheduled(testData = testData).createOutcomeReferPolice()

      integrationTestData().createOutcomeProsecution(testData).expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
        .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)
        .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
    }

    @Test
    fun `amend outcome - refer police without hearing `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createOutcomeReferPolice().getGeneratedChargeNumber()

      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumber/outcome")
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
  }

  @Nested
  inner class CompletedHearingOutcome {
    @Test
    fun `create completed hearing outcome - not proceed`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().getGeneratedChargeNumber()
      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/not-proceed")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().getGeneratedChargeNumber()
      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/dismissed")
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
    fun `create completed hearing outcome - charge proved v2`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().getGeneratedChargeNumber()
      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/charge-proved/v2")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "adjudicator" to "test",
            "plea" to HearingOutcomePlea.NOT_GUILTY,
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
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.amount").doesNotExist()
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
    }

    @Test
    fun `create completed hearing outcome - dismissed throws exception when hearing is missing`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/dismissed")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/not-proceed")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/complete-hearing/charge-proved")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().getGeneratedChargeNumber()

      webTestClient.delete()
        .uri("/reported-adjudications/$chargeNumber/remove-completed-hearing")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.status")
        .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()
        .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome").doesNotExist()
    }
  }

  @Nested
  inner class Quashed {
    @Test
    fun `quash completed hearing outcome `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().getGeneratedChargeNumber()

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/outcome/quashed")
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
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().createQuashed().getGeneratedChargeNumber()

      webTestClient.delete()
        .uri("/reported-adjudications/$chargeNumber/outcome")
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
    fun `amend outcome - quashed `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().createQuashed().getGeneratedChargeNumber()

      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumber/outcome")
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
  }

  @Nested
  inner class Referrals {

    @Test
    fun `create refer gov referral outcome`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      initDataForUnScheduled(testData = testData).createHearing().createReferral(HearingOutcomeCode.REFER_INAD).createOutcomeReferGov()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.status")
        .isEqualTo(ReportedAdjudicationStatus.REFER_GOV.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.referGovReason")
        .isEqualTo(ReferGovReason.OTHER.name)
    }

    @Test
    fun `amend outcome - not proceed when its a referral outcome`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing().createReferral(HearingOutcomeCode.REFER_POLICE).createOutcomeNotProceed().getGeneratedChargeNumber()

      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumber/outcome")
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
    fun `amend outcome - refer gov when its a referral outcome`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD).createOutcomeReferGov()
        .returnResult(ReportedAdjudicationResponse::class.java)
        .responseBody
        .blockFirst()!!
        .reportedAdjudication.chargeNumber

      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumber/outcome")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "details" to "updated",
            "referGovReason" to ReferGovReason.GOV_INQUIRY,
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.status")
        .isEqualTo(ReportedAdjudicationStatus.REFER_GOV.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.details")
        .isEqualTo("updated")
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.referGovReason")
        .isEqualTo(ReferGovReason.GOV_INQUIRY.name)
    }

    @Test
    fun `amend outcome - not proceed when its a ref gov referral outcome`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD).createOutcomeReferGov()
        .returnResult(ReportedAdjudicationResponse::class.java)
        .responseBody
        .blockFirst()!!
        .reportedAdjudication.chargeNumber

      integrationTestData().createOutcomeNotProceed(testDataSet = testData).expectStatus().isCreated

      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumber/outcome")
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
        .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
        .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.details")
        .isEqualTo("updated")
    }

    @Test
    fun `amend outcome - not proceed when its a ref gov hearing outcome`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT).createReferral(HearingOutcomeCode.REFER_GOV).createOutcomeNotProceed()

      webTestClient.put()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome")
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
        .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.details")
        .isEqualTo("updated")
    }

    @Test
    fun `refer police, schedule hearing, adjourn, scheduled hearing, refer inad, then remove referral and hearing `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val chargeNumber = initDataForUnScheduled(testData = testData).createOutcomeReferPolice()
        .createHearing(dateTimeOfHearing = LocalDateTime.now())
        .createAdjourn()
        .createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1))
        .createReferral(code = HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

      webTestClient.delete()
        .uri("/reported-adjudications/$chargeNumber/remove-referral")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)

      webTestClient.delete()
        .uri("/reported-adjudications/$chargeNumber/hearing/v2")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.status")
        .isEqualTo(ReportedAdjudicationStatus.ADJOURNED.name)
    }
  }

  @Test
  fun `issue around removing the scheduled hearing outcome on delete hearing `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData)
      .createOutcomeReferPolice()
      .createHearing(dateTimeOfHearing = LocalDateTime.now())
      .createAdjourn()
      .createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1))
      .createReferral(code = HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.outcome").doesNotExist()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
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

  @Test
  fun `remove charge proved with punishments removes punishments `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData)
      .createHearing(dateTimeOfHearing = LocalDateTime.now())
      .createChargeProved()
      .createPunishments()
      .getGeneratedChargeNumber()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-completed-hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()")
      .isEqualTo(0)
  }

  @Test
  fun `activate a suspended punishment, then remove the charge proved outcome and ensure the suspended punishment is available for selection again`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "OUTCO")
    val dummyCharge = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()
    val suspended = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().getGeneratedChargeNumber()
    val punishmentId = createPunishments(suspended).expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication.punishments.first().id

    val activated = initDataForUnScheduled(testData = testData).createHearing().createChargeProved().getGeneratedChargeNumber()
    createPunishments(chargeNumber = activated, activatedFrom = suspended, isSuspended = false, id = punishmentId).expectStatus().isCreated

    getSuspendedPunishments(chargeNumber = dummyCharge, prisonerNumber = testData.prisonerNumber).expectStatus().isOk
      .expectBody().jsonPath("$.size()").isEqualTo(0)

    webTestClient.delete()
      .uri("/reported-adjudications/$activated/remove-completed-hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk

    getSuspendedPunishments(chargeNumber = dummyCharge, prisonerNumber = testData.prisonerNumber).expectStatus().isOk
      .expectBody().jsonPath("$.size()").isEqualTo(1)
  }
}
