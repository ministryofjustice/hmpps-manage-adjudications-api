package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
class SpringOauthIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun beforeEach() {
    prisonApiMockServer.stubGetAdjudication()
  }

  @Test
  fun `should request a new token for each request`() {
    oAuthMockServer.stubGrantToken()

    webTestClient
      .get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders(username = "USER1"))
      .exchange().expectStatus().is2xxSuccessful

    webTestClient
      .get()
      .uri("/reported-adjudications/1524242")
      .headers(setHeaders(username = "USER2"))
      .exchange().expectStatus().is2xxSuccessful

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER1"))
    )

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER2"))
    )
  }
}
