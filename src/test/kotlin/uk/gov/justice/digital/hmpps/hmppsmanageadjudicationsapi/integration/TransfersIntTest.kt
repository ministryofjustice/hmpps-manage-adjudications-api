package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.PrisonOffenderEventListener
import java.time.Instant

class TransfersIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
    initDataForHearings()

    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(
          jsonString(
            HMPPSDomainEvent(
              eventType = PrisonOffenderEventListener.PRISONER_TRANSFER_EVENT_TYPE,
              additionalInformation = AdditionalInformation(
                prisonId = "TJW",
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

  @CsvSource("TJW, true", "XXX, false")
  @ParameterizedTest
  fun `test access for {0} is {1} `(agencyId: String, allowed: Boolean) {
    val response = webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders(activeCaseload = agencyId))
      .exchange()

    when (allowed) {
      true -> response.expectStatus().isOk.expectBody().jsonPath("$.reportedAdjudication.overrideAgencyId").isEqualTo(agencyId)
      false -> response.expectStatus().isNotFound
    }
  }
}
