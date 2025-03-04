package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatusCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.DEFAULT_CREATED_USER_ID
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationDetailResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonerSearchService
import java.time.format.DateTimeFormatter

@Import(TestOAuth2Config::class)
class ReportsIntTest : SqsIntegrationTestBase() {
  @MockBean
  private lateinit var prisonerSearchService: PrisonerSearchService

  @MockBean
  private lateinit var locationService: LocationService

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @ParameterizedTest
  @CsvSource(
    "2010-11-13, 2010-11-17, AWAITING_REVIEW, 4",
    "2010-11-14, 2010-11-14, AWAITING_REVIEW, 2",
  )
  fun `return a page of reported adjudications for agency with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
  ) {
    initMyReportData(agencyId = "DTI")

    webTestClient.get()
      .uri("/reported-adjudications/reports?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(activeCaseload = "DTI", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
  }

  @ParameterizedTest
  @CsvSource(
    "2010-11-15, 2010-11-16, AWAITING_REVIEW, 2",
    "2010-11-14, 2010-11-14, AWAITING_REVIEW, 2",
  )
  fun `return a page of reported adjudications completed by the current user with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
  ) {
    initMyReportData(agencyId = "FBI")

    webTestClient.get()
      .uri("/reported-adjudications/my-reports?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(username = DEFAULT_CREATED_USER_ID, activeCaseload = "FBI"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
  }

  @Test
  fun `return a page of reported adjudications completed by the current user`() {
    initMyReportData(agencyId = "FKI")
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "FKI")

    val startDate =
      testData.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = testData.dateTimeOfIncident.toLocalDate().plusDays(4).format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/my-reports?status=AWAITING_REVIEW&startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(username = DEFAULT_CREATED_USER_ID, activeCaseload = "FKI"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(3)
  }

  @Test
  fun `return a page of reported adjudications completed in the current agency`() {
    val intTestData = integrationTestData()

    val testData1 = IntegrationTestData.getDefaultAdjudication(plusDays = 1, agencyId = "BFI")
    val testData2 = IntegrationTestData.getDefaultAdjudication(plusDays = 2, agencyId = "BFI")

    val firstDraftUserHeaders = setHeaders(
      username = IntegrationTestData.USED_BY_DRAFT_NOT_GOING_TO_REFACTOR_OUT.createdByUserId,
      activeCaseload = IntegrationTestData.USED_BY_DRAFT_NOT_GOING_TO_REFACTOR_OUT.agencyId,
    )
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.USED_BY_DRAFT_NOT_GOING_TO_REFACTOR_OUT)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = testData1.createdByUserId, activeCaseload = testData1.agencyId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = secondDraftUserHeaders,
    )
    val secondChargeNumber = secondDraftIntTestScenarioBuilder
      .startDraft(testData1)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .getGeneratedChargeNumber()

    val thirdDraftUserHeaders = setHeaders(username = testData2.createdByUserId, activeCaseload = testData2.agencyId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = thirdDraftUserHeaders,
    )
    val thirdChargeNumber = thirdDraftIntTestScenarioBuilder
      .startDraft(testData2)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .getGeneratedChargeNumber()

    val startDate =
      testData2.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = testData2.dateTimeOfIncident.toLocalDate().plusDays(4).format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/reports?status=AWAITING_REVIEW&startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(activeCaseload = "BFI", username = "NEW_USER", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(2)
      .jsonPath("$.content[0].chargeNumber").isEqualTo(thirdChargeNumber)
      .jsonPath("$.content[1].chargeNumber").isEqualTo(secondChargeNumber)
  }

  @Test
  fun `get adjudications for issue for all locations in agency RSI for date range`() {
    initMyReportData(agencyId = "RSI")

    val intTestData = integrationTestData()
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "RSI")
    val headers = setHeaders(activeCaseload = "RSI", username = testData.createdByUserId)
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = headers,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(testData)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testData.agencyId)
      .issueReport()

    webTestClient.get()
      .uri("/reported-adjudications/for-issue/v2?startDate=2010-11-12&endDate=2020-12-16")
      .headers(headers)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").isEqualTo(testData.createdByUserId)
  }

  @Test
  fun `get issued adjudications for all locations in agency MDI for date range`() {
    initMyReportData(agencyId = "FMI") // ensure more data to filter out

    val intTestData = integrationTestData()
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "FMI")

    val underTestHeaders = setHeaders(username = testData.createdByUserId, activeCaseload = "FMI")
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = underTestHeaders,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(testData)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testData.agencyId)
      .issueReport()
      .createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/for-print?issueStatus=ISSUED&startDate=2010-11-12&endDate=2020-12-20")
      .headers(setHeaders(activeCaseload = "FMI"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").isEqualTo(testData.createdByUserId)
  }

  @Test
  fun `get not issued adjudications for all locations in agency MDI for date range`() {
    initMyReportData(agencyId = "AKI") // ensure more data to filter out

    val intTestData = integrationTestData()
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "AKI")

    val underTestHeaders = setHeaders(username = testData.createdByUserId, activeCaseload = "AKI")
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = underTestHeaders,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(testData)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testData.agencyId)
      .createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/for-print?issueStatus=NOT_ISSUED&startDate=2010-11-12&endDate=2020-12-20")
      .headers(underTestHeaders)
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].dateTimeOfFirstHearing")
      .isEqualTo(IntegrationTestData.DEFAULT_DATE_TIME_OF_HEARING_TEXT)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").doesNotExist()
  }

  @Test
  fun `get report count by agency `() {
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "BLI")
    initDataForAccept(testData = testData, overrideActiveCaseLoad = "BLI")

    webTestClient.get()
      .uri("/reported-adjudications/report-counts")
      .headers(setHeaders(activeCaseload = "BLI"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reviewTotal").isEqualTo(1)
      .jsonPath("$.transferReviewTotal").isEqualTo(0)
  }

  @Test
  fun `get reports for booking`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 19789)
    initDataForUnScheduled(testData = testData)

    webTestClient.get()
      .uri("/reported-adjudications/booking/${testData.offenderBookingId}?agency=${testData.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
      .jsonPath("$.content[0].canActionFromHistory").isEqualTo(true)
  }

  @Test
  fun `get reports for booking with ADA`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 911)
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT)
      .createChargeProved().createPunishments(punishmentType = PunishmentType.ADDITIONAL_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/booking/${testData.offenderBookingId}?status=CHARGE_PROVED&ada=true&agency=${testData.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for booking with PADA`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 1001)
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT)
      .createChargeProved().createPunishments(punishmentType = PunishmentType.PROSPECTIVE_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/booking/${testData.offenderBookingId}?status=CHARGE_PROVED&pada=true&agency=${testData.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for booking with suspended`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 100001)
    initDataForUnScheduled(testData = testData).createHearing().createChargeProved().createPunishments()

    webTestClient.get()
      .uri("/reported-adjudications/booking/${testData.offenderBookingId}?status=CHARGE_PROVED&suspended=true&agency=${testData.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "REPORTS")
    initDataForUnScheduled(testData = testData)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${testData.prisonerNumber}?page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
      .jsonPath("$.content[0].canActionFromHistory").isEqualTo(true)
  }

  @Test
  fun `get reports for prisoner with ADA`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "ADA")
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT)
      .createChargeProved().createPunishments(punishmentType = PunishmentType.ADDITIONAL_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${testData.prisonerNumber}?status=CHARGE_PROVED&ada=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner with PADA`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "PADA")
    initDataForUnScheduled(testData = testData).createHearing(oicHearingType = OicHearingType.INAD_ADULT)
      .createChargeProved().createPunishments(punishmentType = PunishmentType.PROSPECTIVE_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${testData.prisonerNumber}?status=CHARGE_PROVED&pada=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner with suspended`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "SUSP")
    initDataForUnScheduled(testData = testData).createHearing().createChargeProved().createPunishments()

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${testData.prisonerNumber}?status=CHARGE_PROVED&suspended=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get all reports for prisoner`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "REPORT2")
    initDataForUnScheduled(testData = testData)

    webTestClient.get()
      .uri("/reported-adjudications/prisoner/${testData.prisonerNumber}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo(testData.prisonerNumber)
  }

  @Test
  fun `get all reports for booking`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 78987)
    initDataForUnScheduled(testData = testData)

    webTestClient.get()
      .uri("/reported-adjudications/all-by-booking/${testData.offenderBookingId}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo(testData.prisonerNumber)
  }

  @Test
  fun `offender has reports`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData)

    webTestClient.get()
      .uri("/adjudications/booking/${testData.offenderBookingId}/exists")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hasAdjudications").isEqualTo(true)
  }

  @Test
  fun `offender has no reports`() {
    webTestClient.get()
      .uri("/adjudications/booking/12/exists")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hasAdjudications").isEqualTo(false)
  }

  @Test
  fun `SAR returns a 209 if no prn is set`() {
    webTestClient.get()
      .uri("/subject-access-request?crn=12345")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isEqualTo(HttpStatusCode.valueOf(209))
  }

  @Test
  fun `SAR has no content`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A123459")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `SAR has content`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData)

    // Then stub in your test:
    whenever(prisonerSearchService.getPrisonerDetail("ANY_PRN"))
      .thenReturn(PrisonerResponse("Jane", "Doe"))

    whenever(locationService.getNomisLocationDetail("12345"))
      .thenReturn(LocationResponse("A-123", 12345))

    whenever(locationService.getLocationDetail("A-123"))
      .thenReturn(
        LocationDetailResponse(
          "9d306768-26a3-4bce-8b5d-3ec0f8a57b2c",
          "LEI",
          "MAX",
          "RES-AWING-AWING",
          "LEI-RES-AWING-AWING",
        ),
      )

    webTestClient.get()
      .uri("/subject-access-request?prn=${testData.prisonerNumber}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .consumeWith(System.out::println)
      .jsonPath("$.content").isNotEmpty
  }

  @Test
  fun `SAR has content with date`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData)

    // Then stub in your test:
    whenever(prisonerSearchService.getPrisonerDetail("ANY_PRN"))
      .thenReturn(PrisonerResponse("Jane", "Doe"))

    whenever(locationService.getNomisLocationDetail("12345"))
      .thenReturn(LocationResponse("A-123", 12345))

    whenever(locationService.getLocationDetail("A-123"))
      .thenReturn(
        LocationDetailResponse(
          "9d306768-26a3-4bce-8b5d-3ec0f8a57b2c",
          "LEI",
          "MAX",
          "RES-AWING-AWING",
          "LEI-RES-AWING-AWING",
        ),
      )

    webTestClient.get()
      .uri("/subject-access-request?prn=${testData.prisonerNumber}&fromDate=1999-01-01")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .consumeWith(System.out::println)
      .jsonPath("$.content").isNotEmpty
  }

  @Test
  fun `SAR has no content with date`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    initDataForUnScheduled(testData = testData)
    webTestClient.get()
      .uri("/subject-access-request?prn=${testData.prisonerNumber}&toDate=1999-01-01")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus().isNoContent
  }

  private fun initMyReportData(
    agencyId: String,
  ) {
    val intTestData = integrationTestData()

    val testData1 = IntegrationTestData.getDefaultAdjudication(plusDays = 1, agencyId = agencyId)
    val testData2 = IntegrationTestData.getDefaultAdjudication(plusDays = 2, agencyId = agencyId)
    val testData3 = IntegrationTestData.getDefaultAdjudication(plusDays = 3, agencyId = agencyId)
    val testData4 = IntegrationTestData.getDefaultAdjudication(plusDays = 4, agencyId = agencyId)

    val firstDraftUserHeaders = setHeaders(username = testData1.createdByUserId, activeCaseload = testData1.agencyId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )
    firstDraftIntTestScenarioBuilder
      .startDraft(testData1)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = testData2.createdByUserId, activeCaseload = testData2.agencyId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = secondDraftUserHeaders,
    )
    secondDraftIntTestScenarioBuilder
      .startDraft(testData2)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = testData3.createdByUserId, activeCaseload = testData3.agencyId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = thirdDraftUserHeaders,
    )
    thirdDraftIntTestScenarioBuilder
      .startDraft(testData3)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val fourthDraftUserHeaders = setHeaders(username = testData4.createdByUserId, activeCaseload = testData4.agencyId)
    val fourthDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = fourthDraftUserHeaders,
    )
    fourthDraftIntTestScenarioBuilder
      .startDraft(testData4)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
  }
}
