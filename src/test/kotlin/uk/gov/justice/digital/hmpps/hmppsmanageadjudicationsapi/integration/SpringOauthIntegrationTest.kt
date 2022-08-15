package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationResponse

@ActiveProfiles("test")
class SpringOauthIntegrationTest : IntegrationTestBase() {

  private lateinit var intTestData: IntegrationTestData
  private lateinit var draftCreationResponseForAdjudication1: DraftAdjudicationResponse
  private lateinit var draftCreationResponseForAdjudication2: DraftAdjudicationResponse

  @BeforeEach
  fun beforeEach() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)

    intTestData = IntegrationTestData(webTestClient, jwtAuthHelper, prisonApiMockServer)

    draftCreationResponseForAdjudication1 = intTestData.startNewAdjudication(IntegrationTestData.ADJUDICATION_1)
    intTestData.setApplicableRules(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1)
    intTestData.setIncidentRole(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1)
    intTestData.setAssociatedPrisoner(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1)
    intTestData.setOffenceDetails(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1)

    draftCreationResponseForAdjudication2 = intTestData.startNewAdjudication(IntegrationTestData.ADJUDICATION_2)
    intTestData.setApplicableRules(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2)
    intTestData.setIncidentRole(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2)
    intTestData.setAssociatedPrisoner(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2)
    intTestData.setOffenceDetails(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2)
  }

  @Test
  fun `should request a new token for each request`() {
    oAuthMockServer.stubGrantToken()

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication1,
      IntegrationTestData.ADJUDICATION_1,
      setHeaders(username = "USER1")
    )

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication2,
      IntegrationTestData.ADJUDICATION_2,
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
