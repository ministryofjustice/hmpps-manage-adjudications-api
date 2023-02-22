package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

class OutcomeIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }
  @Test
  fun `create outcome`() {
    initDataForOutcome()

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "code" to OutcomeCode.NOT_PROCEED,
          "details" to "details",
          "reason" to NotProceedReason.NOT_FAIR,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.NOT_PROCEED.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome..id").isNotEmpty
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.reason").isEqualTo(NotProceedReason.NOT_FAIR.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `delete an outcome - not proceed `() {
    initDataForOutcome().createOutcome(code = OutcomeCode.NOT_PROCEED)

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/outcome")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()")
      .isEqualTo(0)
      .jsonPath("$.reportedAdjudication.outcomes.size()")
      .isEqualTo(0)
  }

  @Test
  fun `refer to police leads to police prosecution`() {
    initDataForOutcome().createOutcome()

    integrationTestData().createOutcome(
      IntegrationTestData.DEFAULT_ADJUDICATION, OutcomeCode.PROSECUTION
    ).expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.history.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome").exists()
      .jsonPath("$.reportedAdjudication.history[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.history[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  protected fun initDataForOutcome(): IntegrationTestScenario {
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
