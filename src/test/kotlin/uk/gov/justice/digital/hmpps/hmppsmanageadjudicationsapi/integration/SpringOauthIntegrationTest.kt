package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse

@ActiveProfiles("test")
class SpringOauthIntegrationTest : IntegrationTestBase() {

  private lateinit var intTestData: IntTestData
  private lateinit var draftCreationResponseForAdjudication1: DraftAdjudicationResponse
  private lateinit var draftCreationResponseForAdjudication2: DraftAdjudicationResponse

  @BeforeEach
  fun beforeEach() {
    setAuditTime(IntTestData.DEFAULT_REPORTED_DATE_TIME)

    intTestData = IntTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)

    draftCreationResponseForAdjudication1 = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_1)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication1, IntTestData.ADJUDICATION_1)

    draftCreationResponseForAdjudication2 = intTestData.startNewAdjudication(IntTestData.ADJUDICATION_2)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication2, IntTestData.ADJUDICATION_2)
  }

  @Test
  fun `should request a new token for each request`() {
    oAuthMockServer.stubGrantToken()

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication1,
      IntTestData.ADJUDICATION_1,
      setHeaders(username = "USER1")
    )

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication2,
      IntTestData.ADJUDICATION_2,
      setHeaders(username = "USER2")
    )

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
