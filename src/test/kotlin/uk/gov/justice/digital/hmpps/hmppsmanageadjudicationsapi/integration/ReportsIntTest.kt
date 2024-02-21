package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.format.DateTimeFormatter

class ReportsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @ParameterizedTest
  @CsvSource(
    "2020-12-14, 2020-12-17, AWAITING_REVIEW, 3, MDI-000003",
    "2020-12-15, 2020-12-15, AWAITING_REVIEW, 1, MDI-000002",
  )
  fun `return a page of reported adjudications for agency with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: String,
  ) {
    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/reports?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
      .jsonPath("$.content[0].chargeNumber").isEqualTo(adjudicationNumber)
  }

  @ParameterizedTest
  @CsvSource(
    "2020-12-14, 2020-12-16, AWAITING_REVIEW, 2, MDI-000003",
    "2020-12-14, 2020-12-14, AWAITING_REVIEW, 1, MDI-000001",
  )
  fun `return a page of reported adjudications completed by the current user with filters`(
    startDate: String,
    endDate: String,
    reportedAdjudicationStatus: ReportedAdjudicationStatus,
    expectedCount: Int,
    adjudicationNumber: String,
  ) {
    initMyReportData()

    webTestClient.get()
      .uri("/reported-adjudications/my-reports?startDate=$startDate&endDate=$endDate&status=$reportedAdjudicationStatus&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(expectedCount)
      .jsonPath("$.content[0].chargeNumber").isEqualTo(adjudicationNumber)
  }

  @Test
  fun `return a page of reported adjudications completed by the current user`() {
    initMyReportData()

    val startDate =
      IntegrationTestData.ADJUDICATION_2.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = IntegrationTestData.ADJUDICATION_5.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/my-reports?status=AWAITING_REVIEW&startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].chargeNumber").isEqualTo("MDI-000003")
      .jsonPath("$.content[1].chargeNumber").isEqualTo("MDI-000001")
  }

  @Test
  fun `return a page of reported adjudications completed in the current agency`() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_1.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_1.agencyId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_1)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_2.agencyId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = secondDraftUserHeaders,
    )
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_3.agencyId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = thirdDraftUserHeaders,
    )
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val startDate =
      IntegrationTestData.ADJUDICATION_1.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)
    val endDate = IntegrationTestData.ADJUDICATION_3.dateTimeOfIncident.toLocalDate().format(DateTimeFormatter.ISO_DATE)

    webTestClient.get()
      .uri("/reported-adjudications/reports?status=AWAITING_REVIEW&startDate=$startDate&endDate=$endDate&page=0&size=20")
      .headers(setHeaders(username = "NEW_USER", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content[0].chargeNumber").isEqualTo("MDI-000002")
      .jsonPath("$.content[1].chargeNumber").isEqualTo("MDI-000001")
  }

  @Test
  fun `get adjudications for issue for all locations in agency MDI for date range`() {
    initMyReportData() // ensure more data to filter out

    val intTestData = integrationTestData()
    val underTestHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = underTestHeaders,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport()
      .issueReport()

    webTestClient.get()
      .uri("/reported-adjudications/for-issue?startDate=2010-11-12&endDate=2020-12-16")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
  }

  @Test
  fun `get issued adjudications for all locations in agency MDI for date range`() {
    initMyReportData() // ensure more data to filter out

    val intTestData = integrationTestData()
    val underTestHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = underTestHeaders,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport()
      .issueReport()
      .createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/for-print?issueStatus=ISSUED&startDate=2010-11-12&endDate=2020-12-20")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
  }

  @Test
  fun `get not issued adjudications for all locations in agency MDI for date range`() {
    initMyReportData() // ensure more data to filter out

    val intTestData = integrationTestData()
    val underTestHeaders = setHeaders(username = IntegrationTestData.DEFAULT_ADJUDICATION.createdByUserId)
    val underTestDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = underTestHeaders,
    )
    underTestDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.DEFAULT_ADJUDICATION)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport()
      .createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/for-print?issueStatus=NOT_ISSUED&startDate=2010-11-12&endDate=2020-12-20")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudications.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudications[0].dateTimeOfFirstHearing").isEqualTo(IntegrationTestData.DEFAULT_DATE_TIME_OF_HEARING_TEXT)
      .jsonPath("$.reportedAdjudications[0].issuingOfficer").doesNotExist()
  }

  @Test
  fun `get report count by agency `() {
    initDataForAccept()

    webTestClient.get()
      .uri("/reported-adjudications/report-counts")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reviewTotal").isEqualTo(1)
      .jsonPath("$.transferReviewTotal").isEqualTo(0)
  }

  @Test
  fun `get reports for booking`() {
    initDataForUnScheduled()

    webTestClient.get()
      .uri("/reported-adjudications/booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}?agency=${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
      .jsonPath("$.content[0].canActionFromHistory").isEqualTo(true)
  }

  @Test
  fun `get reports for booking with ADA`() {
    initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved().createPunishments(punishmentType = PunishmentType.ADDITIONAL_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}?status=CHARGE_PROVED&ada=true&agency=${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for booking with PADA`() {
    initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved().createPunishments(punishmentType = PunishmentType.PROSPECTIVE_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}?status=CHARGE_PROVED&pada=true&agency=${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for booking with suspended`() {
    initDataForUnScheduled().createHearing().createChargeProved().createPunishments()

    webTestClient.get()
      .uri("/reported-adjudications/booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}?status=CHARGE_PROVED&suspended=true&agency=${IntegrationTestData.DEFAULT_ADJUDICATION.agencyId}&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner`() {
    initDataForUnScheduled()

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}?page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
      .jsonPath("$.content[0].canActionFromHistory").isEqualTo(true)
  }

  @Test
  fun `get reports for prisoner with ADA`() {
    initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved().createPunishments(punishmentType = PunishmentType.ADDITIONAL_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}?status=CHARGE_PROVED&ada=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner with PADA`() {
    initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved().createPunishments(punishmentType = PunishmentType.PROSPECTIVE_DAYS)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}?status=CHARGE_PROVED&pada=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get reports for prisoner with suspended`() {
    initDataForUnScheduled().createHearing().createChargeProved().createPunishments()

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}?status=CHARGE_PROVED&suspended=true&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }

  @Test
  fun `get all reports for prisoner`() {
    initDataForUnScheduled()

    webTestClient.get()
      .uri("/reported-adjudications/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
  }

  @Test
  fun `get all reports for booking`() {
    initDataForUnScheduled()

    webTestClient.get()
      .uri("/reported-adjudications/all-by-booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
  }

  @Test
  fun `offender has reports`() {
    initDataForUnScheduled()

    webTestClient.get()
      .uri("/adjudications/booking/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}/exists")
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
  fun `SAR has no content`() {
    webTestClient.get()
      .uri("/subject-access-request?prn=A12345")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isNoContent
  }

  @Test
  fun `SAR has content`() {
    initDataForUnScheduled()
    webTestClient.get()
      .uri("/subject-access-request?prn=${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .consumeWith(System.out::println)
      .jsonPath("$.content").isNotEmpty
  }

  @Test
  fun `SAR has content with date`() {
    initDataForUnScheduled()
    webTestClient.get()
      .uri("/subject-access-request?prn=${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}&fromDate=1999-01-01")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .consumeWith(System.out::println)
      .jsonPath("$.content").isNotEmpty
  }

  @Test
  fun `SAR has no content with date`() {
    initDataForUnScheduled()
    webTestClient.get()
      .uri("/subject-access-request?prn=${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}&toDate=1999-01-01")
      .headers(setHeaders(username = "P_NESS", roles = listOf("ROLE_ALL_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isNoContent
  }

  private fun initMyReportData() {
    val intTestData = integrationTestData()

    val firstDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_2.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_2.agencyId)
    val firstDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = firstDraftUserHeaders,
    )
    firstDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_2)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val secondDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_3.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_3.agencyId)
    val secondDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = secondDraftUserHeaders,
    )
    secondDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_3)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val thirdDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_4.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_4.agencyId)
    val thirdDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = thirdDraftUserHeaders,
    )
    thirdDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_4)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()

    val fourthDraftUserHeaders = setHeaders(username = IntegrationTestData.ADJUDICATION_5.createdByUserId, activeCaseload = IntegrationTestData.ADJUDICATION_5.agencyId)
    val fourthDraftIntTestScenarioBuilder = IntegrationTestScenarioBuilder(
      intTestData = intTestData,
      intTestBase = this,
      headers = fourthDraftUserHeaders,
    )
    fourthDraftIntTestScenarioBuilder
      .startDraft(IntegrationTestData.ADJUDICATION_5)
      .setApplicableRules()
      .setIncidentRole()
      .setAssociatedPrisoner()
      .setOffenceData()
      .addIncidentStatement()
      .completeDraft()
  }
}
