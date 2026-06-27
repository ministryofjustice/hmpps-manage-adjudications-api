package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import jakarta.persistence.EntityManager
import net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.IncidentRoleRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_REPORTED_DATE_TIME
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_LOCATION_UUID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationDetailResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerSearchService
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

@Import(TestOAuth2Config::class, SarIntegrationTestHelperConfig::class, SubjectAccessRequestMigrationIntTest.SarHelperConfig::class)
class SubjectAccessRequestMigrationIntTest :
  SqsIntegrationTestBase(),
  SarFlywaySchemaTest,
  SarJpaEntitiesTest {

  @MockitoBean
  private lateinit var prisonerSearchService: PrisonerSearchService

  @MockitoBean
  private lateinit var locationService: LocationService

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var entityManager: EntityManager

  @BeforeEach
  fun setUp() {
    // This test asserts absolute, sequence-generated identifiers (charge numbers from the
    // per-agency *_CHARGE_SEQUENCE sequences, plus hearing/outcome/punishment identity ids),
    // and its expected snapshots rely on those sequences climbing across the test methods in
    // this class (MDI-000001, MDI-000002, ...). The base cleanup only deletes rows, leaving
    // sequences advanced by whatever ran before, so the assertions are only stable when this
    // class runs first against a fresh container. Test-class ordering is not guaranteed, so
    // reset to a pristine state once - before the first method - to make the expected ids
    // deterministic regardless of execution order while preserving the intra-class progression.
    if (!databaseReset) {
      resetDatabaseToPristineState()
      databaseReset = true
    }
    setAuditTime(DEFAULT_REPORTED_DATE_TIME)
  }

  private fun resetDatabaseToPristineState() {
    dataSource.connection.use { connection ->
      connection.createStatement().use { statement ->
        val tables = mutableListOf<String>()
        statement.executeQuery(
          "SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename <> 'flyway_schema_history'",
        ).use { rs -> while (rs.next()) tables.add(rs.getString(1)) }

        if (tables.isNotEmpty()) {
          statement.execute(
            tables.joinToString(prefix = "TRUNCATE TABLE ", postfix = " RESTART IDENTITY CASCADE") { "public.\"$it\"" },
          )
        }

        val sequences = mutableListOf<String>()
        statement.executeQuery(
          "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = 'public'",
        ).use { rs -> while (rs.next()) sequences.add(rs.getString(1)) }

        sequences.forEach { statement.execute("ALTER SEQUENCE public.\"$it\" RESTART WITH 1") }
      }
    }
  }

  @Test
  fun `SAR template endpoint returns the copied template`() {
    val expectedTemplate = this::class.java.getResource("/template_hmpps-manage-adjudications-api.mustache")!!.readText()

    webTestClient.get()
      .uri("/subject-access-request/template")
      .headers(hmppsHeaders(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
      .expectBody(String::class.java)
      .isEqualTo(expectedTemplate)
  }

  @Test
  fun `SAR API should return expected data`() {
    setupTestData()

    val response = requestSarData()
    val contentJson = objectMapper.readTree(response).get("content").toString()

    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      sarIntegrationTestHelper.saveContentToFile(contentJson, "sar-api-response.json.log")
    } else {
      assertThatJson(contentJson).`as`("Response content json")
        .isEqualTo(sarIntegrationTestHelper.getExpectedSarJson())
    }
  }

  @Test
  fun `SAR report should render as expected`() {
    setupTestData()
    sarIntegrationTestHelper.stubFindPrisonNameWith("Moorland (HMP & YOI)")
    sarIntegrationTestHelper.stubFindUserLastNameWith("Johnson")
    sarIntegrationTestHelper.stubFindLocationNameByNomisIdWith("PROPERTY BOX 1")
    sarIntegrationTestHelper.stubFindLocationNameByDpsIdWith("PROPERTY BOX 2")

    val dataResponse = requestSarData()
    val content = objectMapper.treeToValue(
      objectMapper.readTree(dataResponse).get("content"),
      Array<ReportedAdjudicationDto>::class.java,
    )
    val templateResponse = requestSarTemplate()
    val renderResult = sarIntegrationTestHelper.renderServiceReport(
      data = content,
      templateVersion = "1.0",
      template = templateResponse,
    )

    sarIntegrationTestHelper.renderAndSaveReportAsPdf(renderResult, getPrn(), getCrn())
    if (System.getenv("SAR_GENERATE_ACTUAL").toBoolean()) {
      sarIntegrationTestHelper.saveGeneratedReport(renderResult)
    } else {
      sarIntegrationTestHelper.assertHtmlEquals(renderResult, sarIntegrationTestHelper.getExpectedRenderResult())
    }
  }

  private fun setupTestData() {
    val locationUuid = UUID.fromString(UPDATED_LOCATION_UUID)
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = PRISONER_NUMBER)
    val userHeaders = hmppsHeaders(
      username = testData.createdByUserId,
      roles = listOf("ROLE_ADJUDICATIONS_REVIEWER", "ROLE_VIEW_ADJUDICATIONS"),
      activeCaseload = testData.agencyId,
    )

    whenever(prisonerSearchService.getPrisonerDetail(PRISONER_NUMBER)).thenReturn(
      PrisonerResponse(
        firstName = "Jane",
        lastName = "Doe",
      ),
    )
    whenever(locationService.getLocationDetail(locationUuid)).thenReturn(
      LocationDetailResponse(
        id = locationUuid.toString(),
        prisonId = testData.agencyId,
        pathHierarchy = "PROPERTY BOX 1",
        localName = "PROPERTY BOX 1",
        key = "MDI-PROPERTY-BOX-1",
      ),
    )

    val draft = startDraft(testData, userHeaders)
    setApplicableRules(draft, testData, userHeaders)
    setIncidentRole(draft, testData, userHeaders)
    setAssociatedPrisoner(draft, testData, userHeaders)
    setOffenceDetails(draft, testData, userHeaders)
    addIncidentStatement(draft, testData, userHeaders)
    addDamages(draft, testData, userHeaders)
    addEvidence(draft, testData, userHeaders)
    addWitnesses(draft, testData, userHeaders)
    val reported = completeDraft(draft, userHeaders)
    acceptReport(reported.chargeNumber, testData.agencyId)
    issueReport(draft, reported.chargeNumber, userHeaders)
    createHearing(reported.chargeNumber, testData, userHeaders)
    createChargeProved(reported.chargeNumber, userHeaders)
    createPunishment(reported.chargeNumber, userHeaders)
  }

  private fun getPrn(): String = PRISONER_NUMBER

  private fun getCrn(): String? = null

  private fun getFromDate(): LocalDate = IntegrationTestData.DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().minusDays(1)

  private fun getToDate(): LocalDate = IntegrationTestData.DEFAULT_DATE_TIME_OF_INCIDENT.toLocalDate().plusDays(2)

  private fun requestSarData(): String = webTestClient.get().uri {
    it.path("/subject-access-request")
      .queryParam("prn", getPrn())
      .queryParam("fromDate", getFromDate())
      .queryParam("toDate", getToDate())
      .build()
  }
    .headers(hmppsHeaders(username = "IT_SAR", roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectBody(String::class.java)
    .returnResult().responseBody!!

  private fun requestSarTemplate(): String = webTestClient.get()
    .uri("/subject-access-request/template")
    .headers(hmppsHeaders(username = "IT_SAR", roles = listOf("ROLE_SAR_DATA_ACCESS")))
    .exchange()
    .expectStatus().isOk
    .expectBody(String::class.java)
    .returnResult().responseBody!!

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  private fun startDraft(
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ): DraftAdjudicationResponse = webTestClient.post()
    .uri("/draft-adjudications")
    .headers(headers)
    .bodyValue(
      mapOf(
        "prisonerNumber" to testData.prisonerNumber,
        "offenderBookingId" to testData.offenderBookingId,
        "gender" to testData.gender.name,
        "agencyId" to testData.agencyId,
        "locationId" to testData.locationId,
        "locationUuid" to testData.locationUuid,
        "dateTimeOfIncident" to testData.dateTimeOfIncident,
        "dateTimeOfDiscovery" to testData.dateTimeOfDiscovery,
      ),
    )
    .exchange()
    .expectStatus().is2xxSuccessful
    .returnResult(DraftAdjudicationResponse::class.java)
    .responseBody
    .blockFirst()!!

  private fun setApplicableRules(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/applicable-rules")
      .headers(headers)
      .bodyValue(mapOf("isYouthOffenderRule" to testData.isYouthOffender))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun setIncidentRole(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/incident-role")
      .headers(headers)
      .bodyValue(
        mapOf(
          "incidentRole" to IncidentRoleRequest(testData.incidentRoleCode),
          "removeExistingOffences" to true,
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun setAssociatedPrisoner(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/associated-prisoner")
      .headers(headers)
      .bodyValue(mapOf("associatedPrisonersNumber" to testData.incidentRoleAssociatedPrisonersNumber))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun setOffenceDetails(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/offence-details")
      .headers(headers)
      .bodyValue(
        mapOf(
          "offenceDetails" to testData.offence.also {
            it.protectedCharacteristics = testData.protectedCharacteristics
          },
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun addIncidentStatement(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.post()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/incident-statement")
      .headers(headers)
      .bodyValue(mapOf("statement" to testData.statement))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun addDamages(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/damages")
      .headers(headers)
      .bodyValue(mapOf("damages" to testData.damages))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun addEvidence(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/evidence")
      .headers(headers)
      .bodyValue(mapOf("evidence" to testData.evidence))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun addWitnesses(
    draft: DraftAdjudicationResponse,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/draft-adjudications/${draft.draftAdjudication.id}/witnesses")
      .headers(headers)
      .bodyValue(mapOf("witnesses" to testData.witnesses))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun completeDraft(
    draft: DraftAdjudicationResponse,
    headers: (HttpHeaders) -> Unit,
  ): ReportedAdjudicationDto = webTestClient.post()
    .uri("/draft-adjudications/${draft.draftAdjudication.id}/complete-draft-adjudication")
    .headers(headers)
    .exchange()
    .expectStatus().is2xxSuccessful
    .returnResult(ReportedAdjudicationDto::class.java)
    .responseBody
    .blockFirst()!!

  private fun acceptReport(chargeNumber: String, activeCaseload: String) {
    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/status")
      .headers(hmppsHeaders(username = "ITAG_ALO", activeCaseload = activeCaseload))
      .bodyValue(
        mapOf(
          "status" to ReportedAdjudicationStatus.UNSCHEDULED.name,
          "statusReason" to "status reason",
          "statusDetails" to "status details",
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun issueReport(
    draft: DraftAdjudicationResponse,
    chargeNumber: String,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.put()
      .uri("/reported-adjudications/$chargeNumber/issue")
      .headers(headers)
      .bodyValue(mapOf("dateTimeOfIssue" to draft.draftAdjudication.incidentDetails.dateTimeOfDiscovery.plusDays(1)))
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun createHearing(
    chargeNumber: String,
    testData: AdjudicationIntTestDataSet,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(headers)
      .bodyValue(
        mapOf(
          "locationId" to testData.locationId,
          "locationUuid" to testData.locationUuid,
          "dateTimeOfHearing" to testData.dateTimeOfHearing,
          "oicHearingType" to "GOV_ADULT",
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun createChargeProved(
    chargeNumber: String,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/complete-hearing/charge-proved/v2")
      .headers(headers)
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "plea" to HearingOutcomePlea.NOT_GUILTY,
        ),
      )
      .exchange()
      .expectStatus().is2xxSuccessful
  }

  private fun createPunishment(
    chargeNumber: String,
    headers: (HttpHeaders) -> Unit,
  ) {
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/v2")
      .headers(headers)
      .bodyValue(
        mapOf(
          "punishments" to listOf(
            PunishmentRequest(
              type = PunishmentType.CONFINEMENT,
              duration = 10,
              suspendedUntil = LocalDate.of(2010, 12, 1),
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  private fun hmppsHeaders(
    username: String? = "ITAG_USER",
    roles: List<String> = listOf("ROLE_ADJUDICATIONS_REVIEWER", "ROLE_VIEW_ADJUDICATIONS"),
    activeCaseload: String? = "MDI",
  ): (HttpHeaders) -> Unit = {
    it.setBearerAuth(jwtAuthHelper.createJwt(subject = username, roles = roles, scope = listOf("write", "read")))
    it.set("Active-Caseload", activeCaseload)
    it.contentType = MediaType.APPLICATION_JSON
  }

  companion object {
    private const val PRISONER_NUMBER = "SAR1234"

    // A new test instance is created per method, so this static guard ensures the pristine
    // reset runs only once - before the first method of this class.
    private var databaseReset = false
  }

  @TestConfiguration
  class SarHelperConfig {
    @Bean
    fun jwtAuthorisationHelper(): JwtAuthorisationHelper = JwtAuthorisationHelper()
  }
}
