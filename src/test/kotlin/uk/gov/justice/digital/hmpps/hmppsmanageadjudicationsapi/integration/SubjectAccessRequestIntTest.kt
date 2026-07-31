package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_DATE_TIME_OF_INCIDENT
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_LOCATION_UUID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationDetailResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerSearchService
import java.nio.charset.StandardCharsets
import java.util.UUID

@Import(TestOAuth2Config::class)
@ActiveProfiles("test")
class SubjectAccessRequestIntTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var prisonerSearchService: PrisonerSearchService

  @MockitoBean
  private lateinit var locationService: LocationService

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `get a subject access request with a local name`() {
    val locationUuid: UUID = UUID.fromString(UPDATED_LOCATION_UUID)
    val prisonerNumber: String = "AA1234C"

    whenever(prisonerSearchService.getPrisonerDetail("AA1234C")).thenReturn(
      PrisonerResponse(
        firstName = "firstName",
        lastName = "lastName",
      ),
    )

    whenever(locationService.getLocationDetail(locationUuid)).thenReturn(
      LocationDetailResponse(
        id = locationUuid.toString(),
        prisonId = "MDI",
        pathHierarchy = "Landing",
        localName = "Landing",
        key = "L-1",
      ),
    )

    val testAdjudication1 = IntegrationTestData.getDefaultAdjudication(prisonerNumber = prisonerNumber)
    val intTestData1 = integrationTestData()
    val intTestBuilder1 = IntegrationTestScenarioBuilder(
      intTestData = intTestData1,
      intTestBase = this,
      activeCaseload = testAdjudication1.agencyId,
    )

    intTestBuilder1
      .startDraft(testAdjudication1)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testAdjudication1.agencyId).issueReport()
      .createHearing()

    val fromDate = DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().minusDays(1)
    val toDate = DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().plusDays(1)

    val result = webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
      .headers(setHeaders(username = "IT_SAR", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(SubjectAccessRequestContent::class.java)
      .returnResult().responseBody!!

    assertThat(result.content).hasSize(1)
    assertThat(result.content[0].prisonerName).isEqualTo("firstName lastName")
    assertThat(result.content[0].incidentDetails.locationName).isEqualTo("Landing")
    assertThat(result.content[0].hearings[0].locationName).isEqualTo("Landing")
  }

  @Test
  fun `get a subject access request without a local name`() {
    val locationUuid: UUID = UUID.fromString(UPDATED_LOCATION_UUID)
    val prisonerNumber: String = "AA1234D"

    whenever(prisonerSearchService.getPrisonerDetail("AA1234D")).thenReturn(null)

    whenever(locationService.getLocationDetail(locationUuid)).thenReturn(
      LocationDetailResponse(
        id = locationUuid.toString(),
        prisonId = "MDI",
        pathHierarchy = "Landing",
        localName = null,
        key = "L-1",
      ),
    )
    val testAdjudication1 = IntegrationTestData.getDefaultAdjudication(prisonerNumber = prisonerNumber)
    val intTestData1 = integrationTestData()
    val intTestBuilder1 = IntegrationTestScenarioBuilder(
      intTestData = intTestData1,
      intTestBase = this,
      activeCaseload = testAdjudication1.agencyId,
    )

    intTestBuilder1
      .startDraft(testAdjudication1)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testAdjudication1.agencyId).issueReport()
      .createHearing()

    val fromDate = DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().minusDays(1)
    val toDate = DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().plusDays(1)

    val result = webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
      .headers(setHeaders(username = "IT_SAR", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody(SubjectAccessRequestContent::class.java)
      .returnResult().responseBody!!

    assertThat(result.content).hasSize(1)
    assertThat(result.content[0].prisonerName).isNull()
    assertThat(result.content[0].incidentDetails.locationName).isEqualTo("Unknown")
    assertThat(result.content[0].hearings[0].locationName).isEqualTo("Unknown")
  }

  @Test
  fun `subject access request returns downstream 503 instead of incomplete content`() {
    val prisonerNumber = "AA1234E"
    val testAdjudication = IntegrationTestData.getDefaultAdjudication(prisonerNumber = prisonerNumber)
    val intTestBuilder = IntegrationTestScenarioBuilder(
      intTestData = integrationTestData(),
      intTestBase = this,
      activeCaseload = testAdjudication.agencyId,
    )

    intTestBuilder
      .startDraft(testAdjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testAdjudication.agencyId).issueReport()
      .createHearing()

    val serviceUnavailableException = WebClientResponseException.create(
      HttpStatus.SERVICE_UNAVAILABLE.value(),
      "Service Unavailable",
      HttpHeaders(),
      "<html>Service Unavailable</html>".toByteArray(StandardCharsets.UTF_8),
      null,
    )
    whenever(prisonerSearchService.getPrisonerDetail(prisonerNumber)).thenThrow(serviceUnavailableException)

    webTestClient.get()
      .uri("/subject-access-request?prn=$prisonerNumber")
      .headers(setHeaders(username = "IT_SAR", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
  }
}

data class SubjectAccessRequestContent(val content: List<ReportedAdjudicationDto>)
