package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import java.time.LocalDateTime
import java.util.function.Consumer

class DataAPiHelpers(private val webTestClient: WebTestClient, private val defaultHeaders: Consumer<HttpHeaders>) {

  fun startNewAdjudication(
    dateTimeOfIncident: LocalDateTime,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): DraftAdjudicationResponse = webTestClient.post()
    .uri("/draft-adjudications")
    .headers(headers)
    .bodyValue(
      mapOf(
        "prisonerNumber" to "A12345",
        "agencyId" to "MDI",
        "locationId" to 2,
        "dateTimeOfIncident" to dateTimeOfIncident
      )
    )
    .exchange()
    .expectStatus().is2xxSuccessful
    .returnResult(DraftAdjudicationResponse::class.java)
    .responseBody
    .blockFirst()!!

  fun editIncidentDetails(
    id: Long,
    dateTimeOfIncident: LocalDateTime,
    locationId: Long,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): DraftAdjudicationResponse =
    webTestClient.put()
      .uri("/draft-adjudications/$id/incident-details")
      .headers(headers)
      .bodyValue(
        mapOf(
          "locationId" to locationId,
          "dateTimeOfIncident" to dateTimeOfIncident
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

  fun addIncidentStatement(
    id: Long,
    statement: String,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): DraftAdjudicationResponse = webTestClient.post()
    .uri("/draft-adjudications/$id/incident-statement")
    .headers(headers)
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

  fun editIncidentStatement(
    id: Long,
    statement: String,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): DraftAdjudicationResponse = webTestClient.put()
    .uri("/draft-adjudications/$id/incident-statement")
    .headers(headers)
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

  fun getDraftAdjudicationDetails(
    id: Long,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/draft-adjudications/$id")
    .headers(headers)
    .exchange()

  fun completeDraftAdjudication(
    id: Long,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): WebTestClient.ResponseSpec = webTestClient.post()
    .uri("/draft-adjudications/$id/complete-draft-adjudication")
    .headers(headers)
    .exchange()

  fun createAndCompleteADraftAdjudication(dateTimeOfIncident: LocalDateTime): ReportedAdjudicationDto {
    val draftAdjudication = startNewAdjudication(dateTimeOfIncident)
    addIncidentStatement(draftAdjudication.draftAdjudication.id, statement = "hello")

    return completeDraftAdjudication(draftAdjudication.draftAdjudication.id)
      .expectStatus().isCreated
      .returnResult(ReportedAdjudicationDto::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun createADraftFromAReportedAdjudication(
    adjudicationNumber: Long,
    headers: Consumer<HttpHeaders>? = defaultHeaders
  ): DraftAdjudicationResponse = webTestClient.post()
    .uri("/reported-adjudications/$adjudicationNumber/create-draft-adjudication")
    .headers(headers)
    .exchange()
    .expectStatus().is2xxSuccessful
    .returnResult(DraftAdjudicationResponse::class.java)
    .responseBody
    .blockFirst()!!
}
