package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration.IntegrationTestData.Companion.UPDATED_LOCATION_ID

class MigrateResetIntTest : SqsIntegrationTestBase() {
  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `reset migration removes new records`() {
    val adjudicationMigrateDto = MigrateIntTest.getAdjudicationForReset()
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
  fun `reset migration removes existing record updates - phase 1`() {
    initDataForAccept(incDamagesEvidenceWitnesses = false).acceptReport(
      reportNumber = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber,
      activeCaseLoad = IntegrationTestData.DEFAULT_ADJUDICATION.agencyId,
      status = ReportedAdjudicationStatus.ACCEPTED,
    )

    migrateRecord(
      dto = MigrateIntTest.getExistingRecordForReset(
        oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
        prisonerNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
        offenceCode = "51:5",
      ),
    )
    webTestClient.delete()
      .uri("/reported-adjudications/migrate/reset")
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.status").isEqualTo(ReportedAdjudicationStatus.ACCEPTED.name)
      .jsonPath("$.reportedAdjudication.hearings.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.punishmentComments.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.outcomes.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.damages.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.evidence.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.witnesses.size()").isEqualTo(0)
      .jsonPath("$.reportedAdjudication.offenceDetails.offenceCode").isEqualTo(4001)
  }

  @Test
  fun `reset migration - phase 2 resets nomis outcome code`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubNomisHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber, 100)

    initDataForHearings().createHearing()

    webTestClient.put()
      .uri("/scheduled-tasks/check-nomis-created-hearing-outcomes-for-locking")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk

    migrateRecord(
      dto = MigrateExistingIntTest.getExistingRecord(
        oicIncidentId = IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber.toLong(),
        prisonerNumber = IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber,
      ),
    )

    webTestClient.delete()
      .uri("/reported-adjudications/migrate/reset")
      .headers(setHeaders(activeCaseload = null, roles = listOf("ROLE_MIGRATE_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.code").isEqualTo(HearingOutcomeCode.NOMIS.name)
      .jsonPath("$.reportedAdjudication.outcomes[0].outcome").doesNotExist()
  }

  @Disabled("disabled for now, as punishment mapper for sanction seq not implemented for existing yet")
  @Test
  fun `reset migration - phase 3 resets hearings, hearing outcomes and punishments to original data`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber)

    initDataForHearings().createHearing().createChargeProved()
    integrationTestData().createPunishments(testDataSet = IntegrationTestData.DEFAULT_ADJUDICATION).expectStatus().isCreated

    migrateRecord(
      dto = MigrateExistingIntTest.getExistingRecord(
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
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.reportedAdjudication.hearings[0].locationId").isEqualTo(UPDATED_LOCATION_ID)
      .jsonPath("$.reportedAdjudication.hearings[0].outcome.adjudicator").isEqualTo("test")
  }
}
