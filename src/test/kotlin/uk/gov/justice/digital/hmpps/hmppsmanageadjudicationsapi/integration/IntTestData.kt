package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.BankHolidayApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import java.time.LocalDateTime

class IntTestData(
  private val webTestClient: WebTestClient,
  private val jwtAuthHelper: JwtAuthHelper,
  private val bankHolidayApiMockServer: BankHolidayApiMockServer,
  private val prisonApiMockServer: PrisonApiMockServer
) {

  companion object {
    const val DEFAULT_ADJUDICATION_NUMBER = 1524242L
    const val DEFAULT_PRISONER_NUMBER = "AA1234A"
    const val DEFAULT_PRISONER_BOOKING_ID = 123L
    const val DEFAULT_AGENCY_ID = "MDI"
    val DEFAULT_DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 11, 12, 10, 0)

    const val UPDATED_DATE_TIME_OF_INCIDENT_TEXT = "2010-11-13T10:00:00"
    const val UPDATED_HANDOVER_DEADLINE_TEXT = "2010-11-16T10:00:00"
    const val UPDATED_LOCATION_ID = 721899L
    const val UPDATED_STATEMENT = "updated test statement"
    val UPDATED_DATE_TIME_OF_INCIDENT = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1)

    val ADJUDICATION_1 = AdjudicationIntTestDataSet(
      adjudicationNumber = 456L,
      prisonerNumber = "BB2345B",
      agencyId = "LEI",
      locationId = 11L,
      dateTimeOfIncidentISOString = "2020-12-13T08:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-13T08:00:00"),
      statement = "Test statement"
    )

    val ADJUDICATION_2 = AdjudicationIntTestDataSet(
      adjudicationNumber = 567L,
      prisonerNumber = "CC2345C",
      agencyId = "MDI",
      locationId = 12L,
      dateTimeOfIncidentISOString = "2020-12-14T09:00:00",
      dateTimeOfIncident = LocalDateTime.parse("2020-12-14T09:00:00"),
      statement = "Different test statement"
    )
  }

  fun startNewAdjudication(
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    bankHolidayApiMockServer.stubGetBankHolidays()

    return webTestClient.post()
      .uri("/draft-adjudications")
      .headers(headers)
      .bodyValue(
        mapOf(
          "prisonerNumber" to testDataSet.prisonerNumber,
          "agencyId" to testDataSet.agencyId,
          "locationId" to testDataSet.locationId,
          "dateTimeOfIncident" to testDataSet.dateTimeOfIncident
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun addIncidentStatement(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): DraftAdjudicationResponse {
    return webTestClient.post()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/incident-statement")
      .headers(headers)
      .bodyValue(
        mapOf(
          "statement" to testDataSet.statement
        )
      )
      .exchange()
      .expectStatus().is2xxSuccessful
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
  }

  fun completeDraftAdjudication(
    draftCreationData: DraftAdjudicationResponse,
    testDataSet: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit = setHeaders()
  ): WebTestClient.ResponseSpec {
    prisonApiMockServer.stubPostAdjudication(testDataSet)

    return webTestClient.post()
      .uri("/draft-adjudications/${draftCreationData.draftAdjudication.id}/complete-draft-adjudication")
      .headers(headers)
      .exchange()
  }

  fun setHeaders(contentType: MediaType = MediaType.APPLICATION_JSON, username: String? = "ITAG_USER", roles: List<String> = emptyList()): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles, scope = listOf("write")))
    it.contentType = contentType
  }
}
