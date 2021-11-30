package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault

class BankHolidayApiMockServer : WireMockServer(8978) {
  fun stubGetBankHolidays() {
    stubFor(
      get(urlEqualTo("/bank-holidays.json"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(200)
            .withBody(
              """
              {
                 "england-and-wales":{
                    "division":"england-and-wales",
                    "events":[
                       {
                          "title":"New Year’s Day",
                          "date":"2016-01-01",
                          "notes":"",
                          "bunting":true
                       },
                       {
                          "title":"Good Friday",
                          "date":"2016-03-25",
                          "notes":"",
                          "bunting":false
                       },
                       {
                          "title":"Easter Monday",
                          "date":"2016-03-28",
                          "notes":"",
                          "bunting":true
                       }
                    ]
                 },
                 "scotland":{
                    "division":"scotland",
                    "events":[
                       {
                          "title":"New Year’s Day",
                          "date":"2016-01-01",
                          "notes":"",
                          "bunting":true
                       }
                    ]
                 },
                 "northern-ireland":{
                    "division":"northern-ireland",
                    "events":[
                       {
                          "title":"New Year’s Day",
                          "date":"2016-01-01",
                          "notes":"",
                          "bunting":true
                       }
                    ]
                 }
              }
              """.trimIndent()
            )
        )
    )
  }

  fun stubGetBankHolidaysFailure() {
    stubFor(
      get(urlEqualTo("/bank-holidays.json"))
        .willReturn(
          aResponse()
            .withFault(Fault.MALFORMED_RESPONSE_CHUNK)
        )
    )
  }
}
