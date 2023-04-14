package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate
import java.time.LocalDateTime

class PunishmentsIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `create punishments `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    createPunishments()
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

    val punishmentToAmend = reportedAdjudicationResponse.reportedAdjudication.punishments.first { it.type == PunishmentType.CONFINEMENT }.id

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
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.days").isEqualTo(8)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.suspendedUntil").isEqualTo("2023-03-27")
  }

  @Test
  fun `activate suspended punishment ` () {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()
    initDataForOutcome(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), overrideTestDataSet = IntegrationTestData.ADJUDICATION_2).createChargeProved()

    createPunishments(type = PunishmentType.PROSPECTIVE_DAYS)
      .expectStatus().isCreated

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.ADJUDICATION_2.adjudicationNumber}/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.PROSPECTIVE_DAYS,
                days = 10,
                activatedFrom = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PROSPECTIVE_DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo( IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PROSPECTIVE_DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `get suspended punishments `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved()

    createPunishments()
      .expectStatus().isCreated

    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/suspended")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].reportNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.[0].punishment.type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.[0].punishment.schedule.days").isEqualTo(10)
      .jsonPath("$.[0].punishment.schedule.suspendedUntil").isEqualTo("2023-03-27")
  }

  private fun createPunishments(adjudicationNumber: Long = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
  type: PunishmentType = PunishmentType.CONFINEMENT): WebTestClient.ResponseSpec {
    val suspendedUntil = LocalDate.of(2023, 3, 27)

    return webTestClient.post()
      .uri("/reported-adjudications/${adjudicationNumber}/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = type,
                days = 10,
                suspendedUntil = suspendedUntil,
              ),
            ),
        ),
      )
      .exchange()
  }
}
