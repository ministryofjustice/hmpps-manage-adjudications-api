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
    val firstCharge = initDataForUnScheduled().getGeneratedChargeNumber()
    val scenario2 = initDataForUnScheduled()
    val updatedTestDataSet = IntegrationTestData.DEFAULT_ADJUDICATION.also {
      it.chargeNumber = scenario2.getGeneratedChargeNumber()
    }

    scenario2.createHearing(overrideTestDataSet = updatedTestDataSet)
      .createChargeProved(overrideTestDataSet = updatedTestDataSet)
      .createPunishments(overrideTestDataSet = updatedTestDataSet)

    webTestClient.get()
      .uri("/reported-adjudications/$firstCharge/print-support/dis5")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.chargeNumber").isEqualTo(firstCharge)
      .jsonPath("$.previousAtCurrentEstablishmentCount").isEqualTo(1)
      .jsonPath("$.previousCount").isEqualTo(1)
      .jsonPath("$.sameOffenceCount").isEqualTo(1)
      .jsonPath("$.lastReportedOffence.chargeNumber").isEqualTo(scenario2.getGeneratedChargeNumber())
      .jsonPath("$.lastReportedOffence.punishments.size()").isEqualTo(1)
  }

  @Test
  fun `get dis5 print support with no last reported charge`() {
    val firstCharge = initDataForUnScheduled().getGeneratedChargeNumber()

    webTestClient.get()
      .uri("/reported-adjudications/$firstCharge/print-support/dis5")
      .headers(setHeaders(username = "P_NESS"))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.chargeNumber").isEqualTo(firstCharge)
      .jsonPath("$.previousAtCurrentEstablishmentCount").isEqualTo(0)
      .jsonPath("$.previousCount").isEqualTo(0)
      .jsonPath("$.sameOffenceCount").isEqualTo(0)
      .jsonPath("$.lastReportedOffence").doesNotExist()
  }
}
