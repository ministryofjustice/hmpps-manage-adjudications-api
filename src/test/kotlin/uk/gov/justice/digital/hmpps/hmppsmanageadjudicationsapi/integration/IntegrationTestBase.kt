package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.TestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase : TestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @MockBean
  lateinit var dateTimeProvider: DateTimeProvider

  @SpyBean
  lateinit var auditingHandler: AuditingHandler

  @Autowired
  lateinit var flyway: Flyway

  @PostConstruct
  fun init() {
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

  protected fun initDataForAccept(
    overrideAgencyId: String? = null,
    testData: AdjudicationIntTestDataSet = IntegrationTestData.DEFAULT_ADJUDICATION,
    incDamagesEvidenceWitnesses: Boolean = true,
  ): IntegrationTestScenario {
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
        .startDraft(testAdjudication = testData, overrideAgencyId = overrideAgencyId)
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
        .startDraft(testAdjudication = testData, overrideAgencyId = overrideAgencyId)
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

  protected fun getSuspendedPunishments(chargeNumber: String): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/suspended/v2?chargeNumber=$chargeNumber")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()

  protected fun createPunishments(
    chargeNumber: String,
    type: PunishmentType = PunishmentType.CONFINEMENT,
    consecutiveChargeNumber: String? = null,
    isSuspended: Boolean = true,
    activatedFrom: String? = null,
    id: Long? = null,
  ): WebTestClient.ResponseSpec {
    val suspendedUntil = LocalDate.now().plusMonths(1)

    return webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = if (activatedFrom.isNullOrBlank()) null else id,
                type = type,
                suspendedUntil = if (isSuspended) suspendedUntil else null,
                startDate = if (isSuspended) null else suspendedUntil,
                endDate = if (isSuspended) null else suspendedUntil.plusDays(10),
                consecutiveChargeNumber = consecutiveChargeNumber,
                activatedFrom = activatedFrom,
                duration = 10,
              ),
            ),
        ),
      )
      .exchange()
  }
}
