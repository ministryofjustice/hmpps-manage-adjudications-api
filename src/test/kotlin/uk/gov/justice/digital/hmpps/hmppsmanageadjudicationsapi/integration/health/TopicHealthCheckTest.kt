package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.health

import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.SqsIntegrationTestBase

@Import(TestOAuth2Config::class)
class TopicHealthCheckTest : SqsIntegrationTestBase() {

  @Test
  fun `Outbound topic health ok`() {
    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
      .jsonPath("components.domainevents-health.status").isEqualTo("UP")
      .jsonPath("components.domainevents-health.details.topicArn").isEqualTo(hmppsSqsPropertiesSpy.domaineventsTopicConfig().arn)
      .jsonPath("components.domainevents-health.details.subscriptionsConfirmed").isEqualTo(0)
      .jsonPath("components.domainevents-health.details.subscriptionsPending").isEqualTo(0)
  }
}
