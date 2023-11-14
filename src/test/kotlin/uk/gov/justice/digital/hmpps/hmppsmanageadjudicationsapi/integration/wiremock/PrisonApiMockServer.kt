package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response

@Deprecated("remove post migration - and remove from health")
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
}
