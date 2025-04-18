package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config

@Import(TestOAuth2Config::class)
class PrintSupportIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `get dis5 print support`() {
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "ZZ12345", offenderBookingId = 999999)
    val firstCharge = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()
    val scenario2 = initDataForUnScheduled(testData = testData)
    val updatedTestDataSet = testData.also {
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
    val testData = IntegrationTestData.getDefaultAdjudication(prisonerNumber = "ZZ56789", offenderBookingId = 111111)
    val firstCharge = initDataForUnScheduled(testData = testData).getGeneratedChargeNumber()

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
