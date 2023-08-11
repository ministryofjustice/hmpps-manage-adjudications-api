package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder
import java.time.LocalDateTime
import java.util.stream.Stream

@ActiveProfiles("sync")
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
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
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

  @Disabled("this can only be turned on once v1 endpoints removed, due to change number.toLong")
  @Test
  fun `status is correct - SCHEDULED when UNSCHEDULED migrated record is updated via UI`() {
    val dto = getAdjudicationForReset()
    migrateRecord(dto)

    webTestClient.get()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED.name)

    webTestClient.post()
      .uri("/reported-adjudications/${dto.oicIncidentId}-${dto.offenceSequence}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to LocalDateTime.now().plusDays(1),
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.SCHEDULED.name)
  }

  @Test
  fun `reset migration removes records`() {
    val adjudicationMigrateDto = getAdjudicationForReset()
    migrateRecord(adjudicationMigrateDto)

    webTestClient.delete()
      .uri("/reported-adjudications/migrate/reset")
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk

    webTestClient.get()
      .uri("/reported-adjudications/${adjudicationMigrateDto.oicIncidentId}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isNotFound
  }

  @Test
  fun `existing record phase 1 updates status to CHARGE_PROVED and collections updated`() {
    initDataForAccept().acceptReport(
      reportNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber,
      activeCaseLoad = IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      status = ReportedAdjudicationStatus.ACCEPTED,
    )

    migrateRecord(
      dto = getExistingRecord(
        oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
        prisonerNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      ),
    )

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceCode").isEqualTo(4001)
      .jsonPath("$.reportedAdjudication.damages.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.evidence.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.witnesses.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
  }

  @Test
  fun `existing record conflict exception`() {
    initDataForAccept().acceptReport(
      reportNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber,
      activeCaseLoad = IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      status = ReportedAdjudicationStatus.ACCEPTED,
    )

    val body = objectMapper.writeValueAsString(
      getExistingRecord(
        oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
        prisonerNumber = "XYZ",
      ),
    )

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
      .bodyValue(body)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.CONFLICT)
  }

  @Test
  fun `existing record offence code has changed`() {
    initDataForAccept().acceptReport(
      reportNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber,
      activeCaseLoad = IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      status = ReportedAdjudicationStatus.ACCEPTED,
    )

    migrateRecord(
      dto = getExistingRecord(
        oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
        prisonerNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
        offenceCode = "51:5",
      ),
    )

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceCode").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphNumber").isEqualTo("51:5")
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceRule.paragraphDescription").isEqualTo("updated desc")
  }

  private fun migrateRecord(dto: AdjudicationMigrateDto) {
    val body = objectMapper.writeValueAsString(dto)

    webTestClient.post()
      .uri("/reported-adjudications/migrate")
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
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

    fun getExistingRecord(oicIncidentId: Long, prisonerNumber: String, offenceCode: String = "51:4") = MigrationEntityBuilder().createAdjudication(
      oicIncidentId = oicIncidentId,
      offence = MigrationEntityBuilder().createOffence(offenceCode = offenceCode, offenceDescription = "updated desc"),
      prisoner = MigrationEntityBuilder().createPrisoner(prisonerNumber = prisonerNumber),
      hearings = listOf(
        MigrationEntityBuilder().createHearing(
          hearingResult = MigrationEntityBuilder().createHearingResult(),
        ),
      ),
      punishments = listOf(
        MigrationEntityBuilder().createPunishment(),
      ),
    )

    fun getPoliceProsecutionFromHearing() = migrateFixtures.HEARING_WITH_PROSECUTION

    fun getPoliceReferralScheduleNewHearing() = migrateFixtures.POLICE_REFERRAL_NEW_HEARING

    fun getMultipleRefersToProsecution() = migrateFixtures.MULITPLE_POLICE_REFER_TO_PROSECUTION

    fun getPoliceReferToNotProceed() = migrateFixtures.POLICE_REF_NOT_PROCEED

    fun getMultipleResultsSameOutcome() = migrateFixtures.WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED
  }
}
