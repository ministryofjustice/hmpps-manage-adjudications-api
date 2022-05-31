package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_INCIDENT_ROLE_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_HANDOVER_DEADLINE_ISO_STRING
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_INCIDENT_ROLE_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_INCIDENT_ROLE_PARAGRAPH_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
          "incidentRole" to IncidentRoleRequest("25a", "B2345BB"),
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-10-14T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(1)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo("25a")
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber").isEqualTo("25(a)")
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo("Attempts to commit any of the foregoing offences:")
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo("B2345BB")
  }

  @Test
  fun `get draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
  }

  @Test
  fun `get previously submitted draft adjudication details`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val userHeaders = setHeaders(username = testAdjudication.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, userHeaders)

    intTestBuilder
      .startDraft(testAdjudication)
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
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.startedByUserId").isEqualTo(testAdjudication.createdByUserId)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.dateTimeOfIncident.plusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").isEqualTo(testAdjudication.offences[0].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.offences[0].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.offences[0].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(testAdjudication.offences[0].victimPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimStaffUsername")
      .isEqualTo(testAdjudication.offences[0].victimStaffUsername)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(testAdjudication.offences[0].victimOtherPersonsName)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceCode").isEqualTo(testAdjudication.offences[1].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.offences[1].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.offences[1].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[2]").doesNotExist()
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo(testAdjudication.statement)
  }

  @Test
  fun `add offence details to the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/offence-details")
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
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").isEqualTo(testAdjudication.offences[0].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.offences[0].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.offences[0].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(testAdjudication.offences[0].victimPrisonersNumber!!)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimStaffUsername")
      .isEqualTo(testAdjudication.offences[0].victimStaffUsername!!)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(testAdjudication.offences[0].victimOtherPersonsName!!)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceCode").isEqualTo(testAdjudication.offences[1].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.offences[1].paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.offences[1].paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimOtherPersonsName").doesNotExist()
  }

  @Test
  fun `remove offence from adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()

    var draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    draftAdjudicationResponse = intTestData.setOffenceDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)

    webTestClient.delete()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/delete-offence/${draftAdjudicationResponse.draftAdjudication.offenceDetails!![0].offenceId}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.offenceDetails.length()").isEqualTo(0)
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
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test")
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
          "incidentRole" to IncidentRoleRequest("25b", "C3456CC"),
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-11-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-11-15T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(3)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo("25b")
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber").isEqualTo("25(b)")
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo("Incites another prisoner to commit any of the foregoing offences:")
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo("C3456CC")
  }

  @Test
  fun `edit the incident details and delete all offences`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setOffenceData()

    // Check we have offences
    val draftId = intTestScenario.getDraftId()
    val initialDraft = draftAdjudicationRepository.findById(draftId)
    assertThat(initialDraft.get().offenceDetails).hasSize(2)

    webTestClient.put()
      .uri("/draft-adjudications/$draftId/incident-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT.plusMonths(1),
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
  fun `edit offence details for the draft adjudication`() {
    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
      .setOffenceData()

    webTestClient.put()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/offence-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenceDetails" to listOf(IntegrationTestData.BASIC_OFFENCE),
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudication.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.BASIC_OFFENCE.offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.BASIC_OFFENCE.paragraphNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.BASIC_OFFENCE.paragraphDescription)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimStaffUsername").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1]").doesNotExist()
  }

  @Test
  fun `edit the incident statement`() {
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
          "statement" to "new statement"
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("new statement")
  }

  @Test
  fun `complete draft adjudication`() {
    prisonApiMockServer.stubPostAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)

    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setOffenceData()
      .addIncidentStatement()

    webTestClient.post()
      .uri("/draft-adjudications/${intTestScenario.getDraftId()}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.prisonerNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.bookingId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.bookingId)
      .jsonPath("$.incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.incidentDetails.handoverDeadline")
      .isEqualTo(IntegrationTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.locationId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.incidentRole.roleCode").isEqualTo(DEFAULT_INCIDENT_ROLE_CODE)
      .jsonPath("$.incidentRole.offenceRule.paragraphNumber").isEqualTo(DEFAULT_INCIDENT_ROLE_PARAGRAPH_NUMBER)
      .jsonPath("$.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(DEFAULT_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION)
      .jsonPath("$.incidentRole.associatedPrisonersNumber").isEqualTo(DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER)
      .jsonPath("$.offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphNumber)
      .jsonPath("$.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].paragraphDescription)
      .jsonPath("$.offenceDetails[0].victimPrisonersNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimPrisonersNumber)
      .jsonPath("$.offenceDetails[0].victimStaffUsername")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimStaffUsername)
      .jsonPath("$.offenceDetails[0].victimOtherPersonsName")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[0].victimOtherPersonsName)
      .jsonPath("$.offenceDetails[1].offenceCode")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].offenceCode)
      .jsonPath("$.offenceDetails[1].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphNumber)
      .jsonPath("$.offenceDetails[1].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.offences[1].paragraphDescription)
      .jsonPath("$.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.offenceDetails[1].victimStaffUsername").doesNotExist()
      .jsonPath("$.offenceDetails[1].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.incidentStatement.statement").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.statement)

    val expectedBody = mapOf(
      "offenderNo" to IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      "agencyId" to IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      "incidentLocationId" to IntegrationTestData.DEFAULT_ADJUDICATION.locationId,
      "incidentTime" to IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString,
      "statement" to IntegrationTestData.DEFAULT_ADJUDICATION.statement,
      "offenceCodes" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.nomisCodes,
      "victimStaffUsernames" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.victimStaffUsernames,
      "victimOffenderIds" to IntegrationTestData.DEFAULT_EXPECTED_NOMIS_DATA.victimPrisonersNumbers,
      "connectedOffenderIds" to listOf(DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER),
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))

    intTestScenario.getDraftAdjudicationDetails().expectStatus().isNotFound
  }

  @Test
  fun `complete draft adjudication rolls back DB if Prison API call fails`() {
    prisonApiMockServer.stubPostAdjudicationFailure()

    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
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
    prisonApiMockServer.stubPutAdjudication()
    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    val intTestScenario = intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setOffenceData()
      .addIncidentStatement()

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(0)

    intTestScenario.completeDraft()
    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)

    val draftAdjudicationResponse =
      intTestData.recallCompletedDraftAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)
    intTestData.editIncidentDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.setOffenceDetails(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)
    intTestData.editIncidentStatement(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.prisonerNumber").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.prisonerNumber)
      .jsonPath("$.bookingId").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.bookingId)
      .jsonPath("$.incidentDetails.dateTimeOfIncident")
      .isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.incidentDetails.handoverDeadline").isEqualTo(UPDATED_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.locationId").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.locationId)
      .jsonPath("$.incidentRole.roleCode").isEqualTo(UPDATED_INCIDENT_ROLE_CODE)
      .jsonPath("$.incidentRole.offenceRule.paragraphNumber").isEqualTo(UPDATED_INCIDENT_ROLE_PARAGRAPH_NUMBER)
      .jsonPath("$.incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(UPDATED_INCIDENT_ROLE_PARAGRAPH_DESCRIPTION)
      .jsonPath("$.incidentRole.associatedPrisonersNumber").isEqualTo(UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER)
      .jsonPath("$.offenceDetails[0].offenceCode")
      .isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.offenceDetails[0].offenceRule.paragraphNumber")
      .isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.offences[0].paragraphNumber)
      .jsonPath("$.offenceDetails[0].offenceRule.paragraphDescription")
      .isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.offences[0].paragraphDescription)
      .jsonPath("$.offenceDetails[0].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.offenceDetails[0].victimStaffUsername").doesNotExist()
      .jsonPath("$.offenceDetails[0].victimOtherPersonsName").doesNotExist()
      .jsonPath("$.offenceDetails[1]").doesNotExist()
      .jsonPath("$.incidentStatement.statement").isEqualTo(IntegrationTestData.UPDATED_ADJUDICATION.statement)

    val expectedBody = mapOf(
      "incidentLocationId" to IntegrationTestData.UPDATED_ADJUDICATION.locationId,
      "incidentTime" to IntegrationTestData.UPDATED_ADJUDICATION.dateTimeOfIncidentISOString,
      "statement" to IntegrationTestData.UPDATED_ADJUDICATION.statement,
      "offenceCodes" to IntegrationTestData.UPDATED_EXPECTED_NOMIS_DATA.nomisCodes,
      "victimStaffUsernames" to IntegrationTestData.UPDATED_EXPECTED_NOMIS_DATA.victimStaffUsernames,
      "victimOffenderIds" to IntegrationTestData.UPDATED_EXPECTED_NOMIS_DATA.victimPrisonersNumbers,
      "connectedOffenderIds" to listOf(UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER),
    )

    prisonApiMockServer.verifyPutAdjudication(objectMapper.writeValueAsString(expectedBody))

    intTestData.getDraftAdjudicationDetails(draftAdjudicationResponse).expectStatus().isNotFound

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)
  }

  @Test
  fun `complete draft update does not modify DB if Prison API call fails`() {
    prisonApiMockServer.stubPutAdjudicationFailure()

    val intTestData = integrationTestData()
    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this, firstDraftUserHeaders)

    intTestBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val draftAdjudicationResponse =
      intTestData.recallCompletedDraftAdjudication(IntegrationTestData.DEFAULT_ADJUDICATION)
    intTestData.editIncidentStatement(draftAdjudicationResponse, IntegrationTestData.UPDATED_ADJUDICATION)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is5xxServerError

    val savedAdjudication =
      reportedAdjudicationRepository.findByReportNumber(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    assertThat(savedAdjudication!!.statement).isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.statement)
  }

  @Test
  fun `should not delete the draft adjudication when the adjudication report submission fails`() {
    prisonApiMockServer.stubPostAdjudicationFailure()

    val testAdjudication = IntegrationTestData.ADJUDICATION_1
    val intTestData = integrationTestData()
    val intTestBuilder = IntegrationTestScenarioBuilder(intTestData, this)

    val intTestScenario = intTestBuilder
      .startDraft(testAdjudication)
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
      .jsonPath("$.draftAdjudications[0].prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudications[0].incidentDetails.dateTimeOfIncident")
      .isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudications[0].incidentDetails.handoverDeadline")
      .isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudications[0].incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudications[0].incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudications[0].incidentRole.offenceRule.paragraphNumber")
      .isEqualTo(testAdjudication.incidentRoleParagraphNumber)
      .jsonPath("$.draftAdjudications[0].incidentRole.offenceRule.paragraphDescription")
      .isEqualTo(testAdjudication.incidentRoleParagraphDescription)
      .jsonPath("$.draftAdjudications[0].incidentRole.associatedPrisonersNumber")
      .isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
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
  fun `gets offence rule details for a valid offence code`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/1005")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo(1)
      .jsonPath("$.paragraphDescription").isEqualTo("Commits any assault")
  }

  @Test
  fun `gets default offence rule details for an invalid offence code`() {
    webTestClient.get()
      .uri("/draft-adjudications/offence-rule/999")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.paragraphNumber").isEqualTo("")
      .jsonPath("$.paragraphDescription").isEqualTo("")
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
