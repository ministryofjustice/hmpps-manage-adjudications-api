package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder
import java.time.LocalDateTime
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
      .jsonPath("$.chargeNumberMapping.chargeNumber").isEqualTo("${adjudicationMigrateDto.oicIncidentId}-${adjudicationMigrateDto.offenceSequence}")

    webTestClient.get()
      .uri("/reported-adjudications/${adjudicationMigrateDto.oicIncidentId}-${adjudicationMigrateDto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphNumber").isEqualTo(adjudicationMigrateDto.offence.offenceCode)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphDescription").isEqualTo(adjudicationMigrateDto.offence.offenceDescription)
  }

  @Test
  fun `with reported date time and reporting officer overrides audit`() {
    val reportedDateTime = LocalDateTime.of(2017, 10, 12, 10, 0)

    val dto = getWithReportedDateTime(reportedDateTime)
    val body = objectMapper.writeValueAsString(dto)

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null))
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.createdDateTime").isEqualTo("2017-10-12T10:00:00")
      .jsonPath("$.reportedAdjudication.createdByUserId").isEqualTo("OFFICER_RO")
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
      .uri("/reported-adjudications/${adjudicationMigrateDto.oicIncidentId}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
  }

  companion object {
    private val migrateFixtures = MigrateFixtures()

    fun getWithReportedDateTime(reportedDateTime: LocalDateTime): AdjudicationMigrateDto = migrateFixtures.ADULT_WITH_REPORTED_DATE_TIME(
      reportedDateTime = reportedDateTime,
    )

    fun getAdjudicationForReset(): AdjudicationMigrateDto = migrateFixtures.ADULT_SINGLE_OFFENCE

    @JvmStatic
    fun getAllNewAdjudications(): Stream<AdjudicationMigrateDto> = migrateFixtures.getAll().stream()

    fun getConflictRecord(oicIncidentId: Long) = MigrationEntityBuilder().createAdjudication(oicIncidentId = oicIncidentId)
  }
}
