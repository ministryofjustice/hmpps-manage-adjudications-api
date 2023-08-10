package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
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
    migrateRecord(dto)

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
  fun `police prosecution from hearing returns correct referral outcome structure`() {
    val dto = getPoliceProsecutionFromHearing()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
  }

  @Test
  fun `police referral from hearing returns correct referral outcome when another hearing is scheduled`() {
    val dto = getPoliceReferralScheduleNewHearing()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing").exists()
  }

  @Test
  fun `police referral to not proceed returns a schedule hearing rather than not proceed referral outcome`() {
    val dto = getPoliceReferToNotProceed()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing.outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.NOT_PROCEED.name)
  }

  @Test
  fun `multiple refers to prosecution`() {
    val dto = getMultipleRefersToProsecution()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").exists()
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.referralOutcome.code").isEqualTo(OutcomeCode.SCHEDULE_HEARING.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
  }

  @Test
  fun `multiple hearings same outcomes returns correct structure`() {
    val dto = getMultipleResultsSameOutcome()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[0].hearing.outcome.code").isEqualTo(HearingOutcomeCode.ADJOURN.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome").doesNotExist()
      .jsonPath("$.reportedAdjudication.outcomes[1].hearing.outcome.code").isEqualTo(HearingOutcomeCode.ADJOURN.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].hearing.outcome.code").isEqualTo(HearingOutcomeCode.COMPLETE.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
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
    migrateRecord(adjudicationMigrateDto)

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

  private fun migrateRecord(dto: AdjudicationMigrateDto) {
    val body = objectMapper.writeValueAsString(dto)

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null))
      .bodyValue(body)
      .exchange()
      .expectStatus().isCreated
  }

  companion object {
    private val migrateFixtures = MigrateFixtures()

    fun getWithReportedDateTime(reportedDateTime: LocalDateTime): AdjudicationMigrateDto = migrateFixtures.ADULT_WITH_REPORTED_DATE_TIME(
      reportedDateTime = reportedDateTime,
    )

    fun getAdjudicationForReset(): AdjudicationMigrateDto = migrateFixtures.ADULT_SINGLE_OFFENCE

    @JvmStatic
    fun getAllNewAdjudications(): Stream<AdjudicationMigrateDto> = migrateFixtures.getSelection().stream()

    fun getConflictRecord(oicIncidentId: Long) = MigrationEntityBuilder().createAdjudication(oicIncidentId = oicIncidentId)

    fun getPoliceProsecutionFromHearing() = migrateFixtures.HEARING_WITH_PROSCUTION

    fun getPoliceReferralScheduleNewHearing() = migrateFixtures.POLICE_REFERRAL_NEW_HEARING

    fun getMultipleRefersToProsecution() = migrateFixtures.MULITPLE_POLICE_REEER_TO_PROSECUTION

    fun getPoliceReferToNotProceed() = migrateFixtures.POLICE_REF_NOT_PROCEED

    fun getMultipleResultsSameOutcome() = migrateFixtures.WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED
  }
}
