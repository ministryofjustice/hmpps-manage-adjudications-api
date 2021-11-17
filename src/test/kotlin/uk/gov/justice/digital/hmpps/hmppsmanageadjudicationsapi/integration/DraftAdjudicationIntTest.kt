package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(1)
  }

  @Test
  fun `get draft adjudication details`() {
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
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(2)
  }

  @Test
  fun `add the incident statement to the draft adjudication`() {
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
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(2)
      .jsonPath("$.draftAdjudication.prisonerNumber").isEqualTo("A12345")
      .jsonPath("$.draftAdjudication.incidentStatement.statement").isEqualTo("test")
      .jsonPath("$.draftAdjudication.incidentStatement.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByDateTime").exists()
  }

  @Test
  fun `edit the incident details`() {
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
      .jsonPath("$.draftAdjudication.incidentDetails.locationId").isEqualTo(3)
      .jsonPath("$.draftAdjudication.incidentDetails.modifiedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentDetails.modifiedByDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentDetails.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentDetails.createdDateTime").exists()
  }

  @Test
  fun `edit the incident statement`() {
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
      .jsonPath("$.draftAdjudication.incidentStatement.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByDateTime").exists()
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
      .jsonPath("$.incidentDetails.locationId").isEqualTo(721850)

    val expectedBody = mapOf(
      "offenderNo" to "A12345",
      "incidentLocationId" to 2L,
      "incidentTime" to "2010-10-12T10:00:00",
      "statement" to "test statement"
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))

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
      .jsonPath("$.draftAdjudications[0].incidentDetails.locationId").isEqualTo(2)
      .jsonPath("$.draftAdjudications[0].incidentDetails.modifiedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudications[0].incidentDetails.modifiedByDateTime").exists()
      .jsonPath("$.draftAdjudications[0].incidentDetails.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudications[0].incidentDetails.createdDateTime").exists()
  }

  @Test
  fun `make the incident statement has being complete`() {
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
      .jsonPath("$.draftAdjudication.incidentStatement.createdByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.createdDateTime").exists()
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByUserId").isEqualTo("ITAG_USER")
      .jsonPath("$.draftAdjudication.incidentStatement.modifiedByDateTime").exists()
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
