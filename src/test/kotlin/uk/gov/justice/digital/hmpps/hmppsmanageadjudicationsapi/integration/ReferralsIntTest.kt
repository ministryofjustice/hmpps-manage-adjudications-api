package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode

class ReferralsIntTest : OutcomeIntTest() {

  @Test
  fun `remove referral with hearing`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    val reportedAdjudication = initDataForHearings().createHearing()
    integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!, HearingOutcomeCode.REFER_POLICE
    )

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
    val reportedAdjudication = initDataForHearings().createHearing()
    integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!, HearingOutcomeCode.REFER_POLICE
    )
    integrationTestData().createOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber.toString(), OutcomeCode.PROSECUTION
    ).expectStatus().isCreated
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
    initDataForOutcome().createOutcome(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())

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
    initDataForOutcome().createOutcome(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString(), OutcomeCode.PROSECUTION
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

  /*
   Given a report adjudication is unscheduled
   when its referred to police without a hearing
   and the next step is Police Prosecution - No - Schedule a hearing
   and the hearing is scheduled
   and the hearing outcome is refer to inad
   and the next step is schedule a hearing
   and the hearing is scheduled
   and the hearing outcome is refer to the police
   and the next step is Police Prosecution - Yes
   and then!  the user removes referral
   i expect the last referral outcome (PROSECUTION) to be remove
   and the last referral outcome REFER POLICE to be removed
   and the hearing outcome of REFER POLICE to be removed
 */
  @Test
  fun `remove referral, referral outcome and hearing outcome for a POLICE_REFER related to complex example`() {
    initDataForOutcome().createOutcome(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString())

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString(), OutcomeCode.SCHEDULE_HEARING
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)

    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    var reportedAdjudication = integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.first().id!!, HearingOutcomeCode.REFER_INAD
    )

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString(), OutcomeCode.SCHEDULE_HEARING
    )
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[1].referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(1)

    reportedAdjudication = integrationTestData().createHearing(IntegrationTestData.DEFAULT_ADJUDICATION)

    integrationTestData().createHearingOutcome(
      reportedAdjudication.reportedAdjudication.adjudicationNumber, reportedAdjudication.reportedAdjudication.hearings.last().id!!, HearingOutcomeCode.REFER_POLICE
    )

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber.toString(), OutcomeCode.PROSECUTION
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
      .jsonPath("$.reportedAdjudication.hearings[1].outcome").doesNotExist()
  }
}
