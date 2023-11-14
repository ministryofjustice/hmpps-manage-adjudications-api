package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.TestBase
import java.time.LocalDateTime
import java.util.Optional

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase : TestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var flyway: Flyway

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @MockBean
  lateinit var dateTimeProvider: DateTimeProvider

  @SpyBean
  lateinit var auditingHandler: AuditingHandler

  companion object {

    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @JvmField
    internal val oAuthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
      oAuthMockServer.start()
      oAuthMockServer.stubGrantToken()
      oAuthMockServer.stubHealthPing(200)
      prisonApiMockServer.stubHealth()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      oAuthMockServer.stop()
    }
  }

  @AfterEach
  fun resetDb() {
    flyway.clean()
    flyway.migrate()
  }

  fun setHeaders(
    contentType: MediaType = MediaType.APPLICATION_JSON,
    username: String? = "ITAG_USER",
    roles: List<String> = listOf("ROLE_ADJUDICATIONS_REVIEWER", "ROLE_VIEW_ADJUDICATIONS"),
    activeCaseload: String? = "MDI",
  ): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles, scope = listOf("write")))
    it.set("Active-Caseload", activeCaseload)
    it.contentType = contentType
  }

  fun setAuditTime(auditDateTime: LocalDateTime? = null) {
    if (auditDateTime == null) {
      auditingHandler.setDateTimeProvider(null)
    } else {
      auditingHandler.setDateTimeProvider(dateTimeProvider)
      whenever(dateTimeProvider.now).thenReturn(Optional.of(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME))
    }
  }

  fun integrationTestData(): IntegrationTestData {
    return IntegrationTestData(webTestClient, jwtAuthHelper)
  }

  protected fun initDataForAccept(overrideAgencyId: String? = null, testData: AdjudicationIntTestDataSet = IntegrationTestData.DEFAULT_ADJUDICATION, incDamagesEvidenceWitnesses: Boolean = true): IntegrationTestScenario {
    oAuthMockServer.stubGrantToken()
    val intTestData = integrationTestData()

    val draftUserHeaders = if (overrideAgencyId != null) {
      setHeaders(username = testData.createdByUserId, activeCaseload = overrideAgencyId)
    } else {
      setHeaders(username = testData.createdByUserId)
    }
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = draftUserHeaders,
    )

    return if (incDamagesEvidenceWitnesses) {
      draftIntTestScenarioBuilder
        .startDraft(testData)
        .setApplicableRules()
        .setIncidentRole()
        .setAssociatedPrisoner()
        .setOffenceData()
        .addIncidentStatement()
        .addDamages()
        .addEvidence()
        .addWitnesses()
        .completeDraft()
    } else {
      draftIntTestScenarioBuilder
        .startDraft(testData)
        .setApplicableRules()
        .setIncidentRole()
        .setAssociatedPrisoner()
        .setOffenceData()
        .addIncidentStatement()
        .completeDraft()
    }
  }

  protected fun initDataForUnScheduled(adjudication: AdjudicationIntTestDataSet = IntegrationTestData.DEFAULT_ADJUDICATION): IntegrationTestScenario {
    val intTestData = integrationTestData()
    val draftUserHeaders = setHeaders(username = adjudication.createdByUserId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = draftUserHeaders,
    )

    return draftIntTestScenarioBuilder
      .startDraft(adjudication)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()
      .acceptReport()
  }

  fun migrateRecord(dto: AdjudicationMigrateDto) {
    val body = objectMapper.writeValueAsString(dto)

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(username = "hmpps-prisoner-from-nomis-migration-adjudications-1", activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
  }
}
