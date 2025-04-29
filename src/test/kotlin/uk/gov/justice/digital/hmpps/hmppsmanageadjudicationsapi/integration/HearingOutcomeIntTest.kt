package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime
import java.util.*

@Import(TestOAuth2Config::class)
class HearingOutcomeIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Nested
  inner class Adjourn {
    @Test
    fun `create hearing outcome - adjourn`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createHearing()

      webTestClient.post()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/outcome/adjourn")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "adjudicator" to "test",
            "reason" to HearingOutcomeAdjournReason.LEGAL_ADVICE,
            "details" to "details",
            "plea" to HearingOutcomePlea.UNFIT,
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.ADJOURNED.name)
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
        .isEqualTo("test")
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason")
        .isEqualTo(HearingOutcomeAdjournReason.LEGAL_ADVICE.name)
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.details")
        .isEqualTo("details")
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea")
        .isEqualTo(HearingOutcomePlea.UNFIT.name)
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.ADJOURN.name)
        .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
        .jsonPath("$.reportedAdjudication.outcomes[0].hearing").exists()
        .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.ADJOURN.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome").doesNotExist()
    }

    @Test
    fun `remove adjourn outcome `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createHearing().createAdjourn()

      webTestClient.delete()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/outcome/adjourn")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()
        .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome").doesNotExist()
        .jsonPath("$.reportedAdjudication.outcomes[0].hearing").exists()
    }

    @Test
    fun `create hearing outcome - adjourn and then create new hearing`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createHearing().createAdjourn()

      webTestClient.post()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "locationId" to 1,
            "locationUuid" to UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
            "dateTimeOfHearing" to LocalDateTime.now(),
            "oicHearingType" to OicHearingType.GOV.name,
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.status")
        .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
    }
  }

  @Nested
  inner class Referral {
    @Test
    fun `create hearing outcome for referral`() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData).createHearing()
      webTestClient.post()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/outcome/referral")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "adjudicator" to "test",
            "code" to HearingOutcomeCode.REFER_POLICE,
            "details" to "details",
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.id").isNotEmpty
        .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
        .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
        .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.details").isEqualTo("details")
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator")
        .isEqualTo("test")
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.reason").doesNotExist()
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.plea").doesNotExist()
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.finding").doesNotExist()
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.details")
        .isEqualTo("details")
        .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.REFER_POLICE.name)
    }

    @Test
    fun `should only create one schedule hearing - refer police, adjourn, adjourn `() {
      val testData = IntegrationTestData.getDefaultAdjudication()
      val scenario = initDataForUnScheduled(testData = testData)
        .createOutcomeReferPolice()
        .createHearing(dateTimeOfHearing = LocalDateTime.now())
        .createAdjourn()

      webTestClient.post()
        .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .bodyValue(
          mapOf(
            "locationId" to 1,
            "locationUuid" to UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
            "dateTimeOfHearing" to LocalDateTime.now().plusDays(3),
            "oicHearingType" to OicHearingType.GOV.name,
          ),
        )
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome").doesNotExist()
    }
  }
}
