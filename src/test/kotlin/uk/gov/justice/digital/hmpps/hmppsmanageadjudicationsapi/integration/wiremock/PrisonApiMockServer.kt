package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.AdjudicationIntTestDataSet
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_ADJUDICATION_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_AGENCY_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_PRISONER_BOOKING_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.DEFAULT_PRISONER_NUMBER
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_DATE_TIME_OF_INCIDENT_TEXT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_LOCATION_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntTestData.Companion.UPDATED_STATEMENT

class PrisonApiMockServer : WireMockServer {
  constructor():super(8979) {
    /* Add logging of request and any matched response. */
    addMockServiceRequestListener(::requestReceived)
  }

  private fun requestReceived(inRequest: Request,
                              inResponse: Response) {
    System.out.println("BODY: ${inResponse.bodyAsString}")
  }

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
                  "agencyId": "MDI",
                  "incidentTime": "2021-10-25T09:03:11",
                  "incidentLocationId": 721850,
                  "statement": "It keeps happening..."
                }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetAdjudications() {
    stubFor(
      post(urlEqualTo("/api/adjudications/search?size=20&page=0&sort=incidentDate,DESC&sort=incidentTime,DESC"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                 "content":[
                    {
                       "adjudicationNumber":1,
                       "reporterStaffId":486080,
                       "offenderNo":"AA1234A",
                       "bookingId":123,
                       "agencyId": "MDI",
                       "incidentTime":"2021-10-25T09:03:11",
                       "incidentLocationId":721850,
                       "statement":"It keeps happening..."
                    },
                    {
                       "adjudicationNumber":2,
                       "reporterStaffId":486080,
                       "offenderNo":"AA1234B",
                       "bookingId":456,
                       "agencyId": "MDI",
                       "incidentTime":"2021-10-25T09:03:11",
                       "incidentLocationId":721850,
                       "statement":"It keeps happening..."
                    }
                 ],
                 "pageable":{
                    "sort":{
                       "empty":false,
                       "sorted":true,
                       "unsorted":false
                    },
                    "offset":0,
                    "pageSize":1,
                    "pageNumber":0,
                    "paged":true,
                    "unpaged":false
                 },
                 "last":false,
                 "totalElements":2,
                 "totalPages":2,
                 "size":1,
                 "number":0,
                 "sort":{
                    "empty":false,
                    "sorted":true,
                    "unsorted":false
                 },
                 "first":true,
                 "numberOfElements":1,
                 "empty":false
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetValidAdjudicationsById(testDataSet1: AdjudicationIntTestDataSet, testDataSet2: AdjudicationIntTestDataSet) {
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
                     "adjudicationNumber": ${testDataSet1.adjudicationNumber},
                     "reporterStaffId":486080,
                     "offenderNo": "${testDataSet1.prisonerNumber}",
                     "bookingId":123,
                     "agencyId": "${testDataSet1.agencyId}",
                     "incidentTime": "${testDataSet1.dateTimeOfIncidentISOString}",
                     "incidentLocationId": ${testDataSet1.locationId},
                     "statement": "${testDataSet1.statement}"
                  },
                  {
                     "adjudicationNumber":${testDataSet2.adjudicationNumber},
                     "reporterStaffId":486080,
                     "offenderNo": "${testDataSet2.prisonerNumber}",
                     "bookingId":456,
                     "agencyId": "${testDataSet2.agencyId}",
                     "incidentTime": "${testDataSet2.dateTimeOfIncidentISOString}",
                     "incidentLocationId": ${testDataSet2.locationId},
                     "statement": "${testDataSet2.statement}"
                  }
                ]
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
                  "agencyId": "MDI",
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
                  "statement": "$UPDATED_STATEMENT"
                }
              """.trimIndent()
            )
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
//        .withRequestBody(matchingJsonPath("$.offenderNo = '${testDataSet.prisonerNumber}'"))
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
                  "bookingId": 1,
                  "agencyId": "${testDataSet.agencyId}",
                  "incidentTime": "${testDataSet.dateTimeOfIncidentISOString}",
                  "incidentLocationId": ${testDataSet.locationId},
                  "statement": "${testDataSet.statement}"
                }
              """.trimIndent()
            )
        )
    )
  }
}
