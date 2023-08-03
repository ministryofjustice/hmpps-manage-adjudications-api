package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder
import java.util.stream.Stream

class MigrateIntTest : SqsIntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @ParameterizedTest
  @MethodSource("getAllNewAdjudications")
  fun `migrate all the new records`(adjudicationMigrateDto: AdjudicationMigrateDto) {
    val body = objectMapper.writeValueAsString(adjudicationMigrateDto)
    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null))
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.chargeNumberMapping.chargeNumber").exists()
  }

  @Test
  fun `migrate existing record throws custom exception`() {
    val body = objectMapper.writeValueAsString(
      getConflictRecord(oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong()),
    )

    initDataForHearings()

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null))
      .bodyValue(body)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `reset migration removes records`() {
    val adjudicationMigrateDto = getAdjudicationForReset()

    val body = objectMapper.writeValueAsString(adjudicationMigrateDto)

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null))
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated

    webTestClient.delete()
      .uri("/reported-adjudications/migrate/reset")
      .headers(setHeaders(activeCaseload = null))
      .exchange()
      .expectStatus().isOk

    webTestClient.get()
      .uri("/reported-adjudications/${adjudicationMigrateDto.oicIncidentId}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private val migrateFixtures = MigrateFixtures()

    fun getAdjudicationForReset(): AdjudicationMigrateDto = migrateFixtures.ADULT_SINGLE_OFFENCE

    @JvmStatic
    fun getAllNewAdjudications(): Stream<AdjudicationMigrateDto> = migrateFixtures.getAll().stream()

    fun getConflictRecord(oicIncidentId: Long) = MigrationEntityBuilder().createAdjudication(oicIncidentId = oicIncidentId)
  }
}
