package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
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
    System.out.println("${inRequest.url} BODY: ${inResponse.bodyAsString}")
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

  fun stubGetPrisonerInfoByNoms(prisonerNumber: String, bookingId: Long) {
    stubFor(
      get("/api/bookings/offenderNo/$prisonerNumber").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          // language=json
          .withBody(
            """
              {
                "agencyId": "MDI",
                "assignedLivingUnitId": "1-1-002",
                "bookingId": $bookingId,
                "bookingNo": "A12121",
                "firstName": "JOHN",
                "lastName": "SMITH",
                "offenderNo": "$prisonerNumber"
              }
            """,
          ),
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

  fun stubCreateHearing(adjudicationNumber: String) {
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

  fun stubCreateHearingFailure(adjudicationNumber: String) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubAmendHearing(adjudicationNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubAmendHearingFailure(adjudicationNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubDeleteHearing(adjudicationNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingFailure(adjudicationNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubCreateHearingResult(adjudicationNumber: String) {
    stubFor(
      post(
        urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201),
        ),
    )
  }

  fun stubAmendHearingResult(adjudicationNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingResult(adjudicationNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubCreateSanctions(adjudicationNumber: String) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubCreateSanction(adjudicationNumber: String) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanction"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              JSONObject()
                .put("sanctionSeq", "1")
                .toString(),
            ),
        ),
    )
  }

  fun stubUpdateSanctions(adjudicationNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubQuashSanctions(adjudicationNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanctions/quash"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteSanctions(adjudicationNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteSanction(adjudicationNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$adjudicationNumber/sanction/1"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }
}
