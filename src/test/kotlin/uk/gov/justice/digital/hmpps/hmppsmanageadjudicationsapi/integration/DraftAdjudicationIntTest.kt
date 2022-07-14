package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.time.LocalDateTime

class DraftAdjudicationIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Autowired
  lateinit var draftAdjudicationRepository: DraftAdjudicationRepository

  @Autowired
  lateinit var reportedAdjudicationRepository: ReportedAdjudicationRepository

  @Test
  fun `makes a request to start a new draft adjudication`() {
    webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
          "agencyId" to "MDI",
          "locationId" to 1,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `get draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, userHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    webTestClient.get()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `get previously submitted draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, userHeaders)

    intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val draftAdjudicationResponse = intTestData.recallCompletedDraftAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.adjudicationNumber").isEqualTo(testAdjudication.adjudicationNumber)
  }

  @Test
  fun `add offence details to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, userHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/offence-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenceDetails" to testAdjudication.offences,
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `add the incident statement to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-statement")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "statement" to "test",
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `edit the incident details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT.plusMonths(1),
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
  }

  @Test
  fun `edit the incident role and delete all offences`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setOffenceData()

    // Check we have offences
    val draftId = intTestScenario.getDraftId()
    val initialDraft = draftAdjudicationRepository.findById(draftId)
    assertThat(initialDraft.get().offenceDetails).hasSize(2)

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/incident-role")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "incidentRole" to IncidentRoleRequest("25b", "C3456CC"),
          "removeExistingOffences" to true,
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber

    // Check it has been removed from the DB
    val draft = draftAdjudicationRepository.findById(draftId)
    assertThat(draft.get().offenceDetails).hasSize(0)
  }

  @Test
  fun `complete draft adjudication`() {
    prisonApiMockServer.stubPostAdjudicationCreationRequestData(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()

    webTestClient.post()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    prisonApiMockServer.verifyPostAdjudicationCreationRequestData(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
    intTestScenario.getDraftAdjudicationDetails().expectStatus().isNotFound
  }

  @Test
  fun `complete draft adjudication rolls back DB if Prison API call fails`() {
    prisonApiMockServer.stubPostAdjudicationCreationRequestDataFailure()

    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()

    webTestClient.post()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is5xxServerError

    val savedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    assertThat(savedAdjudication).isNull()
  }

  @Test
  fun `complete draft update of existing adjudication`() {
    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(0)

    intTestScenario.completeDraft()
    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)

    val draftAdjudicationResponse =
      intTestData.recallCompletedDraftAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)
    intTestData.editIncidentDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.setIncidentRole(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.setOffenceDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.editIncidentStatement(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.adjudicationNumber)

    intTestData.getDraftAdjudicationDetails(draftAdjudicationResponse).expectStatus().isNotFound

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)
  }

  @Test
  fun `should not delete the draft adjudication when the adjudication report submission fails`() {
    prisonApiMockServer.stubPostAdjudicationCreationRequestDataFailure()

    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()

    webTestClient.post()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is5xxServerError

    intTestScenario.getDraftAdjudicationDetails().expectStatus().isOk
  }

  @Test
  fun `returns all in progress draft adjudications created by the current user in the given caseload`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/my/agency/${IntegrationTestData.ADJUDICATION_1.agencyId}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudications[0].id").isEqualTo(draftAdjudicationResponse.draftAdjudication.id)
  }

  @Test
  fun `mark the incident statement as being complete`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .addIncidentStatement()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/incident-statement")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "completed" to true
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo(testAdjudication.statement)
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
  }

  @Test
  fun `gets adult offence rule details`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/3001?youthOffender=false")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo(3)
      .jsonPath("$.paragraphDescription").isEqualTo("Denies access to any part of the prison to any officer or any person (other than a prisoner) who is at the prison for the purpose of working there")
  }

  @Test
  fun `gets youth offence rule details`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/3001?youthOffender=true")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo(4)
      .jsonPath("$.paragraphDescription").isEqualTo("Denies access to any part of the young offender institution to any officer or any person (other than an inmate) who is at the young offender institution for the purpose of working there")
  }

  @Test
  fun `gets default offence rule details for an invalid offence code`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/999?youthOffender=false")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo("")
      .jsonPath("$.paragraphDescription").isEqualTo("")
  }

  @Test
  fun `set the applicable rule and delete all offences`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()

    val draftId = intTestScenario.getDraftId()
    val initialDraft = draftAdjudicationRepository.findById(draftId)
    assertThat(initialDraft.get().isYouthOffender).isEqualTo(false)

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/applicable-rules")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "isYouthOffenderRule" to true,
          "removeExistingOffences" to true,
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber

    val draft = draftAdjudicationRepository.findById(draftId)
    assertThat(draft.get().isYouthOffender).isEqualTo(true)
    // Check it has been removed from the DB
    assertThat(draft.get().offenceDetails).hasSize(0)
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
