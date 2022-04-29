package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.AdjudicationIntTestDataSet
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_ADJUDICATION_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_AGENCY_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_CREATED_USER_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_PRISONER_BOOKING_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_DATE_TIME_OF_INCIDENT_TEXT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_LOCATION_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_STATEMENT

class PrisonApiMockServer : WireMockServer {
  constructor() : super(8979) {
    /* Add logging of request and any matched response. */
    addMockServiceRequestListener(::requestReceived)
  }

  private fun requestReceived(
    inRequest: Request,
    inResponse: Response
  ) {
    System.out.println("BODY: ${inResponse.bodyAsString}")
  }

  fun stubHealth() {
    stubFor(
      get(urlEqualTo("/health/ping"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                 "status": "UP"
               }
              """.trimIndent()
            )
        )
    )
  }

  fun stubHealthFailure() {
    stubFor(
      get(urlEqualTo("/health/ping"))
        .willReturn(
          aResponse()
            .withFault(Fault.CONNECTION_RESET_BY_PEER)
        )
    )
  }

  fun stubPutAdjudication() {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$DEFAULT_ADJUDICATION_NUMBER"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": $DEFAULT_ADJUDICATION_NUMBER,
                  "reporterStaffId": 486080,
                  "offenderNo": "$DEFAULT_PRISONER_NUMBER",
                  "bookingId": $DEFAULT_PRISONER_BOOKING_ID,
                  "agencyId": "$DEFAULT_AGENCY_ID",
                  "incidentTime": "$UPDATED_DATE_TIME_OF_INCIDENT_TEXT",
                  "incidentLocationId": $UPDATED_LOCATION_ID,
                  "statement": "$UPDATED_STATEMENT",
                  "createdByUserId": "$DEFAULT_CREATED_USER_ID"
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPutAdjudicationFailure() {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$DEFAULT_ADJUDICATION_NUMBER"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500)
        )
    )
  }

  fun verifyPutAdjudication(bodyAsJson: String) {
    verify(
      putRequestedFor(urlEqualTo("/api/adjudications/adjudication/$DEFAULT_ADJUDICATION_NUMBER"))
        .withRequestBody(equalTo(bodyAsJson))
    )
  }

  fun stubPostAdjudication(testDataSet: AdjudicationIntTestDataSet) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication"))
        .withRequestBody(matchingJsonPath("$.[?(@.offenderNo == '${testDataSet.prisonerNumber}')]"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": ${testDataSet.adjudicationNumber},
                  "reporterStaffId": 486080,
                  "offenderNo": "${testDataSet.prisonerNumber}",
                  "bookingId": ${testDataSet.bookingId},
                  "agencyId": "${testDataSet.agencyId}",
                  "incidentTime": "${testDataSet.dateTimeOfIncidentISOString}",
                  "incidentLocationId": ${testDataSet.locationId},
                  "statement": "${testDataSet.statement}",
                  "createdByUserId": "${testDataSet.createdByUserId}"
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPostAdjudicationFailure() {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500)
        )
    )
  }

  fun verifyPostAdjudication(bodyAsJson: String) {
    verify(
      postRequestedFor(urlEqualTo("/api/adjudications/adjudication"))
        .withRequestBody(equalTo(bodyAsJson))
    )
  }
}
