package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures

class MigrateIntTest : SqsIntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  private val migrateFixtures = MigrateFixtures()

  @Test
  fun `migrate`() {
    migrateFixtures.getAll().forEach {
        adjudicationMigrateDto ->
      val body = objectMapper.writeValueAsString(adjudicationMigrateDto)
      webTestClient.post()
        .uri("/reported-adjudications/migrate")
        .headers(setHeaders())
        .bodyValue(body)
        .exchange()
        .expectStatus().isCreated
        .expectBody()
        .jsonPath("$.chargeNumber").exists()
    }
  }
}
