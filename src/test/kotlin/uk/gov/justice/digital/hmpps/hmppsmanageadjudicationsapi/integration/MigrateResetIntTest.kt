package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

@ActiveProfiles("sync")
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
}
