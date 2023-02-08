package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class OutcomeIntTest : IntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create outcome`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "code" to OutcomeCode.NOT_PROCEED.name,
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR.name,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcome.id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `remove referral with hearing`() {
    TODO("implement me")
  }

  @Test
  fun `remove referral without hearing`() {
    TODO("implement me")
  }

  private fun initDataForOutcome(): IntegrationTestScenario {
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
