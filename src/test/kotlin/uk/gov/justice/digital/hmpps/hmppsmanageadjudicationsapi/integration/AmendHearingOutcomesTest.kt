package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class AmendHearingOutcomesTest : IntegrationTestBase() {

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
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.adjudicator")
      .isEqualTo("updated adjudicator")
  }
}
