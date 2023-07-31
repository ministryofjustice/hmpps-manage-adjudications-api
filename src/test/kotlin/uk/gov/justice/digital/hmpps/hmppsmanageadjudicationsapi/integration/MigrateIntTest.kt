package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures
import java.util.stream.Stream

class MigrateIntTest : SqsIntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @ParameterizedTest
  @MethodSource("getAllAdjudications")
  fun `migrate all the new records`(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val body = objectMapper.writeValueAsString(adjudicationMigrateDto)
    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders())
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.chargeNumberMapping.chargeNumber").exists()
  }

  companion object {
    private val migrateFixtures = MigrateFixtures()

    @JvmStatic
    fun getAllAdjudications(): Stream<AdjudicationMigrateDto> = migrateFixtures.getAll().stream()
  }
}
