package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse

@ActiveProfiles("test")
class SpringOauthIntegrationTest : IntegrationTestBase() {

  private lateinit var intTestData: IntegrationTestData
  private lateinit var draftCreationResponseForAdjudication1: DraftAdjudicationResponse
  private lateinit var draftCreationResponseForAdjudication2: DraftAdjudicationResponse

  @BeforeEach
  fun beforeEach() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)

    intTestData = IntegrationTestData(webTestClient, jwtAuthHelper, prisonApiMockServer)

    val headers = setHeaders(activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId)

    draftCreationResponseForAdjudication1 = intTestData.startNewAdjudication(IntegrationTestData.ADJUDICATION_1, headers)
    intTestData.setApplicableRules(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1, headers)
    intTestData.setIncidentRole(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1, headers)
    intTestData.setAssociatedPrisoner(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1, headers)
    intTestData.setOffenceDetails(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1, headers)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication1, IntegrationTestData.ADJUDICATION_1, headers)

    val headers2 = setHeaders(activeCaseload = IntegrationTestData.ADJUDICATION_2.agencyId)

    draftCreationResponseForAdjudication2 = intTestData.startNewAdjudication(IntegrationTestData.ADJUDICATION_2, headers2)
    intTestData.setApplicableRules(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2, headers2)
    intTestData.setIncidentRole(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2, headers2)
    intTestData.setAssociatedPrisoner(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2, headers2)
    intTestData.setOffenceDetails(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2, headers2)
    intTestData.addIncidentStatement(draftCreationResponseForAdjudication2, IntegrationTestData.ADJUDICATION_2, headers2)
  }

  @Test
  fun `should request a new token for each request`() {
    oAuthMockServer.stubGrantToken()

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication1,
      IntegrationTestData.ADJUDICATION_1,
      setHeaders(username = "USER1", activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId),
    )

    intTestData.completeDraftAdjudication(
      draftCreationResponseForAdjudication2,
      IntegrationTestData.ADJUDICATION_2,
      setHeaders(username = "USER2", activeCaseload = IntegrationTestData.ADJUDICATION_2.agencyId),
    )

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER1")),
    )

    oAuthMockServer.verify(
      WireMock.postRequestedFor(WireMock.urlEqualTo("/auth/oauth/token"))
        .withRequestBody(WireMock.equalTo("grant_type=client_credentials&scope=write&username=USER2")),
    )
  }
}
