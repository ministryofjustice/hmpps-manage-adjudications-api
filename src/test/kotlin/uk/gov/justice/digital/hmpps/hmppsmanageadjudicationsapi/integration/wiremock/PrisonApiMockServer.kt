package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.verify

class PrisonApiMockServer : WireMockServer(8979) {
  fun stubHealth() {
    stubFor(
      get(urlEqualTo("/api/health/ping"))
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

  fun stubGetAdjudication() {
    stubFor(
      get(urlEqualTo("/api/adjudications/adjudication/1524242"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": 1524242,
                  "reporterStaffId": 486080,
                  "offenderNo": "AA1234A",
                  "bookingId": 123,
                  "incidentTime": "2021-10-25T09:03:11",
                  "incidentLocationId": 721850,
                  "statement": "It keeps happening..."
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAllAdjudications() {
    stubFor(
      post(urlEqualTo("/api/adjudications"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              [
               {
                  "adjudicationNumber": 1,
                  "reporterStaffId": 486080,
                  "offenderNo": "AA1234A",
                  "bookingId": 123,
                  "incidentTime": "2021-10-25T09:03:11",
                  "incidentLocationId": 721850,
                  "statement": "It keeps happening..."
                },
                 {
                  "adjudicationNumber": 2,
                  "reporterStaffId": 486080,
                  "offenderNo": "AA1234B",
                  "bookingId": 456,
                  "incidentTime": "2021-10-25T09:03:11",
                  "incidentLocationId": 721850,
                  "statement": "It keeps happening..."
                }
              ]
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetPagedAdjudications() {
    stubFor(
      post(urlEqualTo("/api/adjudications"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                 "pageNumber":1,
                 "pageSize":20,
                 "totalResults":2,
                 "results":[
                    {
                       "adjudicationNumber":1,
                       "reporterStaffId":486080,
                       "offenderNo":"AA1234A",
                       "bookingId":123,
                       "incidentTime":"2021-10-25T09:03:11",
                       "incidentLocationId":721850,
                       "statement":"It keeps happening..."
                    },
                    {
                       "adjudicationNumber":2,
                       "reporterStaffId":486080,
                       "offenderNo":"AA1234B",
                       "bookingId":456,
                       "incidentTime":"2021-10-25T09:03:11",
                       "incidentLocationId":721850,
                       "statement":"It keeps happening..."
                    }
                 ]
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAdjudicationWithInvalidNumber() {
    stubFor(
      get(urlEqualTo("/api/adjudications/adjudication/1524242"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(404)
            .withBody(
              """
                {
                  "timestamp": "2021-11-03T11:28:44.225+00:00",
                  "status": 404,
                  "error": "Not Found",
                  "path": "/reported-adjudications/1524242"
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubPostAdjudication() {
    stubFor(
      post(urlEqualTo("/api/adjudications/adjudication"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
               {
                  "adjudicationNumber": 1524242,
                  "reporterStaffId": 486080,
                  "offenderNo": "A12345",
                  "bookingId": 1,
                  "incidentTime": "2010-11-12T10:00:00",
                  "incidentLocationId": 721850,
                  "statement": "new statement"
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
