package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MigrateExistingIntTest : SqsIntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime()
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

  @Test
  fun `existing record phase 2 updates status to CHARGE_PROVED and collections updated`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubNomisHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, 100)

    initDataForHearings().createHearing()

    webTestClient.put()
      .uri("/scheduled-tasks/check-nomis-created-hearing-outcomes-for-locking")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk

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
  fun `existing record phase 2 - PROSECUTED`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubNomisHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, 100)

    initDataForHearings().createHearing()

    webTestClient.put()
      .uri("/scheduled-tasks/check-nomis-created-hearing-outcomes-for-locking")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk

    migrateRecord(
      dto = getExistingRecordProsecuted(
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
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.PROSECUTION.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.REFER_POLICE.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.referralOutcome.code").isEqualTo(OutcomeCode.PROSECUTION.name)
  }

  @Test
  fun `existing record CHARGE_PROVED to QUASHED - Phase 2 - simulate two hearing outcomes of NOMIS`() {
    prisonApiMockServer.stubCreateHearing(chargeNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, nextState = "second")
    prisonApiMockServer.stubCreateHearing(chargeNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, currentState = "second", oicHearingId = 101)
    prisonApiMockServer.stubNomisHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, 100)
    prisonApiMockServer.stubNomisHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, 101)

    initDataForHearings().createHearing()

    // note: need to run this, as cant create 2 hearings without results
    webTestClient.put()
      .uri("/scheduled-tasks/check-nomis-created-hearing-outcomes-for-locking")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
    // create our second hearing
    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/hearing/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "locationId" to 1,
          "dateTimeOfHearing" to IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing!!.plusDays(1),
          "oicHearingType" to OicHearingType.GOV.name,
        ),
      )
      .exchange()
      .expectStatus().isCreated

    // update outcome again
    webTestClient.put()
      .uri("/scheduled-tasks/check-nomis-created-hearing-outcomes-for-locking")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk

    migrateRecord(
      dto = getExistingRecordWithTwoNomisOutcomes(
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
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.QUASHED.name)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(3)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[1].outcome.outcome.code").isEqualTo(OutcomeCode.CHARGE_PROVED.name)
      .jsonPath("$.reportedAdjudication.outcomes[2].outcome.outcome.code").isEqualTo(OutcomeCode.QUASHED.name)
  }

  @Test
  fun `existing record amends hearing and adjudicator`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing().createChargeProved()
    val dto = getExistingRecord(
      oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
      prisonerNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
    )
    migrateRecord(dto = dto)

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].dateTimeOfHearing").isEqualTo(
        dto.hearings.first().hearingDateTime.format(
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
        ),
      )
      .jsonPath("$.reportedAdjudication.hearings[0].locationId").isEqualTo(dto.hearings.first().locationId)
      .jsonPath("$.reportedAdjudication.hearings[0].oicHearingType").isEqualTo(dto.hearings.first().oicHearingType.name)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo(dto.hearings.first().adjudicator!!)
  }

  companion object {
    fun getExistingRecord(oicIncidentId: Long, prisonerNumber: String, offenceCode: String = "51:4") = MigrationEntityBuilder().createAdjudication(
      oicIncidentId = oicIncidentId,
      offence = MigrationEntityBuilder().createOffence(offenceCode = offenceCode, offenceDescription = "updated desc"),
      prisoner = MigrationEntityBuilder().createPrisoner(prisonerNumber = prisonerNumber),
      hearings = listOf(
        MigrationEntityBuilder().createHearing(
          oicHearingId = 100,
          hearingResult = MigrationEntityBuilder().createHearingResult(),
        ),
      ),
      punishments = listOf(
        MigrationEntityBuilder().createPunishment(),
      ),
    )

    fun getExistingRecordProsecuted(oicIncidentId: Long, prisonerNumber: String, offenceCode: String = "51:4") = MigrationEntityBuilder().createAdjudication(
      oicIncidentId = oicIncidentId,
      offence = MigrationEntityBuilder().createOffence(offenceCode = offenceCode, offenceDescription = "updated desc"),
      prisoner = MigrationEntityBuilder().createPrisoner(prisonerNumber = prisonerNumber),
      hearings = listOf(
        MigrationEntityBuilder().createHearing(
          oicHearingId = 100,
          hearingResult = MigrationEntityBuilder().createHearingResult(finding = Finding.PROSECUTED.name),
        ),
      ),

    )

    fun getExistingRecordWithTwoNomisOutcomes(oicIncidentId: Long, prisonerNumber: String, offenceCode: String = "51:4") = MigrationEntityBuilder().createAdjudication(
      oicIncidentId = oicIncidentId,
      offence = MigrationEntityBuilder().createOffence(offenceCode = offenceCode, offenceDescription = "updated desc"),
      prisoner = MigrationEntityBuilder().createPrisoner(prisonerNumber = prisonerNumber),
      hearings = listOf(
        MigrationEntityBuilder().createHearing(
          oicHearingId = 100,
          hearingResult = MigrationEntityBuilder().createHearingResult(),
        ),
        MigrationEntityBuilder().createHearing(
          oicHearingId = 101,
          hearingDateTime = LocalDateTime.now().plusDays(1),
          hearingResult = MigrationEntityBuilder().createHearingResult(finding = Finding.QUASHED.name),
        ),
      ),
    )
  }
}
