package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonOffenderEventListener
import java.time.Instant
import java.time.LocalDateTime

class TransfersIntTest : SqsIntegrationTestBase() {

  @Nested
  inner class TransfersOriginal {
    private var chargeNumber: String? = null

    fun setUp(from: String = "BXI", to: String = "BLI", prisonerNumber: String) {
      setAuditTime()
      val testData = IntegrationTestData.getDefaultAdjudication(agencyId = from, dateTimeOfIncident = LocalDateTime.now(), prisonerNumber = prisonerNumber)

      chargeNumber = initDataForUnScheduled(testData = testData).createHearing().getGeneratedChargeNumber()
      initDataForAccept(
        overrideActiveCaseLoad = from,
        testData = testData,
      )
      sendEvent(prisonerNumber = testData.prisonerNumber, agencyId = to)

      Thread.sleep(500)
    }

    @Nested
    inner class AccessForSingleReport {
      @BeforeEach
      fun `init`() {
        setUp(prisonerNumber = "T58")
      }

      @CsvSource("BLI,true", "XXX,false")
      @ParameterizedTest
      fun `test access for single report`(agencyId: String, allowed: Boolean) {
        val response = webTestClient.get()
          .uri("/reported-adjudications/$chargeNumber/v2")
          .headers(setHeaders(activeCaseload = agencyId))
          .exchange()

        when (allowed) {
          true -> response.expectStatus().isOk.expectBody().jsonPath("$.reportedAdjudication.overrideAgencyId")
            .isEqualTo(agencyId)

          false -> response.expectStatus().isNotFound
        }
      }
    }

    @Nested
    inner class HearingOverride {
      @BeforeEach
      fun `init`() {
        setUp(prisonerNumber = "T999")
      }

      @Test
      fun `a transferable adjudication sets the latest hearing at the override agency id `() {
        adjourn(chargeNumber = chargeNumber!!, activeCaseLoad = "BXI")

        val dateTimeOfHearing = LocalDateTime.of(2020, 10, 12, 10, 0)

        webTestClient.post()
          .uri("/reported-adjudications/$chargeNumber/hearing/v2")
          .headers(setHeaders(activeCaseload = "BLI", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
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
          .jsonPath("$.reportedAdjudication.hearings[0].agencyId").isEqualTo("BXI")
          .jsonPath("$.reportedAdjudication.hearings[1].agencyId").isEqualTo("BLI")
      }
    }

    @Nested
    inner class CountsIn {
      @BeforeEach
      fun `init`() {
        setUp(from = "DAI", to = "DGI", prisonerNumber = "T91")
      }

      @Test
      fun `get report count by agency - transfer in `() {
        initDataForAccept(
          overrideActiveCaseLoad = "DGI",
          testData = IntegrationTestData.getDefaultAdjudication(agencyId = "DGI"),
        )

        adjourn(activeCaseLoad = "DAI", chargeNumber = chargeNumber!!)

        webTestClient.get()
          .uri("/reported-adjudications/report-counts")
          .headers(setHeaders(activeCaseload = "DGI"))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.reviewTotal").isEqualTo(1)
          .jsonPath("$.transferReviewTotal").isEqualTo(1)
          .jsonPath("$.transferOutTotal").isEqualTo(0)
          .jsonPath("$.transferAllTotal").isEqualTo(1)
          .jsonPath("$.hearingsToScheduleTotal").isEqualTo(1)
      }
    }

    @Nested
    inner class CountsOut {
      @BeforeEach
      fun `init`() {
        setUp(from = "SLI", to = "SMI", prisonerNumber = "T92")
      }

      @Test
      fun `get report count by agency - transfer out `() {
        webTestClient.get()
          .uri("/reported-adjudications/report-counts")
          .headers(setHeaders(activeCaseload = "SLI"))
          .exchange()
          .expectStatus().isOk
          .expectBody()
          .jsonPath("$.reviewTotal").isEqualTo(1)
          .jsonPath("$.transferReviewTotal").isEqualTo(0)
          .jsonPath("$.transferOutTotal").isEqualTo(2)
          .jsonPath("$.transferAllTotal").isEqualTo(2)
          .jsonPath("$.hearingsToScheduleTotal").isEqualTo(0)
      }
    }
  }

  @Nested
  inner class TransfersOut {

    private var chargeNumberAwaitingReview: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "TCI", dateTimeOfIncident = LocalDateTime.now(), prisonerNumber = "T1000")
      chargeNumberAwaitingReview = initDataForAccept(testData = testData, overrideActiveCaseLoad = "TCI").getGeneratedChargeNumber()

      sendEvent(prisonerNumber = testData.prisonerNumber, agencyId = "TSI")

      Thread.sleep(500)
    }

    @Test
    fun `transfers all & out - agency TCI needs to approve the report for agency TSI`() {
      // 1: transfers out / all should list the report awaiting review for TCI
      chargeNumberAwaitingReview?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=ALL&page=0&size=20")
          .headers(setHeaders(activeCaseload = "TCI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }

      chargeNumberAwaitingReview?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=OUT&page=0&size=20")
          .headers(setHeaders(activeCaseload = "TCI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }

      // 2: transfers in / all should not list the report awaiting review for TSI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "TSI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      // 3: TCI will approve it
      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumberAwaitingReview/status")
        .headers(setHeaders(username = "ITAG_ALO", activeCaseload = "TCI"))
        .bodyValue(
          mapOf(
            "status" to ReportedAdjudicationStatus.UNSCHEDULED,
            "statusReason" to "status reason",
            "statusDetails" to "status details",
          ),
        )
        .exchange()

      // 4: transfers in / all should list the report for TSI
      chargeNumberAwaitingReview?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
          .headers(setHeaders(activeCaseload = "TSI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }

      chargeNumberAwaitingReview?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
          .headers(setHeaders(activeCaseload = "TSI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }

      // 5: transfers out / all should not list the report for TCI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "TCI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
    }
  }

  @Nested
  inner class TransfersUnscheduledIn {
    private var chargeNumberUnscheduled: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "T1", agencyId = "PVI", dateTimeOfIncident = LocalDateTime.now())

      chargeNumberUnscheduled = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()
      sendEvent(prisonerNumber = testData.prisonerNumber, agencyId = "LPI")

      Thread.sleep(500)
    }

    @Test
    fun `transfers all & in - agency LPI needs to schedule a hearing for report from PVI`() {
      // 1: transfers in / all should list the report for LPI
      chargeNumberUnscheduled?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
          .headers(setHeaders(activeCaseload = "LPI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }

      chargeNumberUnscheduled?.let {
        webTestClient.get()
          .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
          .headers(setHeaders(activeCaseload = "LPI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
          .exchange()
          .expectStatus().isOk.expectBody()
          .jsonPath("$.content.size()").isEqualTo(1)
          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
      }
      // 2: transfers out / all should not list the report for PVI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "PVI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
        .headers(setHeaders(activeCaseload = "PVI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
      // 3: LPI schedules hearing
      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumberUnscheduled/hearing/v2")
        .headers(setHeaders(activeCaseload = "LPI", username = "ITAG_ALO"))
        .bodyValue(
          mapOf(
            "locationId" to 1000,
            "dateTimeOfHearing" to LocalDateTime.now(),
            "oicHearingType" to OicHearingType.GOV_ADULT,
          ),
        )
        .exchange()
      // 4: transfers in / all should not list the report for LPI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "LPI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      // 5: transfers out / all should not list the report for PVI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "PVI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
    }
  }

  @Nested
  inner class TransfersScheduledIn {

    private var chargeNumberScheduled: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      val testData = IntegrationTestData.getDefaultAdjudication(agencyId = "WLI", dateTimeOfIncident = LocalDateTime.now(), prisonerNumber = "T3")

      chargeNumberScheduled = initDataForUnScheduled(testData = testData).createHearing().getGeneratedChargeNumber()
      sendEvent(prisonerNumber = testData.prisonerNumber, agencyId = "CFI")

      Thread.sleep(500)
    }

//    @Test
//    fun `transfer in scheduled status, should show in OUT for WLI and then in IN for CFI once adjourned`() {
//      chargeNumberScheduled?.let {
//        webTestClient.get()
//          .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
//          .headers(setHeaders(activeCaseload = "WLI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
//          .exchange()
//          .expectStatus().isOk.expectBody()
//          .jsonPath("$.content.size()").isEqualTo(1)
//          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
//      }
//
//      chargeNumberScheduled?.let {
//        webTestClient.get()
//          .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=OUT&page=0&size=20")
//          .headers(setHeaders(activeCaseload = "WLI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
//          .exchange()
//          .expectStatus().isOk.expectBody()
//          .jsonPath("$.content.size()").isEqualTo(1)
//          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
//      }
//
//      chargeNumberScheduled?.let {
//        webTestClient.get()
//          .uri("/reported-adjudications/reports?startDate=2010-11-10&endDate=2024-11-13&status=SCHEDULED&page=0&size=20")
//          .headers(setHeaders(activeCaseload = "CFI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
//          .exchange()
//          .expectStatus().isOk.expectBody()
//          .jsonPath("$.content.size()").isEqualTo(1)
//          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
//      }
//
//      adjourn(activeCaseLoad = "WLI", chargeNumber = chargeNumberScheduled!!)
//
//      chargeNumberScheduled?.let {
//        webTestClient.get()
//          .uri("/reported-adjudications/transfer-reports?status=ADJOURNED&type=ALL&page=0&size=20")
//          .headers(setHeaders(activeCaseload = "CFI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
//          .exchange()
//          .expectStatus().isOk.expectBody()
//          .jsonPath("$.content.size()").isEqualTo(1)
//          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
//      }
//
//      chargeNumberScheduled?.let {
//        webTestClient.get()
//          .uri("/reported-adjudications/transfer-reports?status=ADJOURNED&type=IN&page=0&size=20")
//          .headers(setHeaders(activeCaseload = "CFI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
//          .exchange()
//          .expectStatus().isOk.expectBody()
//          .jsonPath("$.content.size()").isEqualTo(1)
//          .jsonPath("$.content[0].chargeNumber").isEqualTo(it)
//      }
//    }
  }

  private fun sendEvent(
    prisonerNumber: String,
    agencyId: String,
  ) {
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(
          jsonString(
            HMPPSDomainEvent(
              eventType = PrisonOffenderEventListener.PRISONER_TRANSFER_EVENT_TYPE,
              additionalInformation = AdditionalInformation(
                prisonId = agencyId,
                nomsNumber = prisonerNumber,
                reason = "TRANSFERRED",
              ),
              occurredAt = Instant.now(),
              description = "transfer event",
            ),
          ),
        )
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(PrisonOffenderEventListener.PRISONER_TRANSFER_EVENT_TYPE).build(),
          ),
        )
        .build(),
    )
  }

  private fun adjourn(activeCaseLoad: String = "MDI", chargeNumber: String) {
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/hearing/outcome/adjourn")
      .headers(setHeaders(activeCaseload = activeCaseLoad, username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "adjudicator" to "test",
          "reason" to HearingOutcomeAdjournReason.LEGAL_ADVICE,
          "details" to "details",
          "plea" to HearingOutcomePlea.UNFIT,
        ),
      )
      .exchange()
      .expectStatus().isCreated
  }
}
