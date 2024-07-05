package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

class HearingsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create a hearing `() {
    val scenario = initDataForUnScheduled()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
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
    val scenario = initDataForUnScheduled()

    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 12, 10, 0)
    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to dateTimeOfHearing,
          "oicHearingType" to OicHearingType.GOV_YOI.name,
        ),
      )
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `amend a hearing `() {
    val scenario = initDataForUnScheduled().createHearing()
    val dateTimeOfHearing = LocalDateTime.of(2010, 10, 25, 10, 0)

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 3,
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
    val scenario = initDataForUnScheduled().createHearing()
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
    val scenario = initDataForUnScheduled().createHearing()

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
    val scenario = initDataForUnScheduled().createHearing()

    webTestClient.get()
      .uri("/reported-adjudications/hearings?hearingDate=${IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing!!.toLocalDate()}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_VIEW_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.hearings.size()").isEqualTo(1)
      .jsonPath("$.hearings[0].prisonerNumber")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.hearings[0].status")
      .isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
      .jsonPath("$.hearings[0].chargeNumber")
      .isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.hearings[0].chargeNumber")
      .isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.hearings[0].dateTimeOfDiscovery")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfDiscoveryISOString!!)
      .jsonPath("$.hearings[0].dateTimeOfHearing")
      .isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearingISOString!!)
  }

  @Test
  fun `get hearings by prisoner`() {
    initDataForUnScheduled().createHearing()
    val hearingDate = IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing!!.toLocalDate()

    webTestClient.post()
      .uri("/reported-adjudications/hearings/MDI?date=$hearingDate")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_VIEW_ADJUDICATIONS")))
      .bodyValue(
        listOf(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].prisonerNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber)
      .jsonPath("$.[0].hearing.dateTimeOfHearing").isEqualTo("2010-11-19T10:00:00")
      .jsonPath("$.[0].hearing.locationId").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.locationId)
  }
}
