package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonOffenderEventListener
import java.time.Instant

@Disabled
class PrisonerMergeIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
    initDataForUnScheduled().createHearing()

    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(
          jsonString(
            HMPPSDomainEvent(
              eventType = PrisonOffenderEventListener.PRISONER_MERGE_EVENT_TYPE,
              additionalInformation = AdditionalInformation(
                nomsNumber = "TO",
                removedNomsNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
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

    Thread.sleep(500)
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
      .uri("/reported-adjudications/bookings/prisoner/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}?status=SCHEDULED&page=0&size=20")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(0)
  }
}
