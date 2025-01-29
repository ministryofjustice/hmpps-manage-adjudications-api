package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonOffenderEventListener
import java.time.Instant

@Import(TestOAuth2Config::class)
class PrisonerMergeIntTest : SqsIntegrationTestBase() {

  var prisonerNumber: String? = null

  @BeforeEach
  fun setUp() {
    setAuditTime()
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "MERGE")
    prisonerNumber = testData.prisonerNumber
    initDataForUnScheduled(testData = testData).createHearing()

    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(
          jsonString(
            HMPPSDomainEvent(
              eventType = PrisonOffenderEventListener.PRISONER_MERGE_EVENT_TYPE,
              additionalInformation = AdditionalInformation(
                nomsNumber = "TO",
                removedNomsNumber = testData.prisonerNumber,
                reason = "MERGE",
              ),
              occurredAt = Instant.now(),
              description = "Merge event",
            ),
          ),
        )
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(PrisonOffenderEventListener.PRISONER_MERGE_EVENT_TYPE).build(),
          ),
        )
        .build(),
    )

    Thread.sleep(1000)
  }

  @Test
  fun `prisoner TO should have one report, original prisoner should have 0 reports`() {
    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/TO?status=SCHEDULED&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)

    webTestClient.get()
      .uri("/reported-adjudications/bookings/prisoner/$prisonerNumber!!?status=SCHEDULED&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(0)
  }
}
