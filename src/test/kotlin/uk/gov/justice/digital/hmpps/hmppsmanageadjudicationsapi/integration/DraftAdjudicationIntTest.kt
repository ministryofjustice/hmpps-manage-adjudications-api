package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_ADJUDICATION_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_PRISONER_BOOKING_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_DATE_TIME_OF_INCIDENT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_DATE_TIME_OF_INCIDENT_TEXT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_HANDOVER_DEADLINE_TEXT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_LOCATION_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_STATEMENT
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DraftAdjudicationIntTest : IntegrationTestBase() {
  fun dataAPiHelpers(): DataAPiHelpers = DataAPiHelpers(webTestClient, setHeaders())

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
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT
        )
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-10-14T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(1)
  }

  @Test
  fun `get draft adjudication details`() {
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

    webTestClient.get()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-10-14T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(2)
  }

  @Test
  fun `get previously submitted draft adjudication details`() {
    val testAdjudication = IntTestData.ADJUDICATION_1
    val intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    val draftAdjudicationResponse = intTestData.recallCompletedDraftAdjudication(testAdjudication)

    webTestClient.get()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.reportNumber").isEqualTo(testAdjudication.adjudicationNumber)
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo(testAdjudication.prisonerNumber)
      .jsonPath("$.draftAdjudication.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo(testAdjudication.dateTimeOfIncidentISOString)
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo(testAdjudication.dateTimeOfIncident.plusDays(2).format(DateTimeFormatter.ISO_DATE_TIME))
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(testAdjudication.locationId)
  }

  @Test
  fun `add the incident statement to the draft adjudication`() {
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

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
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-10-14T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(2)
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test")
  }

  @Test
  fun `edit the incident details`() {
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

    webTestClient.put()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/incident-details")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT.plusMonths(1)
        )
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudication.id").isNumber
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.incidentDetails.dateTimeOfIncident").isEqualTo("2010-11-12T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.handoverDeadline").isEqualTo("2010-11-15T10:00:00")
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(3)
  }

  @Test
  fun `edit the incident statement`() {
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

    dataAPiHelpers().addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

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
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("new statement")
  }

  @Test
  fun `complete draft adjudication`() {
    prisonApiMockServer.stubPostAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)
    dataAPiHelpers().addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(1524242)
      .jsonPath("$.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.bookingId").isEqualTo("1")
      .jsonPath("$.dateTimeReportExpires").isEqualTo("2010-11-15T10:00:00")
      .jsonPath("$.incidentStatement.statement").isEqualTo("new statement")
      .jsonPath("$.incidentDetails.dateTimeOfIncident").isEqualTo("2010-11-12T10:00:00")
      .jsonPath("$.incidentDetails.handoverDeadline").isEqualTo("2010-11-15T10:00:00")
      .jsonPath("$.incidentDetails.locationId").isEqualTo(721850)

    val expectedBody = mapOf(
      "offenderNo" to "A12345",
      "agencyId" to "MDI",
      "incidentLocationId" to 2L,
      "incidentTime" to "2010-10-12T10:00:00",
      "statement" to "test statement"
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isNotFound
  }

  @Test
  fun `complete draft update of existing adjudication`() {
    prisonApiMockServer.stubGetAdjudication()
    prisonApiMockServer.stubPutAdjudication()
    bankHolidayApiMockServer.stubGetBankHolidays()

    val draftAdjudicationResponse = dataAPiHelpers().createADraftFromAReportedAdjudication(DEFAULT_ADJUDICATION_NUMBER)
    dataAPiHelpers().editIncidentDetails(draftAdjudicationResponse.draftAdjudication.id, UPDATED_DATE_TIME_OF_INCIDENT, UPDATED_LOCATION_ID)
    dataAPiHelpers().editIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, UPDATED_STATEMENT)

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.adjudicationNumber").isEqualTo(DEFAULT_ADJUDICATION_NUMBER)
      .jsonPath("$.prisonerNumber").isEqualTo(DEFAULT_PRISONER_NUMBER)
      .jsonPath("$.bookingId").isEqualTo(DEFAULT_PRISONER_BOOKING_ID)
      .jsonPath("$.dateTimeReportExpires").isEqualTo(UPDATED_HANDOVER_DEADLINE_TEXT)
      .jsonPath("$.incidentStatement.statement").isEqualTo(UPDATED_STATEMENT)
      .jsonPath("$.incidentDetails.dateTimeOfIncident").isEqualTo(UPDATED_DATE_TIME_OF_INCIDENT_TEXT)
      .jsonPath("$.incidentDetails.handoverDeadline").isEqualTo(UPDATED_HANDOVER_DEADLINE_TEXT)
      .jsonPath("$.incidentDetails.locationId").isEqualTo(UPDATED_LOCATION_ID)

    val expectedBody = mapOf(
      "incidentLocationId" to UPDATED_LOCATION_ID,
      "incidentTime" to UPDATED_DATE_TIME_OF_INCIDENT_TEXT,
      "statement" to UPDATED_STATEMENT
    )

    prisonApiMockServer.verifyPutAdjudication(objectMapper.writeValueAsString(expectedBody))

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isNotFound
  }

  @Test
  fun `should not delete the draft adjudication when the adjudication report submission fails`() {
    prisonApiMockServer.stubPostAdjudicationFailure()

    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

    dataAPiHelpers().addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is5xxServerError

    dataAPiHelpers().getDraftAdjudicationDetails(draftAdjudicationResponse.draftAdjudication.id).expectStatus().isOk
  }

  @Test
  fun `returns all in progress draft adjudications created by the current user in the given caseload`() {
    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)

    webTestClient.get()
      .uri("/draft-adjudications/my/agency/MDI")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.draftAdjudications[0].id").isEqualTo(draftAdjudicationResponse.draftAdjudication.id)
      .jsonPath("$.draftAdjudications[0].prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudications[0].incidentDetails.dateTimeOfIncident").isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.draftAdjudications[0].incidentDetails.handoverDeadline").isEqualTo("2010-10-14T10:00:00")
      .jsonPath("$.draftAdjudications[0].incidentDetails.locationId").isEqualTo(2)
  }

  @Test
  fun `mark the incident statement as being complete`() {
    val draftAdjudicationResponse = dataAPiHelpers().startNewAdjudication(dateTimeOfIncident = DATE_TIME_OF_INCIDENT)
    dataAPiHelpers().addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

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
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test statement")
      .jsonPath("$.draftAdjudication.incidentStatement.completed").isEqualTo(true)
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
