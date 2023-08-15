package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder

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
  fun `existing record phase 2 updates status to CHARGE_PROVED and collections updated` () {
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
  fun `existing record CHARGE_PROVED to QUASHED - Phase 2` () {
    initDataForHearings().createHearing()

  }


  companion object {
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
  }


}