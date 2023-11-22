package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class AdjudicationsDpsEndpointsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `get adjudications summary for offender`() {
    initDataForUnScheduled().createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1)).createChargeProved().createPunishments(
      startDate = LocalDate.now().plusDays(1),
    )

    webTestClient.get()
      .uri("/adjudications/by-booking-id/1")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.awards.size()").isEqualTo(1)
      .jsonPath("$.adjudicationCount").isEqualTo(1)
  }

  @Test
  fun `get adjudications for prisoner`() {
    initDataForUnScheduled().createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1)).createChargeProved().createPunishments(
      startDate = LocalDate.now().plusDays(1),
    )

    webTestClient.get()
      .uri("/adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/adjudications")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.content.size()").isEqualTo(1)
  }
}
