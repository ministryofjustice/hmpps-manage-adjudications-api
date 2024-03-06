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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.IssuedStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService
import java.time.Instant
import java.time.LocalDateTime

class TransfersIntTest : SqsIntegrationTestBase() {

  @Nested
  inner class TransfersOriginal {
    var chargeNumber: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      chargeNumber = initDataForUnScheduled().createHearing().getGeneratedChargeNumber()
      initDataForAccept(
        testData =
        IntegrationTestData.DEFAULT_ADJUDICATION.also {
          it.dateTimeOfDiscovery = ReportsService.transferOutAndHearingsToScheduledCutOffDate.plusDays(1).atStartOfDay()
        },
      )
      sendEvent()

      Thread.sleep(500)
    }

    @CsvSource("BXI,true", "XXX,false")
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

    @CsvSource("BXI,1", "XXX,0")
    @ParameterizedTest
    fun `test access for reports `(agencyId: String, total: Int) {
      adjourn(activeCaseLoad = "BXI", chargeNumber = chargeNumber!!)

      webTestClient.get()
        .uri("/reported-adjudications/reports?startDate=2010-11-10&endDate=2024-02-01&status=ADJOURNED&page=0&size=20")
        .headers(
          setHeaders(
            activeCaseload = agencyId,
            username = "P_NESS",
            roles = listOf("ROLE_ADJUDICATIONS_REVIEWER"),
          ),
        )
        .exchange()
        .expectStatus().isOk.expectBody().jsonPath("$.content.size()").isEqualTo(total)
    }

    @CsvSource("BXI,1", "XXX,0")
    @ParameterizedTest
    fun `test access for reports for issue `(agencyId: String, total: Int) {
      webTestClient.get()
        .uri("/reported-adjudications/for-issue?startDate=2010-11-12&endDate=2024-12-16")
        .headers(setHeaders(activeCaseload = agencyId))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudications.size()").isEqualTo(total)
    }

    @CsvSource(
      "BXI,ISSUED,,1",
      "BXI,NOT_ISSUED,,1",
      "BXI,ISSUED,NOT_ISSUED,1",
      "XXX,ISSUED,,0",
      "XXX,NOT_ISSUED,,0",
      "XXX,ISSUED,NOT_ISSUED,0",
    )
    @ParameterizedTest
    fun `test access for reports for print `(
      agencyId: String,
      issuedStatus: IssuedStatus,
      issuedStatus2: IssuedStatus?,
      total: Int,
    ) {
      if (issuedStatus == IssuedStatus.ISSUED) {
        val dateTimeOfIssue = LocalDateTime.of(2022, 11, 29, 10, 0)
        webTestClient.put()
          .uri("/reported-adjudications/$chargeNumber/issue")
          .headers(setHeaders(activeCaseload = "BXI"))
          .bodyValue(
            mapOf(
              "dateTimeOfIssue" to dateTimeOfIssue,
            ),
          )
          .exchange()
          .expectStatus().isOk
      }

      var issueStatues = "$issuedStatus"

      issuedStatus2?.let {
        issueStatues = "$issueStatues,$it"
      }

      webTestClient.get()
        .uri("/reported-adjudications/for-print?issueStatus=$issueStatues&startDate=2010-11-12&endDate=2020-12-20")
        .headers(setHeaders(activeCaseload = agencyId))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .consumeWith(System.out::println)
        .jsonPath("$.reportedAdjudications.size()").isEqualTo(total)
    }

    @Test
    fun `a transferable adjudication sets the latest hearing at the override agency id `() {
      adjourn(chargeNumber = chargeNumber!!)

      val dateTimeOfHearing = LocalDateTime.of(2020, 10, 12, 10, 0)

      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumber/hearing/v2")
        .headers(setHeaders(activeCaseload = "BXI", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
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
        .jsonPath("$.reportedAdjudication.hearings[0].agencyId").isEqualTo("MDI")
        .jsonPath("$.reportedAdjudication.hearings[1].agencyId").isEqualTo("BXI")
    }

    @Test
    fun `get report count by agency - transfer in `() {
      webTestClient.delete()
        .uri("/reported-adjudications/$chargeNumber/hearing/v2")
        .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk

      initDataForAccept(overrideAgencyId = "BXI", testData = IntegrationTestData.DEFAULT_TRANSFER_ADJUDICATION)

      webTestClient.get()
        .uri("/reported-adjudications/report-counts")
        .headers(setHeaders(activeCaseload = "BXI"))
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reviewTotal").isEqualTo(1)
        .jsonPath("$.transferReviewTotal").isEqualTo(1)
        .jsonPath("$.transferOutTotal").isEqualTo(0)
        .jsonPath("$.transferAllTotal").isEqualTo(1)
        .jsonPath("$.hearingsToScheduleTotal").isEqualTo(1)
    }

    @Test
    fun `get report count by agency - transfer out `() {
      webTestClient.get()
        .uri("/reported-adjudications/report-counts")
        .headers(setHeaders(activeCaseload = "MDI"))
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

  @Nested
  inner class TransfersOut {

    var chargeNumberAwaitingReview: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      chargeNumberAwaitingReview = initDataForAccept().getGeneratedChargeNumber()
      sendEvent()

      Thread.sleep(500)
    }

    @Test
    fun `transfers all & out - agency MDI needs to approve the report for agency BXI`() {
      // 1: transfers out / all should list the report awaiting review for MDI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberAwaitingReview)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=OUT&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberAwaitingReview)

      // 2: transfers in / all should not list the report awaiting review for BXI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=AWAITING_REVIEW&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      // 3: MDI will approve it
      webTestClient.put()
        .uri("/reported-adjudications/$chargeNumberAwaitingReview/status")
        .headers(setHeaders(username = "ITAG_ALO", activeCaseload = "MDI"))
        .bodyValue(
          mapOf(
            "status" to ReportedAdjudicationStatus.UNSCHEDULED,
            "statusReason" to "status reason",
            "statusDetails" to "status details",
          ),
        )
        .exchange()

      // 4: transfers in / all should list the report for BXI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberAwaitingReview)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberAwaitingReview)

      // 5: transfers out / all should not list the report for MDI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
    }
  }

  @Nested
  inner class TransfersUnscheduledIn {
    var chargeNumberUnscheduled: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      chargeNumberUnscheduled = initDataForUnScheduled().getGeneratedChargeNumber()
      sendEvent()

      Thread.sleep(500)
    }

    @Test
    fun `transfers all & in - agency BXI needs to schedule a hearing for report from MDI`() {
      // 1: transfers in / all should list the report for BXI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberUnscheduled)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberUnscheduled)
      // 2: transfers out / all should not list the report for MDI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=UNSCHEDULED&type=IN&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
      // 3: BXI schedules hearing
      webTestClient.post()
        .uri("/reported-adjudications/$chargeNumberUnscheduled/hearing/v2")
        .headers(setHeaders(activeCaseload = "BXI", username = "ITAG_ALO"))
        .bodyValue(
          mapOf(
            "locationId" to 1000,
            "dateTimeOfHearing" to LocalDateTime.now(),
            "oicHearingType" to OicHearingType.GOV_ADULT,
          ),
        )
        .exchange()
      // 4: transfers in / all should not list the report for BXI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)

      // 5: transfers out / all should not list the report for MDI
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(0)
    }
  }

  @Nested
  inner class TransfersScheduledIn {

    var chargeNumberScheduled: String? = null

    @BeforeEach
    fun setUp() {
      setAuditTime()
      chargeNumberScheduled = initDataForUnScheduled().createHearing().getGeneratedChargeNumber()
      sendEvent()

      Thread.sleep(500)
    }

    @Test
    fun `transfer in scheduled status, should show in OUT for MDI and then in IN for BXI once adjourned`() {
      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberScheduled)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=SCHEDULED&type=OUT&page=0&size=20")
        .headers(setHeaders(activeCaseload = "MDI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberScheduled)

      webTestClient.get()
        .uri("/reported-adjudications/reports?startDate=2010-11-10&endDate=2024-11-13&status=SCHEDULED&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberScheduled)

      adjourn(activeCaseLoad = "MDI", chargeNumber = chargeNumberScheduled!!)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=ADJOURNED&type=ALL&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberScheduled)

      webTestClient.get()
        .uri("/reported-adjudications/transfer-reports?status=ADJOURNED&type=IN&page=0&size=20")
        .headers(setHeaders(activeCaseload = "BXI", username = "P_NESS", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
        .exchange()
        .expectStatus().isOk.expectBody()
        .jsonPath("$.content.size()").isEqualTo(1)
        .jsonPath("$.content[0].chargeNumber").isEqualTo(chargeNumberScheduled)
    }
  }

  private fun sendEvent() {
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(
          jsonString(
            HMPPSDomainEvent(
              eventType = PrisonOffenderEventListener.PRISONER_TRANSFER_EVENT_TYPE,
              additionalInformation = AdditionalInformation(
                prisonId = "BXI",
                nomsNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
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
