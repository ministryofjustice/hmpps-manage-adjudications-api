package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate

class PunishmentsIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime(IntegrationTestData.DEFAULT_REPORTED_DATE_TIME)
  }

  @Test
  fun `create punishments `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    val suspendedUntil = LocalDate.of(2023, 3, 27)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                days = 10,
                suspendedUntil = suspendedUntil,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").isEqualTo("2023-03-27")
  }

  @Test
  fun `update punishments - amends one record, removes a record, and creates a record`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    val suspendedUntil = LocalDate.of(2023, 3, 27)
    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                days = 10,
                suspendedUntil = suspendedUntil,
              ),
              PunishmentRequest(
                type = PunishmentType.REMOVAL_ACTIVITY,
                days = 10,
                suspendedUntil = suspendedUntil,
              ),
            ),
        ),
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    val punishmentToAmend = reportedAdjudicationResponse.reportedAdjudication.punishments.first().id!!

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = punishmentToAmend,
                type = PunishmentType.CONFINEMENT,
                days = 15,
                startDate = suspendedUntil,
                endDate = suspendedUntil,
              ),
              PunishmentRequest(
                type = PunishmentType.EXCLUSION_WORK,
                days = 8,
                suspendedUntil = suspendedUntil,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(2)
      .jsonPath("$.reportedAdjudication.punishments[0].id").isEqualTo(punishmentToAmend)
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(15)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo("2023-03-27")
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.endDate").isEqualTo("2023-03-27")
      .jsonPath("$.reportedAdjudication.punishments[1].type").isEqualTo(PunishmentType.EXCLUSION_WORK.name)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.days").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.suspendedUntil").isEqualTo("2023-03-27")
  }
}
