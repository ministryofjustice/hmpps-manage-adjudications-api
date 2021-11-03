package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse
import java.time.LocalDateTime

class DraftAdjudicationIntTest : IntegrationTestBase() {
  @Test
  fun `makes a request to start a new draft adjudication`() {
    webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
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
    val draftAdjudicationResponse = startNewAdjudication()

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
    val draftAdjudicationResponse = startNewAdjudication()

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
    val draftAdjudicationResponse = startNewAdjudication()

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
    val draftAdjudicationResponse = startNewAdjudication()
    addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

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

    val draftAdjudicationResponse = startNewAdjudication()
    addIncidentStatement(draftAdjudicationResponse.draftAdjudication.id, "test statement")

    webTestClient.post()
      .uri("/draft-adjudications/${draftAdjudicationResponse.draftAdjudication.id}/complete-draft-adjudication")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isCreated

    val expectedBody = mapOf(
      "prisonerNumber" to "A12345",
      "incidentLocationId" to 2L,
      "incidentTime" to "2010-10-12T10:00:00",
      "statement" to "test statement"
    )

    prisonApiMockServer.verifyPostAdjudication(objectMapper.writeValueAsString(expectedBody))
  }

  private fun startNewAdjudication(): DraftAdjudicationResponse {
    return webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
          "locationId" to 2,
          "dateTimeOfIncident" to DATE_TIME_OF_INCIDENT
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  private fun addIncidentStatement(id: Long, statement: String): DraftAdjudicationResponse {
    return webTestClient.post()
      .uri("/draft-adjudications/$id/incident-statement")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "statement" to statement
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
  }
}
