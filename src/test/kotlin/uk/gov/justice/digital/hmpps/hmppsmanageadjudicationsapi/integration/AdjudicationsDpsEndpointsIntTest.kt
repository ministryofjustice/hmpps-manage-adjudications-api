package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import java.time.LocalDate
import java.time.LocalDateTime

@Import(TestOAuth2Config::class)
class AdjudicationsDpsEndpointsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `get adjudications summary for offender`() {
    val testData = IntegrationTestData.getDefaultAdjudication(offenderBookingId = 1000)
    initDataForUnScheduled(testData = testData).createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1)).createChargeProved().createPunishments(
      startDate = LocalDate.now().plusDays(1),
    )

    webTestClient.get()
      .uri("/adjudications/by-booking-id/1000")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.awards.size()").isEqualTo(1)
      .jsonPath("$.adjudicationCount").isEqualTo(1)
  }
}
