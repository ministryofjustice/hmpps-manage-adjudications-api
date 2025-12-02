package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.JwtAuthHelper
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.TestBase
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
abstract class IntegrationTestBase : TestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var draftAdjudicationRepository: DraftAdjudicationRepository

  @Autowired
  lateinit var reportedAdjudicationRepository: ReportedAdjudicationRepository

  @MockitoBean
  lateinit var dateTimeProvider: DateTimeProvider

  @MockitoSpyBean
  lateinit var auditingHandler: AuditingHandler

  @BeforeEach
  fun cleanupDatabase() {
    reportedAdjudicationRepository.deleteAll()
    draftAdjudicationRepository.deleteAll()
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
      whenever(dateTimeProvider.now).thenReturn(Optional.of(auditDateTime))
    }
  }

  fun integrationTestData(): IntegrationTestData = IntegrationTestData(webTestClient, jwtAuthHelper)

  protected fun initDataForAccept(
    overrideActiveCaseLoad: String? = null,
    testData: AdjudicationIntTestDataSet,
    incDamagesEvidenceWitnesses: Boolean = true,
    overrideAgencyId: String? = null,
  ): IntegrationTestScenario {
    val intTestData = integrationTestData()

    val draftUserHeaders = if (overrideActiveCaseLoad != null) {
      setHeaders(username = testData.createdByUserId, activeCaseload = overrideActiveCaseLoad)
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

  protected fun initDataForUnScheduled(
    testData: AdjudicationIntTestDataSet,
  ): IntegrationTestScenario {
    val intTestData = integrationTestData()
    val draftUserHeaders = setHeaders(username = testData.createdByUserId, activeCaseload = testData.agencyId)
    val draftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = draftUserHeaders,
    )

    return draftIntTestScenarioBuilder
      .startDraft(testAdjudication = testData)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .addDamages()
      .addEvidence()
      .addWitnesses()
      .completeDraft()
      .acceptReport(activeCaseload = testData.agencyId)
  }

  protected fun getSuspendedPunishments(chargeNumber: String, prisonerNumber: String): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/reported-adjudications/punishments/$prisonerNumber/suspended/v2?chargeNumber=$chargeNumber")
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
