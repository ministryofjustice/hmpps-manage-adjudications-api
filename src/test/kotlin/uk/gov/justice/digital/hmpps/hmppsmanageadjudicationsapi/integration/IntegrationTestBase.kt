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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.BankHolidayApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import java.time.LocalDateTime
import java.util.*

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

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
    internal val bankHolidayApiMockServer = BankHolidayApiMockServer()

    @JvmField
    internal val oAuthMockServer = OAuthMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
      bankHolidayApiMockServer.start()
      oAuthMockServer.start()
      oAuthMockServer.stubGrantToken()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
      bankHolidayApiMockServer.stop()
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
    roles: List<String> = emptyList()
  ): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles, scope = listOf("write")))
    it.contentType = contentType
  }

  fun setAuditTime(auditDateTime: LocalDateTime) {
    auditingHandler.setDateTimeProvider(dateTimeProvider)
    whenever(dateTimeProvider.now).thenReturn(Optional.of(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME))
  }

  fun integrationTestData(): IntegrationTestData {
    return IntegrationTestData(webTestClient, jwtAuthHelper, bankHolidayApiMockServer, prisonApiMockServer)
  }
}
