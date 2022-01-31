package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_INCIDENT_ROLE_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_PRISONER_BOOKING_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_HANDOVER_DEADLINE_ISO_STRING
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_INCIDENT_ROLE_CODE
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DraftAdjudicationIntTest : IntegrationTestBase() {
  fun dataAPiHelpers(): DataAPiHelpers = DataAPiHelpers(webTestClient, setHeaders())

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
          "incidentRole" to IncidentRoleDto("25a", "B2345BB"),
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
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo("B2345BB")
  }

  @Test
  fun `get draft adjudication details`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

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
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
  }

  @Test
  fun `get previously submitted draft adjudication details`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftCreationResponse = intTestData.startNewAdjudication(testAdjudication)
    intTestData.setOffenceDetails(draftCreationResponse, testAdjudication)
    intTestData.addIncidentStatement(draftCreationResponse, testAdjudication)
    intTestData.completeDraftAdjudication(
      draftCreationResponse,
      testAdjudication,
      setHeaders(username = testAdjudication.createdByUserId)
    )

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
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.dateTimeOfIncident.plusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").isEqualTo(testAdjudication.offences[0].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber").isEqualTo(testAdjudication.offences[0].victimPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceCode").isEqualTo(testAdjudication.offences[1].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[2]").doesNotExist()
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo(testAdjudication.statement)
  }

  @Test
  fun `add offence details to the draft adjudication`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

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
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").isEqualTo(testAdjudication.offences[0].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber").isEqualTo(testAdjudication.offences[0].victimPrisonersNumber!!)
      .jsonPath("$.draftAdjudication.offenceDetails[1].offenceCode").isEqualTo(testAdjudication.offences[1].offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[1].victimPrisonersNumber").doesNotExist()
  }

  @Test
  fun `add the incident statement to the draft adjudication`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

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
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test")
  }

  @Test
  fun `edit the incident details`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT.plusMonths(1),
          "incidentRole" to IncidentRoleDto("25b", "C3456CC"),
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
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo("C3456CC")
  }

  @Test
  fun `edit offence details for the draft adjudication`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    intTestData.setOffenceDetails(draftAdjudicationResponse, testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/offence-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "offenceDetails" to listOf(IntTestData.BASIC_OFFENCE),
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudication.incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudication.incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
      .jsonPath("$.draftAdjudication.offenceDetails[0].offenceCode").isEqualTo(IntTestData.BASIC_OFFENCE.offenceCode)
      .jsonPath("$.draftAdjudication.offenceDetails[0].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.draftAdjudication.offenceDetails[1]").doesNotExist()
  }

  @Test
  fun `edit the incident statement`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    intTestData.addIncidentStatement(draftAdjudicationResponse, testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-statement")
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
    prisonApiMockServer.stubPostAdjudication()
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val draftAdjudicationResponse = intTestData.startNewAdjudication(IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.setOffenceDetails(draftAdjudicationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.addIncidentStatement(draftAdjudicationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.prisonerNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.bookingId").isEqualTo(1) // From PrisonAPIMockServer.stubPostAdjudication
      .jsonPath("$.dateTimeReportExpires").isEqualTo(IntTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.dateTimeOfIncident").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.incidentDetails.handoverDeadline").isEqualTo(IntTestData.DEFAULT_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.locationId").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.locationId)
      .jsonPath("$.incidentRole.roleCode").isEqualTo(DEFAULT_INCIDENT_ROLE_CODE)
      .jsonPath("$.incidentRole.associatedPrisonersNumber").isEqualTo(DEFAULT_INCIDENT_ROLE_ASSOCIATED_PRISONER)
      .jsonPath("$.offences[0].offenceCode").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.offences[0].victimPrisonersNumber").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.offences[0].victimPrisonersNumber)
      .jsonPath("$.offences[1].offenceCode").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.offences[1].offenceCode)
      .jsonPath("$.offences[1].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.incidentStatement.statement").isEqualTo(IntTestData.DEFAULT_ADJUDICATION.statement)

    val expectedBody = mapOf(
      "offenderNo" to IntTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      "agencyId" to IntTestData.DEFAULT_ADJUDICATION.agencyId,
      "incidentLocationId" to IntTestData.DEFAULT_ADJUDICATION.locationId,
      "incidentTime" to IntTestData.DEFAULT_ADJUDICATION.dateTimeOfIncidentISOString,
      "statement" to IntTestData.DEFAULT_ADJUDICATION.statement
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isNotFound
  }

  @Test
  fun `complete draft update of existing adjudication`() {
    prisonApiMockServer.stubPutAdjudication()
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val firstDraftUserHeaders = setHeaders(username = IntTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val firstDraftCreationResponse = intTestData.startNewAdjudication(IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.setOffenceDetails(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)
    intTestData.addIncidentStatement(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION, firstDraftUserHeaders)

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(0)

    intTestData.completeDraftAdjudication(firstDraftCreationResponse, IntTestData.DEFAULT_ADJUDICATION)
    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)

    val draftAdjudicationResponse = intTestData.recallCompletedDraftAdjudication(IntTestData.DEFAULT_ADJUDICATION)
    intTestData.editIncidentDetails(draftAdjudicationResponse, IntTestData.UPDATED_ADJUDICATION)
    intTestData.setOffenceDetails(draftAdjudicationResponse, IntTestData.UPDATED_ADJUDICATION)
    intTestData.editIncidentStatement(draftAdjudicationResponse, IntTestData.UPDATED_ADJUDICATION)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(IntTestData.UPDATED_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.prisonerNumber").isEqualTo(IntTestData.UPDATED_ADJUDICATION.prisonerNumber)
      .jsonPath("$.bookingId").isEqualTo(DEFAULT_PRISONER_BOOKING_ID) // From PrisonAPIMockServer.stubPutAdjudication()
      .jsonPath("$.dateTimeReportExpires").isEqualTo(UPDATED_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.dateTimeOfIncident").isEqualTo(IntTestData.UPDATED_ADJUDICATION.dateTimeOfIncidentISOString)
      .jsonPath("$.incidentDetails.handoverDeadline").isEqualTo(UPDATED_HANDOVER_DEADLINE_ISO_STRING)
      .jsonPath("$.incidentDetails.locationId").isEqualTo(IntTestData.UPDATED_ADJUDICATION.locationId)
      .jsonPath("$.incidentRole.roleCode").isEqualTo(UPDATED_INCIDENT_ROLE_CODE)
      .jsonPath("$.incidentRole.associatedPrisonersNumber").isEqualTo(UPDATED_INCIDENT_ROLE_ASSOCIATED_PRISONER)
      .jsonPath("$.offences[0].offenceCode").isEqualTo(IntTestData.UPDATED_ADJUDICATION.offences[0].offenceCode)
      .jsonPath("$.offences[0].victimPrisonersNumber").doesNotExist()
      .jsonPath("$.offences[1]").doesNotExist()
      .jsonPath("$.incidentStatement.statement").isEqualTo(IntTestData.UPDATED_ADJUDICATION.statement)

    val expectedBody = mapOf(
      "incidentLocationId" to IntTestData.UPDATED_ADJUDICATION.locationId,
      "incidentTime" to IntTestData.UPDATED_ADJUDICATION.dateTimeOfIncidentISOString,
      "statement" to IntTestData.UPDATED_ADJUDICATION.statement,
    )

    prisonApiMockServer.verifyPutAdjudication(objectMapper.writeValueAsString(expectedBody))

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isNotFound

    assertThat(reportedAdjudicationRepository.findAll()).hasSize(1)
  }

  @Test
  fun `should not delete the draft adjudication when the adjudication report submission fails`() {
    prisonApiMockServer.stubPostAdjudicationFailure()

    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    intTestData.setOffenceDetails(draftAdjudicationResponse, testAdjudication)
    intTestData.addIncidentStatement(draftAdjudicationResponse, testAdjudication)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is5xxServerError

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isOk
  }

  @Test
  fun `returns all in progress draft adjudications created by the current user in the given caseload`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/my/agency/${IntTestData.ADJUDICATION_1.agencyId}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudications[0].id").isEqualTo(draftAdjudicationResponse.draftAdjudication.id)
      .jsonPath("$.draftAdjudications[0].prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudications[0].incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudications[0].incidentDetails.handoverDeadline").isEqualTo(testAdjudication.handoverDeadlineISOString)
      .jsonPath("$.draftAdjudications[0].incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
      .jsonPath("$.draftAdjudications[0].incidentRole.roleCode").isEqualTo(testAdjudication.incidentRoleCode)
      .jsonPath("$.draftAdjudications[0].incidentRole.associatedPrisonersNumber").isEqualTo(testAdjudication.incidentRoleAssociatedPrisonersNumber)
  }

  @Test
  fun `mark the incident statement as being complete`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.startNewAdjudication(testAdjudication)
    intTestData.addIncidentStatement(draftAdjudicationResponse, testAdjudication)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-statement")
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

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
