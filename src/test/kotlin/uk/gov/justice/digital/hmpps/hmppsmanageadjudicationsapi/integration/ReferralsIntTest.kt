package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

@Import(TestOAuth2Config::class)
class ReferralsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @CsvSource("REFER_POLICE", "REFER_GOV", "REFER_INAD")
  @ParameterizedTest
  fun `hearing referral leads to NOT_PROCEED`(hearingOutcomeCode: HearingOutcomeCode) {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = if (hearingOutcomeCode == HearingOutcomeCode.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT).createReferral(hearingOutcomeCode)
      .getGeneratedChargeNumber()

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
  }

  @CsvSource("REFER_POLICE", "REFER_GOV", "REFER_INAD")
  @ParameterizedTest
  fun `remove referral with hearing`(hearingOutcomeCode: HearingOutcomeCode) {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = if (hearingOutcomeCode == HearingOutcomeCode.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT).createReferral(hearingOutcomeCode)
      .getGeneratedChargeNumber()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").doesNotExist()
  }

  @CsvSource("REFER_POLICE", "REFER_INAD", "REFER_GOV")
  @ParameterizedTest
  fun `remove referral with hearing and referral outcome`(hearingOutcomeCode: HearingOutcomeCode) {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = if (hearingOutcomeCode == HearingOutcomeCode.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT).createReferral(hearingOutcomeCode)

    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR,
        ),
      )
      .exchange().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").exists()

    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `remove referral without hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createOutcomeReferPolice()

    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `remove referral and referral outcome`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createOutcomeReferPolice().getGeneratedChargeNumber()

    integrationTestData().createOutcomeProsecution(
      testData,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing").doesNotExist()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `create police referral without hearing, schedule a new hearing and then remove it`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createOutcomeReferPolice()
    integrationTestData().createHearing(
      testDataSet = testData.also {
        it.chargeNumber = scenario.getGeneratedChargeNumber()
      },
    )
      .expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `remove referral, referral outcome and hearing outcome for a POLICE_REFER related to complex example, police refer - no hearing, inad refer, police refer, prosecute`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createOutcomeReferPolice()
    integrationTestData().createHearing(
      oicHearingType = OicHearingType.GOV_ADULT,
      testDataSet = testData.also {
        it.chargeNumber = scenario.getGeneratedChargeNumber()
      },
    )

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createHearing(testData, testData.dateTimeOfHearing!!.plusDays(1), OicHearingType.INAD_ADULT)

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createOutcomeProsecution(
      testData,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(2)

    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.hearings[1].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing.oicHearingType").isEqualTo(OicHearingType.INAD_ADULT.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing.outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `create hearing, refer to inad, and inad decides not to proceed, then remove referral`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()
    integrationTestData().createHearing(testData)

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createOutcomeNotProceed(
      testData,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_INAD.name)
  }

  @Test
  fun `create hearing, refer to inad, schedule inad hearing, then remove the hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

    integrationTestData().createHearing(oicHearingType = OicHearingType.GOV_ADULT, testDataSet = testData, dateTimeOfHearing = LocalDateTime.now())

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createHearing(testData, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `create hearing, refer to police, schedule hearing, then remove the hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()
    integrationTestData().createHearing(testData, LocalDateTime.now())

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createHearing(testData, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `police refer from hearing leads to prosecution, then referral is removed`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

    integrationTestData().createHearing(testData, LocalDateTime.now())

    integrationTestData().createReferral(
      testData,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createOutcomeProsecution(
      testData,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_POLICE.name)
  }

  @Test
  fun `REFER_INAD referral outcome of REFER_GOV`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
  }

  @Test
  fun `REFER_GOV referral outcome of NOT_PROCEED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT).createReferral(HearingOutcomeCode.REFER_GOV)

    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome/not-proceed")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR,
        ),
      )
      .exchange().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `REFER_GOV referral outcome of SCHEDULE_HEARING`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT).createReferral(HearingOutcomeCode.REFER_GOV)

    integrationTestData().createHearing(testData, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps NOT_PROCEED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    integrationTestData().createOutcomeNotProceed(testDataSet = testData)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps NOT_PROCEED - remove NOT PROCEED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication.chargeNumber

    integrationTestData().createOutcomeNotProceed(testDataSet = testData)
      .expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps SCHEDULE_HEARING`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    integrationTestData().createHearing(testDataSet = testData, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing").exists()
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome").doesNotExist()
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps SCHEDULE_HEARING - CHARGE_PROVED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()

    integrationTestData().createHearing(testDataSet = testData, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
      .expectStatus().isCreated
      .expectBody()

    val response = integrationTestData().createChargeProved(testDataSet = testData).reportedAdjudication

    assertThat(response.outcomes.size).isEqualTo(3)
    assertThat(response.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(response.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.REFER_GOV)
    assertThat(response.outcomes[1].outcome!!.referralOutcome).isNull()
    assertThat(response.outcomes[1].hearing).isNull()
    assertThat(response.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes.last().hearing).isNotNull
    assertThat(response.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    assertThat(response.outcomes.last().outcome!!.referralOutcome).isNull()
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps SCHEDULE_HEARING - CHARGE_PROVED - QUASHED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()

    integrationTestData().createHearing(testDataSet = testData, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
      .expectStatus().isCreated
      .expectBody()

    integrationTestData().createChargeProved(testDataSet = testData)
    val response = integrationTestData().createQuashed(testDataSet = testData).reportedAdjudication

    assertThat(response.outcomes.size).isEqualTo(4)
    assertThat(response.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(response.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.REFER_GOV)
    assertThat(response.outcomes[1].outcome!!.referralOutcome).isNull()
    assertThat(response.outcomes[1].hearing).isNull()
    assertThat(response.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[2].outcome!!.referralOutcome).isNull()
    assertThat(response.outcomes[2].hearing).isNotNull
    assertThat(response.outcomes[2].outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    assertThat(response.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.QUASHED)
    assertThat(response.outcomes.last().hearing).isNull()
  }

  @Test
  fun `REFER_INAD - SCHEDULE_HEARING - REFER_GOV - SCHEDULE_HEARING`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

    integrationTestData().createHearing(
      testData,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code")
      .isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing").exists()
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
          "referGovReason" to ReferGovReason.GOV_INQUIRY,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code")
      .isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    integrationTestData().createHearing(
      testDataSet = testData,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code")
      .isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome.code")
      .isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing").exists()
  }

  @Test
  fun `REFER_INAD - SCHEDULE_HEARING - REFER_GOV - SCHEDULE_HEARING - CHARGE_PROVED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

    integrationTestData().createHearing(
      testData,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
          "referGovReason" to ReferGovReason.GOV_INQUIRY,
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = testData,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()

    val response = integrationTestData().createChargeProved(testDataSet = testData).reportedAdjudication

    assertThat(response.outcomes.size).isEqualTo(3)
    assertThat(response.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(response.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_GOV)
    assertThat(response.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
  }

  @Test
  fun `REFER_INAD - SCHEDULE_HEARING - REFER_GOV - SCHEDULE_HEARING - CHARGE_PROVED - QUASHED`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

    integrationTestData().createHearing(
      testData,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
          "referGovReason" to ReferGovReason.GOV_INQUIRY,
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = testData,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()

    integrationTestData().createChargeProved(testDataSet = testData)
    val response = integrationTestData().createQuashed(testDataSet = testData).reportedAdjudication

    assertThat(response.outcomes.size).isEqualTo(4)
    assertThat(response.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(response.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_GOV)
    assertThat(response.outcomes[2].outcome!!.referralOutcome).isNull()
    assertThat(response.outcomes[2].outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    assertThat(response.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.QUASHED)
    assertThat(response.outcomes.last().hearing).isNull()
  }

  @Test
  fun `REFER_INAD - SCHEDULE_HEARING - REFER_GOV - SCHEDULE_HEARING - remove hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD).getGeneratedChargeNumber()

    integrationTestData().createHearing(
      testData,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
          "referGovReason" to ReferGovReason.GOV_INQUIRY,
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = testData,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    ).expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code")
      .isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV remove referral`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val reportedAdjudication = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication

    assertThat(reportedAdjudication.outcomes.size).isEqualTo(1)
    assertThat(reportedAdjudication.outcomes[0].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(reportedAdjudication.outcomes[0].outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.REFER_GOV)

    webTestClient.delete()
      .uri("/reported-adjudications/${reportedAdjudication.chargeNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.REFER_INAD.name)
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV SCHEDULE_HEARING remove hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication.chargeNumber

    integrationTestData().createHearing(testDataSet = testData, oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(3))

    webTestClient.delete()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
  }

  @Test
  fun `create refer to gov from hearing`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val chargeNumber = initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT).getGeneratedChargeNumber()

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
          "referGovReason" to ReferGovReason.GOV_INQUIRY,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.referGovReason").isEqualTo(ReferGovReason.GOV_INQUIRY.name)
  }
}
