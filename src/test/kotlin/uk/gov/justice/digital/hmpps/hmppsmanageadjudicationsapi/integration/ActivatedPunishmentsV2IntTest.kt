package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import java.time.LocalDate

@ActiveProfiles("test-v2")
class ActivatedPunishmentsV2IntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `activate a suspended punishment with create`() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false, activatedFrom = activatedFrom.first, id = activatedFrom.second)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    confirmPunishmentIsActivated(
      chargeNumber = activatedFrom.first,
      activatedByChargeNumber = scenario.getGeneratedChargeNumber(),
    )
  }

  @Test
  fun `activate a suspended punishment with update`() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(1)

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = activatedFrom.second,
                type = PunishmentType.CONFINEMENT,
                days = 15,
                activatedFrom = activatedFrom.first,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(10),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    confirmPunishmentIsActivated(
      chargeNumber = activatedFrom.first,
      activatedByChargeNumber = scenario.getGeneratedChargeNumber(),
    )
  }

  @Test
  fun `deactivate a suspended punishment`() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false, activatedFrom = activatedFrom.first, id = activatedFrom.second)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    confirmPunishmentIsActivated(
      chargeNumber = activatedFrom.first,
      activatedByChargeNumber = scenario.getGeneratedChargeNumber(),
    )
    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.EXCLUSION_WORK,
                days = 15,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(10),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk

    confirmPunishmentIsDeActivated(
      chargeNumber = activatedFrom.first,
    )
  }

  @Test
  fun `remove charge proved outcome and restore activated suspended punishment to original state`() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false, activatedFrom = activatedFrom.first, id = activatedFrom.second)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    confirmPunishmentIsActivated(
      chargeNumber = activatedFrom.first,
      activatedByChargeNumber = scenario.getGeneratedChargeNumber(),
    )
    webTestClient.delete()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/remove-completed-hearing")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk

    confirmPunishmentIsDeActivated(
      chargeNumber = activatedFrom.first,
    )
  }

  @Test
  fun `quashed deactivates an activated punishment `() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false, activatedFrom = activatedFrom.first, id = activatedFrom.second)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    confirmPunishmentIsActivated(
      chargeNumber = activatedFrom.first,
      activatedByChargeNumber = scenario.getGeneratedChargeNumber(),
    )

    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/outcome/quashed")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "reason" to QuashedReason.APPEAL_UPHELD,
          "details" to "details",
        ),
      )
      .exchange()
      .expectStatus().isCreated

    confirmPunishmentIsDeActivated(
      chargeNumber = activatedFrom.first,
    )
  }

  @Test
  fun `get reported adjudication with activated punishments merged into charge punishments`() {
    val activatedFrom = createSuspendedPunishmentCharge()

    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false, activatedFrom = activatedFrom.first, id = activatedFrom.second)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(0)

    webTestClient.get()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/v2?includeActivated=true")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo(activatedFrom.first)
  }

  private fun createSuspendedPunishmentCharge(): Pair<String, Long> {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    val punishmentId = createPunishments(scenario.getGeneratedChargeNumber())
      .expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication.punishments.first().id

    return Pair(scenario.getGeneratedChargeNumber(), punishmentId!!)
  }

  private fun confirmPunishmentIsActivated(chargeNumber: String, activatedByChargeNumber: String) {
    webTestClient.get()
      .uri("/reported-adjudications/$chargeNumber/v2?includeActivated=true")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").doesNotExist()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").exists()
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(activatedByChargeNumber)
  }

  private fun confirmPunishmentIsDeActivated(chargeNumber: String) {
    webTestClient.get()
      .uri("/reported-adjudications/$chargeNumber/v2?includeActivated=true")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").exists()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").doesNotExist()
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").doesNotExist()
  }
}
