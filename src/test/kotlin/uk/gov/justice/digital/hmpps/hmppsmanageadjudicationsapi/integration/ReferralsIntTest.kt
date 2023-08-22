package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

class ReferralsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @CsvSource("REFER_POLICE", "REFER_GOV", "REFER_INAD")
  @ParameterizedTest
  fun `remove referral with hearing`(hearingOutcomeCode: HearingOutcomeCode) {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = if (hearingOutcomeCode == HearingOutcomeCode.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT).createReferral(hearingOutcomeCode)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = if (hearingOutcomeCode == HearingOutcomeCode.REFER_GOV) OicHearingType.INAD_ADULT else OicHearingType.GOV_ADULT).createReferral(hearingOutcomeCode)
      .createOutcomeNotProceed().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").exists()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    initDataForOutcome().createOutcomeReferPolice()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `remove referral and referral outcome`() {
    initDataForOutcome().createOutcomeReferPolice()

    integrationTestData().createOutcomeProsecution(
      IntegrationTestData.DEFAULT_ADJUDICATION,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    initDataForOutcome().createOutcomeReferPolice()
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)
      .expectStatus().isCreated

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
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
    initDataForOutcome().createOutcomeReferPolice()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(oicHearingType = OicHearingType.GOV_ADULT, testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing!!.plusDays(1), OicHearingType.INAD_ADULT)

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createOutcomeProsecution(
      IntegrationTestData.DEFAULT_ADJUDICATION,
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

    prisonApiMockServer.stubDeleteHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createOutcomeNotProceed(
      IntegrationTestData.DEFAULT_ADJUDICATION,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)

    prisonApiMockServer.stubDeleteHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(oicHearingType = OicHearingType.GOV_ADULT, testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, dateTimeOfHearing = LocalDateTime.now())

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_INAD,
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
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
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now())

    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
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
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now())

    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    integrationTestData().createReferral(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      HearingOutcomeCode.REFER_POLICE,
    )

    integrationTestData().createOutcomeProsecution(
      IntegrationTestData.DEFAULT_ADJUDICATION,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)

    prisonApiMockServer.stubDeleteHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
  }

  @Test
  fun `REFER_GOV referral outcome of NOT_PROCEED`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createReferral(HearingOutcomeCode.REFER_GOV)
      .createOutcomeNotProceed().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `REFER_GOV referral outcome of SCHEDULE_HEARING`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createReferral(HearingOutcomeCode.REFER_GOV)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps NOT_PROCEED`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    integrationTestData().createOutcomeNotProceed(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `REFER_INAD referral outcome REFER_GOV next steps SCHEDULE_HEARING`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    integrationTestData().createHearing(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()

    integrationTestData().createHearing(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
      .expectStatus().isCreated
      .expectBody()

    val response = integrationTestData().createChargeProved(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION).reportedAdjudication

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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubQuashSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()

    integrationTestData().createHearing(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT)
      .expectStatus().isCreated
      .expectBody()

    integrationTestData().createChargeProved(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION)
    val response = integrationTestData().createQuashed(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION).reportedAdjudication

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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD)

    integrationTestData().createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION,
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
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
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
      testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION,
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD)

    integrationTestData().createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()

    val response = integrationTestData().createChargeProved(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION).reportedAdjudication

    assertThat(response.outcomes.size).isEqualTo(3)
    assertThat(response.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
    assertThat(response.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    assertThat(response.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_GOV)
    assertThat(response.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
  }

  @Test
  fun `REFER_INAD - SCHEDULE_HEARING - REFER_GOV - SCHEDULE_HEARING - CHARGE_PROVED - QUASHED`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubQuashSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD)

    integrationTestData().createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    )
      .expectStatus().isCreated
      .expectBody()

    integrationTestData().createChargeProved(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION)
    val response = integrationTestData().createQuashed(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION).reportedAdjudication

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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT)
      .createReferral(HearingOutcomeCode.REFER_INAD)

    integrationTestData().createHearing(
      IntegrationTestData.DEFAULT_ADJUDICATION,
      LocalDateTime.now().plusDays(1),
      oicHearingType = OicHearingType.INAD_ADULT,
    )
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/outcome/referral")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "code" to HearingOutcomeCode.REFER_GOV,
          "details" to "details",
          "adjudicator" to "testing",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    integrationTestData().createHearing(
      testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION,
      dateTimeOfHearing = LocalDateTime.now().plusDays(2),
      oicHearingType = OicHearingType.GOV_ADULT,
    ).expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/remove-referral")
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
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubAmendHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing(oicHearingType = OicHearingType.GOV_ADULT).createReferral(HearingOutcomeCode.REFER_INAD)
      .createOutcomeReferGov().expectStatus().isCreated

    integrationTestData().createHearing(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(3))

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.REFER_GOV.name)
  }
}
