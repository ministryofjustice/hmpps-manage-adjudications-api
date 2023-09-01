package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.delete
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.stubbing.Scenario
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

  fun stubPostAdjudicationCreationRequestData(testDataSet: AdjudicationIntTestDataSet) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/request-creation-data"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": ${testDataSet.chargeNumber}
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
                  "adjudicationNumber": ${testDataSet.chargeNumber},
                  "reporterStaffId": 486080,
                  "offenderNo": "${testDataSet.prisonerNumber}",
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

  fun stubCreateHearing(chargeNumber: String, currentState: String = Scenario.STARTED, nextState: String = Scenario.STARTED, oicHearingId: Long = 100) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing"))
        .inScenario("oic ids")
        .whenScenarioStateIs(currentState)
        .willSetStateTo(nextState)
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201)
            .withBody(
              JSONObject()
                .put("oicHearingId", "$oicHearingId")
                .put("dateTimeOfHearing", "2022-10-24T10:10:10")
                .put("hearingLocationId", "100")
                .toString(),
            ),
        ),
    )
  }

  fun stubCreateHearingFailure(chargeNumber: String) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubAmendHearing(chargeNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubAmendHearingFailure(chargeNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubDeleteHearing(chargeNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingFailure(chargeNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(500),
        ),
    )
  }

  fun stubCreateHearingResult(chargeNumber: String) {
    stubFor(
      post(
        urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100/result"),
      )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(201),
        ),
    )
  }

  fun stubAmendHearingResult(chargeNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteHearingResult(chargeNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/100/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubCreateSanctions(chargeNumber: String) {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubUpdateSanctions(chargeNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubQuashSanctions(chargeNumber: String) {
    stubFor(
      put(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/sanctions/quash"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubDeleteSanctions(chargeNumber: String) {
    stubFor(
      delete(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/sanctions"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200),
        ),
    )
  }

  fun stubNomisHearingResult(chargeNumber: String, oicHearingId: Long) {
    stubFor(
      get(urlEqualTo("/api/adjudications/adjudication/$chargeNumber/hearing/$oicHearingId/result"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
               [{
                  "pleaFindingCode": "",
                  "findingCode": ""
               }]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}
