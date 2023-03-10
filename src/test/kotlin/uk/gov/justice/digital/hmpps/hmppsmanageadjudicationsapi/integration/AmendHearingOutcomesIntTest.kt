package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class AmendHearingOutcomesIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
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
          "details" to "updated details"
        )
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
          "details" to "updated details"
        )
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
        )
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
          "caution" to false,
        )
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
          "details" to "updated details"
        )
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
          "details" to "updated details"
        )
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
}
