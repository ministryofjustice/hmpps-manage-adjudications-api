package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.test.context.bean.override.mockito.MockitoBean
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationService
import java.time.LocalDateTime
import java.util.UUID

class FixLocationIntTest : SqsIntegrationTestBase() {

  @MockitoBean
  private lateinit var locationService: LocationService

  val uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
  val uuid3 = UUID.fromString("33333333-3333-3333-3333-333333333333")
  val uuid4 = UUID.fromString("44444444-4444-4444-4444-444444444444")

  @BeforeEach
  fun setUp() {
    whenever(locationService.getNomisLocationDetail("12345"))
      .thenReturn(LocationResponse("11111111-1111-1111-1111-111111111111", 12345))

    whenever(locationService.getNomisLocationDetail("23456"))
      .thenReturn(LocationResponse("22222222-2222-2222-2222-222222222222", 23456))

    whenever(locationService.getNomisLocationDetail("34567"))
      .thenReturn(LocationResponse("33333333-3333-3333-3333-333333333333", 34567))

    whenever(locationService.getNomisLocationDetail("45678"))
      .thenReturn(LocationResponse("44444444-4444-4444-4444-444444444444", 45678))
  }

  @Test
  fun `run location job for updating incident details`() {
    val draftId1 = createDraftAdjudication(12345)
    val draftId2 = createDraftAdjudication(23456)
    val draftId3 = createDraftAdjudication(34567)
    val draftId4 = createDraftAdjudication(45678, uuid4)

    runFixLocationJob()
    Thread.sleep(1000)

    assertIncidentDetailsLocationUuidUpdated(draftId = draftId1, locationId = 12345, locationUuid = uuid1)
    assertIncidentDetailsLocationUuidUpdated(draftId = draftId2, locationId = 23456, locationUuid = uuid2)
    assertIncidentDetailsLocationUuidUpdated(draftId = draftId3, locationId = 34567, locationUuid = uuid3)
    assertIncidentDetailsLocationUuidUpdated(draftId = draftId4, locationId = 45678, locationUuid = uuid4)

    verify(locationService, times(1)).getNomisLocationDetail(eq("12345"))
    verify(locationService, times(1)).getNomisLocationDetail(eq("23456"))
    verify(locationService, times(1)).getNomisLocationDetail(eq("34567"))
    verifyNoMoreInteractions(locationService)
  }

  @Test
  fun `run location job for updating reported adjudications`() {
    val testAdjudication1 = IntegrationTestData.getDefaultAdjudication().copy(locationId = 12345, locationUuid = null)
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

    val testAdjudication2 = IntegrationTestData.getDefaultAdjudication().copy(locationId = 23456, locationUuid = null)
    val intTestData2 = integrationTestData()
    val intTestBuilder2 = IntegrationTestScenarioBuilder(
      intTestData = intTestData2,
      intTestBase = this,
      activeCaseload = testAdjudication2.agencyId,
    )

    intTestBuilder2
      .startDraft(testAdjudication2)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testAdjudication2.agencyId).issueReport()

//    third

    val testAdjudication3 = IntegrationTestData.getDefaultAdjudication().copy(locationId = 34567, locationUuid = uuid3)
    val intTestData3 = integrationTestData()
    val intTestBuilder3 = IntegrationTestScenarioBuilder(
      intTestData = intTestData3,
      intTestBase = this,
      activeCaseload = testAdjudication3.agencyId,
    )

    intTestBuilder3
      .startDraft(testAdjudication3)
      .setApplicableRules()
      .setIncidentRole()
      .setOffenceData()
      .addDamages()
      .addIncidentStatement()
      .completeDraft()
      .acceptReport(activeCaseload = testAdjudication3.agencyId).issueReport()

    runFixLocationJob()
    Thread.sleep(1000)

    val reportAdjudicationDto1: ReportedAdjudicationDto = getReportedAdjudication(testAdjudication1.chargeNumber!!)
    val reportAdjudicationDto2: ReportedAdjudicationDto = getReportedAdjudication(testAdjudication2.chargeNumber!!)
    val reportAdjudicationDto3: ReportedAdjudicationDto = getReportedAdjudication(testAdjudication3.chargeNumber!!)

    assertEquals(12345L, reportAdjudicationDto1.incidentDetails.locationId)
    assertEquals(uuid1, reportAdjudicationDto1.incidentDetails.locationUuid)

    assertEquals(23456L, reportAdjudicationDto2.incidentDetails.locationId)
    assertEquals(uuid2, reportAdjudicationDto2.incidentDetails.locationUuid)

    assertEquals(34567L, reportAdjudicationDto3.incidentDetails.locationId)
    assertEquals(uuid3, reportAdjudicationDto3.incidentDetails.locationUuid)

    verify(locationService, times(1)).getNomisLocationDetail(eq("12345"))
    verify(locationService, times(1)).getNomisLocationDetail(eq("23456"))
    verifyNoMoreInteractions(locationService)
  }

  @Test
  fun `run location job for updating hearings`() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData)

    val reportAdjudicationDto: ReportedAdjudicationDto = getReportedAdjudication(scenario.getGeneratedChargeNumber())

    assertTrue(reportAdjudicationDto.hearings.isEmpty())

    createHearingWithoutLocationUuid(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      locationId = 12345,
      date = LocalDateTime.of(2022, 1, 1, 1, 1, 1),
    )
    createHearingWithoutLocationUuid(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      locationId = 23456,
      date = LocalDateTime.of(2022, 1, 1, 2, 1, 1),
    )
    createHearingWithoutLocationUuid(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      locationId = 34567,
      date = LocalDateTime.of(2022, 1, 1, 3, 1, 1),
    )
    createHearingWithoutLocationUuid(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      locationId = 34567,
      locationUuid = uuid3,
      LocalDateTime.of(2022, 1, 1, 3, 1, 2),
    )

    val reportAdjudicationDto1: ReportedAdjudicationDto = getReportedAdjudication(scenario.getGeneratedChargeNumber())

    with(reportAdjudicationDto1.hearings) {
      this.single { it.locationId == 12345L && it.locationUuid == null }
      this.single { it.locationId == 23456L && it.locationUuid == null }
      this.single { it.locationId == 34567L && it.locationUuid == null }
      this.single { it.locationId == 34567L && it.locationUuid == uuid3 }
    }

    runFixLocationJob()
    Thread.sleep(1000)

    val reportAdjudicationDto2: ReportedAdjudicationDto = getReportedAdjudication(scenario.getGeneratedChargeNumber())
    with(reportAdjudicationDto2.hearings) {
      this.single { it.locationId == 12345L && it.locationUuid == uuid1 }
      this.single { it.locationId == 23456L && it.locationUuid == uuid2 }
      this.count { it.locationId == 34567L && it.locationUuid == uuid3 }.times(2)
    }

    verify(locationService, times(1)).getNomisLocationDetail(eq("12345"))
    verify(locationService, times(1)).getNomisLocationDetail(eq("23456"))
    verify(locationService, times(1)).getNomisLocationDetail(eq("34567"))
    verifyNoMoreInteractions(locationService)
  }

  private fun runFixLocationJob() = webTestClient.post()
    .uri("/job/adjudications-fix-locations")
    .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
    .exchange()
    .expectStatus().isCreated

  private fun createDraftAdjudication(locationId: Long, locationUuid: UUID? = null): Long {
    val draftAdjudicationResponse = webTestClient.post()
      .uri("/draft-adjudications")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "prisonerNumber" to "A12345",
          "agencyId" to "MDI",
          "locationId" to locationId,
          "locationUuid" to locationUuid,
          "dateTimeOfIncident" to LocalDateTime.of(2010, 10, 12, 10, 0),
          "dateTimeOfDiscovery" to LocalDateTime.of(2010, 10, 12, 10, 0),
          "isYouthOffenderRule" to false,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    assertEquals(locationId, draftAdjudicationResponse.draftAdjudication.incidentDetails.locationId)
    assertEquals(locationUuid, draftAdjudicationResponse.draftAdjudication.incidentDetails.locationUuid)

    return draftAdjudicationResponse.draftAdjudication.id
  }

  private fun createHearingWithoutLocationUuid(chargeNumber: String, locationId: Long, locationUuid: UUID? = null, date: LocalDateTime) {
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/v2")
      .headers(setHeaders())
      .bodyValue(
        mapOf(
          "locationId" to locationId,
          "dateTimeOfHearing" to date,
          "oicHearingType" to OicHearingType.GOV.name,
          "locationUuid" to locationUuid,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/adjourn")
      .headers(setHeaders(username = "ITAG_ALO"))
      .bodyValue(
        mapOf(
          "details" to "details",
          "adjudicator" to "testing",
          "reason" to HearingOutcomeAdjournReason.LEGAL_ADVICE,
          "plea" to HearingOutcomePlea.UNFIT,
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }

  private fun assertIncidentDetailsLocationUuidUpdated(draftId: Long, locationId: Long, locationUuid: UUID) {
    val updatedLocationDraft = webTestClient.get()
      .uri("/draft-adjudications/$draftId")
      .headers(setHeaders(username = "username", activeCaseload = "MDI"))
      .exchange()
      .expectStatus().isOk
      .returnResult(DraftAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    assertEquals(locationId, updatedLocationDraft.draftAdjudication.incidentDetails.locationId)
    assertEquals(locationUuid, updatedLocationDraft.draftAdjudication.incidentDetails.locationUuid)
  }

  private fun getReportedAdjudication(chargeNumber: String): ReportedAdjudicationDto {
    val reportedAdjudicationResponse = webTestClient.get()
      .uri("/reported-adjudications/$chargeNumber/v2")
      .headers(setHeaders(username = "username", activeCaseload = "MDI"))
      .exchange()
      .expectStatus().isOk
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    return reportedAdjudicationResponse.reportedAdjudication
  }
}
