package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrintSupportIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `get dis5 print support`() {
    val scenario = initDataForUnScheduled()

    webTestClient.get()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/print-support/dis5")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
  }
}
