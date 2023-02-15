package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

class ReferralsIntTest : OutcomeIntTest() {

  @Test
  fun `remove referral with hearing`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createHearingOutcome(HearingOutcomeCode.REFER_POLICE)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").doesNotExist()
  }

  @Test
  fun `remove referral with hearing and referral outcome`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForHearings().createHearing().createHearingOutcome(HearingOutcomeCode.REFER_POLICE)
      .createOutcome(OutcomeCode.PROSECUTION).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").exists()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome").doesNotExist()
  }

  @Test
  fun `remove referral without hearing`() {
    initDataForOutcome().createOutcome()

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `remove referral and referral outcome`() {
    initDataForOutcome().createOutcome()

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, OutcomeCode.PROSECUTION
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
  }

  @Test
  fun `remove referral, referral outcome and hearing outcome for a POLICE_REFER related to complex example, police refer - no hearing, inad refer, police refer, prosecute`() {
    initDataForOutcome().createOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_INAD
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing!!.plusDays(1), OicHearingType.INAD_ADULT)

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_POLICE
    )

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, OutcomeCode.PROSECUTION
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(2)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[1].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].hearing").doesNotExist()
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.history[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.history[1].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.history[2].hearing.oicHearingType").isEqualTo(OicHearingType.INAD_ADULT.name)
      .jsonPath("$.reportedAdjudication.history[2].outcome").doesNotExist()
  }

  @Test
  fun `create hearing, refer to inad, and inad decides not to proceed, then remove referral`() {
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_INAD
    )

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, OutcomeCode.NOT_PROCEED, NotProceedReason.NOT_FAIR
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
  }

  @Test
  fun `create hearing, refer to inad, schedule inad hearing, then remove the hearing`() {
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now())

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_INAD
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.history[1].outcome").doesNotExist()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_INAD.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `create hearing, refer to police, schedule hearing, then remove the hearing`() {
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now())

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_POLICE
    )

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now().plusDays(1))
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.history[1].outcome").doesNotExist()

    prisonApiMockServer.stubDeleteHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber, 100)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome").doesNotExist()
  }

  @Test
  fun `police refer from hearing leads to prosecution, then referral is removed`() {
    initDataForOutcome()

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION, LocalDateTime.now())

    integrationTestData().createHearingOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, HearingOutcomeCode.REFER_POLICE
    )

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, OutcomeCode.PROSECUTION,
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/remove-referral")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
  }
}
