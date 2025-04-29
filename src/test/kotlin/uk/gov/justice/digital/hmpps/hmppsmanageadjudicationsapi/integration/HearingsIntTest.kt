package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime
import java.util.*

@Import(TestOAuth2Config::class)
class HearingsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create a hearing `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData)

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "locationUuid" to UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-12T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV.name)
  }

  @Test
  fun `create a hearing illegal state on hearing type `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData)

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "locationUuid" to UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `amend a hearing `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing()
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "locationUuid" to UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_ADULT.name,
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId")
      .isEqualTo(3)
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing")
      .isEqualTo("2010-10-26T10:00:00")
      .jsonPath("$.reportedAdjudication.hearings[0].id").isNotEmpty
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(OicHearingType.GOV_ADULT.name)
  }

  @Test
  fun `amend a hearing illegal state on hearing type `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing()
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
          "dateTimeOfHearing" to dateTimeOfHearing.plusDays(1),
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `delete a hearing `() {
    val testData = IntegrationTestData.getDefaultAdjudication()
    val scenario = initDataForUnScheduled(testData = testData).createHearing()

    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status")
      .isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
  }

  @Test
  fun `get all hearings`() {
    val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "BSI")
    val scenario = initDataForUnScheduled(testData = testData).createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/hearings?hearingDate=${testData.dateTimeOfHearing!!.toLocalDate()}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_VIEW_ADJUDICATIONS"), activeCaseload = "BSI"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hearings.size()").isEqualTo(1)
      .jsonPath("$.hearings[0].prisonerNumber")
      .isEqualTo(testData.prisonerNumber)
      .jsonPath("$.hearings[0].status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.hearings[0].chargeNumber")
      .isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.hearings[0].chargeNumber")
      .isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.hearings[0].dateTimeOfDiscovery")
      .isEqualTo(testData.dateTimeOfDiscoveryISOString!!)
      .jsonPath("$.hearings[0].dateTimeOfHearing")
      .isEqualTo(testData.dateTimeOfHearingISOString!!)
  }

  @Test
  fun `get hearings by prisoner`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "AE99999")
    initDataForUnScheduled(testData = testData).createHearing()
    val hearingDate = testData.dateTimeOfHearing!!.toLocalDate()

    webTestClient.post()
      .uri("/reported-adjudications/hearings/MDI?startDate=$hearingDate&endDate=$hearingDate")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_VIEW_ADJUDICATIONS")))
      .bodyValue(
        listOf("AE99999"),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo("AE99999")
      .jsonPath("$.[0].hearing.dateTimeOfHearing").isEqualTo("2010-11-19T10:00:00")
      .jsonPath("$.[0].hearing.locationId").isEqualTo(testData.locationId)
      .jsonPath("$.[0].hearing.locationUuid").isEqualTo("0194ad42-6616-72e9-96e5-e4fe9356e32b")
  }
}
