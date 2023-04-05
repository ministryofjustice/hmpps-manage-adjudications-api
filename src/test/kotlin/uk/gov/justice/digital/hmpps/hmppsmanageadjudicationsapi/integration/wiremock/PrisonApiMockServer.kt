package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aMultipart
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import org.json.JSONObject
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.AdjudicationIntTestDataSet

class PrisonApiMockServer : WireMockServer {
  constructor() : super(8979) {
    /* Add logging of request and any matched response. */
    addMockServiceRequestListener(::requestReceived)
  }

  private fun requestReceived(
    inRequest: Request,
    inResponse: Response,
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
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubHealthFailure() {
    stubFor(
      get(urlEqualTo("/health/ping"))
        .willReturn(
          aResponse()
            .withFault(Fault.CONNECTION_RESET_BY_PEER),
        ),
    )
  }

  fun stubPostAdjudicationCreationRequestData(testDataSet: AdjudicationIntTestDataSet) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/request-creation-data"))
        .withRequestBody(matching(testDataSet.prisonerNumber))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": ${testDataSet.adjudicationNumber},
                  "bookingId": ${testDataSet.bookingId}
                }
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubPostAdjudicationCreationRequestDataFailure() {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/request-creation-data"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun verifyPostAdjudicationCreationRequestData(bodyAsJson: String) {
    verify(
      postRequestedFor(urlEqualTo("/api/adjudications/adjudication/request-creation-data"))
        .withRequestBody(equalTo(bodyAsJson)),
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
              """.trimIndent(),
            ),
        ),
    )
  }

  fun stubPostAdjudicationFailure() {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun verifyPostAdjudication(bodyAsJson: String) {
    verify(
      postRequestedFor(urlEqualTo("/api/adjudications/adjudication"))
        .withRequestBody(equalTo(bodyAsJson)),
    )
  }

  fun stubCreateHearing(adjudicationNumber: Long) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              JSONObject()
                .put("oicHearingId", "100")
                .put("dateTimeOfHearing", "2022-10-24T10:10:10")
                .put("hearingLocationId", "100")
                .toString(),
            ),
        ),
    )
  }

  fun stubCreateHearingFailure(adjudicationNumber: Long) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubAmendHearing(adjudicationNumber: Long) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubAmendHearingFailure(adjudicationNumber: Long) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubDeleteHearing(adjudicationNumber: Long) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingFailure(adjudicationNumber: Long) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubCreateHearingResult(adjudicationNumber: Long, body: JSONObject) {
    stubFor(
      post(
        urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"),
      ).withMultipartRequestBody(
        aMultipart().withBody(equalToJson(body.toString())),
      ).willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withStatus(201)
          .withBody(
            JSONObject()
              .put("pleaFindingCode", "")
              .put("findingCode", "")
              .toString(),
          ),
      ),
    )
  }

  fun stubAmendHearingResult(adjudicationNumber: Long) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingResult(adjudicationNumber: Long) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }
}
