package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PunishmentsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Test
  fun `create punishments v2`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createPunishments(scenario.getGeneratedChargeNumber())
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @Test
  fun `create punishments - additional days v2`() {
    val scenario = initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), type = PunishmentType.ADDITIONAL_DAYS, consecutiveChargeNumber = "9999")
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.ADDITIONAL_DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].consecutiveChargeNumber").isEqualTo(9999)
  }

  @Test
  fun `update punishments - amends one record, removes a record, and creates a record v2`() {
    initDataForUnScheduled().createHearing().createChargeProved()

    val suspendedUntil = LocalDate.now().plusMonths(1)
    val formattedDate = suspendedUntil.format(DateTimeFormatter.ISO_DATE)
    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/v2")
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
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/v2")
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
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo(formattedDate)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.endDate").isEqualTo(formattedDate)
      .jsonPath("$.reportedAdjudication.punishments[1].type").isEqualTo(PunishmentType.EXCLUSION_WORK.name)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.days").isEqualTo(8)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.suspendedUntil").isEqualTo(formattedDate)
  }

  @Test
  fun `activate suspended punishment on create `() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val scenario2 = initDataForUnScheduled(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(oicHearingType = OicHearingType.INAD_YOI, dateTimeOfHearing = LocalDateTime.now().plusDays(1), overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)
      .createChargeProved(overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)

    val result = createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), type = PunishmentType.REMOVAL_WING)
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    webTestClient.post()
      .uri("/reported-adjudications/${scenario2.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = result.reportedAdjudication.punishments.first().id!!,
                type = PunishmentType.REMOVAL_WING,
                days = 10,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(5),
                activatedFrom = scenario.getGeneratedChargeNumber(),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo(scenario.getGeneratedChargeNumber())

    webTestClient.get()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(scenario2.getGeneratedChargeNumber())
  }

  @Test
  fun `activate suspended punishment on update `() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val scenario2 = initDataForUnScheduled(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(oicHearingType = OicHearingType.INAD_YOI, dateTimeOfHearing = LocalDateTime.now().plusDays(1), overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)
      .createChargeProved(overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)

    val result = createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), type = PunishmentType.REMOVAL_WING)
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    createPunishments(chargeNumber = scenario2.getGeneratedChargeNumber())

    webTestClient.put()
      .uri("/reported-adjudications/${scenario2.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = result.reportedAdjudication.punishments.first().id!!,
                type = PunishmentType.REMOVAL_WING,
                days = 10,
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(5),
                activatedFrom = scenario.getGeneratedChargeNumber(),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo(scenario.getGeneratedChargeNumber())

    webTestClient.get()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(scenario2.getGeneratedChargeNumber())
  }

  @Test
  fun `get suspended punishments `() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    initDataForUnScheduled(IntegrationTestData.DEFAULT_ADJUDICATION_OVERRIDE).createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber())
      .expectStatus().isCreated

    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/suspended/v2?chargeNumber=${IntegrationTestData.DEFAULT_ADJUDICATION_OVERRIDE.chargeNumber}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].chargeNumber").isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.[0].punishment.type").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.[0].punishment.schedule.days").isEqualTo(10)
      .jsonPath("$.[0].punishment.schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @CsvSource("ADDITIONAL_DAYS", "PROSPECTIVE_DAYS")
  @ParameterizedTest
  fun `get additional days punishments `(punishmentType: PunishmentType) {
    val scenario = initDataForUnScheduled().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved()
    initDataForUnScheduled(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(oicHearingType = OicHearingType.INAD_YOI, dateTimeOfHearing = IntegrationTestData.DEFAULT_ADJUDICATION.dateTimeOfHearing, overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = punishmentType,
                days = 10,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated

    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/for-consecutive?type=$punishmentType&chargeNumber=${IntegrationTestData.ADJUDICATION_2.chargeNumber}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].chargeNumber").isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.[0].punishment.type").isEqualTo(punishmentType.name)
      .jsonPath("$.[0].punishment.schedule.days").isEqualTo(10)
      .jsonPath("$.[0].chargeProvedDate").isEqualTo("2010-11-19")
  }

  @Test
  fun `get active punishments for offender booking id`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    initDataForUnScheduled(IntegrationTestData.DEFAULT_ADJUDICATION_OVERRIDE).createHearing().createChargeProved()

    createPunishments(chargeNumber = scenario.getGeneratedChargeNumber(), isSuspended = false)
      .expectStatus().isCreated

    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.offenderBookingId}/active")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_VIEW_ADJUDICATIONS")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].chargeNumber").isEqualTo(scenario.getGeneratedChargeNumber())
      .jsonPath("$.[0].punishmentType").isEqualTo(PunishmentType.CONFINEMENT.name)
      .jsonPath("$.[0].days").isEqualTo(10)
      .jsonPath("$.[0].startDate").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @Test
  fun `create punishment comment `() {
    val scenario = initDataForUnScheduled()

    createPunishmentComment(chargeNumber = scenario.getGeneratedChargeNumber())
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishmentComments[0].comment").isEqualTo("some text")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].reasonForChange").isEqualTo(ReasonForChange.APPEAL.name)
      .jsonPath("$.reportedAdjudication.punishmentComments[0].createdByUserId").isEqualTo("ITAG_ALO")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].dateTime").value<String> { assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)) }
  }

  @Test
  fun `update punishment comment `() {
    initDataForUnScheduled()

    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/comment")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        PunishmentCommentRequest(
          comment = "some text",
        ),
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    val punishmentCommentToAmend = reportedAdjudicationResponse.reportedAdjudication.punishmentComments[0].id

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/comment")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        PunishmentCommentRequest(id = punishmentCommentToAmend, comment = "new text"),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishmentComments[0].comment").isEqualTo("new text")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].createdByUserId").isEqualTo("ITAG_ALO")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].dateTime").value<String> { assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)) }
  }

  @Test
  fun `delete punishment comment `() {
    initDataForUnScheduled()

    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/comment")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        PunishmentCommentRequest(
          comment = "some text",
        ),
      )
      .exchange()
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    val punishmentCommentToAmend = reportedAdjudicationResponse.reportedAdjudication.punishmentComments[0].id

    webTestClient.delete()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/comment/$punishmentCommentToAmend")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishmentComments.size()").isEqualTo(0)
  }

  @Test
  fun `activate a suspended punishment, remove it and ensure the punishment is now available in suspended punishments list`() {
    val dummyCharge = initDataForUnScheduled().getGeneratedChargeNumber()
    val suspended = initDataForUnScheduled().createHearing().createChargeProved().getGeneratedChargeNumber()

    createPunishments(suspended).expectStatus().isCreated

    getSuspendedPunishments(chargeNumber = dummyCharge).expectStatus().isOk
      .expectBody().jsonPath("$.size()").isEqualTo(1)

    val activated = initDataForUnScheduled().createHearing().createChargeProved().getGeneratedChargeNumber()

    createPunishments(chargeNumber = activated, activatedFrom = suspended, isSuspended = false, id = 1).expectStatus().isCreated

    getSuspendedPunishments(chargeNumber = activated).expectStatus().isOk
      .expectBody().jsonPath("$.size()").isEqualTo(0)

    // now update it and remove the activated record.
    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.chargeNumber}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to emptyList<PunishmentRequest>(),
        ),
      )
      .exchange().expectStatus().isOk

    getSuspendedPunishments(chargeNumber = activated).expectStatus().isOk
      .expectBody().jsonPath("$.size()").isEqualTo(1)
  }

  fun createPunishments(
    chargeNumber: String,
    type: PunishmentType = PunishmentType.CONFINEMENT,
    consecutiveChargeNumber: String? = null,
    isSuspended: Boolean = true,
    activatedFrom: String? = null,
    id: Long? = null,
  ): WebTestClient.ResponseSpec {
    val suspendedUntil = LocalDate.now().plusMonths(1)

    return webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = if (activatedFrom.isNullOrBlank()) null else id,
                type = type,
                days = 10,
                suspendedUntil = if (isSuspended) suspendedUntil else null,
                startDate = if (isSuspended) null else suspendedUntil,
                endDate = if (isSuspended) null else suspendedUntil.plusDays(10),
                consecutiveChargeNumber = consecutiveChargeNumber,
                activatedFrom = activatedFrom,
              ),
            ),
        ),
      )
      .exchange()
  }

  private fun createPunishmentComment(
    chargeNumber: String,
  ): WebTestClient.ResponseSpec {
    return webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/comment")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        PunishmentCommentRequest(
          comment = "some text",
          reasonForChange = ReasonForChange.APPEAL,
        ),
      )
      .exchange()
  }

  private fun getSuspendedPunishments(chargeNumber: String): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/suspended/v2?chargeNumber=$chargeNumber")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
}
