package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.health.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock

class PrisonApiMockServer : WireMockServer(8999) {
  fun stubHealth() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/health/ping"))
        .willReturn(
          WireMock.aResponse()
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
}
