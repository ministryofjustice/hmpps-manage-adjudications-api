package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequestV2
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PunishmentsIntTest : SqsIntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    setAuditTime()
  }

  @Deprecated("to remove on completion of NN-5319")
  @Test
  fun `create punishments `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved(caution = false)

    createPunishments()
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
  fun `create punishments v2`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProvedV2()

    createPunishmentsV2()
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

  @Deprecated("to remove on completion of NN-5319")
  @Test
  fun `create punishments - additional days `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved(caution = false)

    createPunishments(type = PunishmentType.ADDITIONAL_DAYS, consecutiveReportNumber = 9999)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.ADDITIONAL_DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].consecutiveReportNumber").isEqualTo(9999)
  }

  @Test
  fun `create punishments - additional days v2`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProvedV2()

    createPunishmentsV2(type = PunishmentType.ADDITIONAL_DAYS, consecutiveReportNumber = 9999)
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.ADDITIONAL_DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].consecutiveReportNumber").isEqualTo(9999)
  }

  @Test
  fun `update punishments - amends one record, removes a record, and creates a record`() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubUpdateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    initDataForOutcome().createHearing().createChargeProved(caution = false)

    val suspendedUntil = LocalDate.now().plusMonths(1)
    val formattedDate = suspendedUntil.format(DateTimeFormatter.ISO_DATE)
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
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo(formattedDate)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.endDate").isEqualTo(formattedDate)
      .jsonPath("$.reportedAdjudication.punishments[1].type").isEqualTo(PunishmentType.EXCLUSION_WORK.name)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.days").isEqualTo(8)
      .jsonPath("$.reportedAdjudication.punishments[1].schedule.suspendedUntil").isEqualTo(formattedDate)
  }

  @Test
  fun `activate suspended punishment on create `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)

    initDataForOutcome().createHearing().createChargeProved(caution = false)
    initDataForOutcome(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)
      .createChargeProved(overrideTestDataSet = IntegrationTestData.ADJUDICATION_2, caution = false)

    val result = createPunishments(type = PunishmentType.REMOVAL_WING)
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.ADJUDICATION_2.adjudicationNumber}/punishments")
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
                activatedFrom = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `activate suspended punishment on update `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubUpdateSanctions(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)

    initDataForOutcome().createHearing().createChargeProved(caution = false)
    initDataForOutcome(adjudication = IntegrationTestData.ADJUDICATION_2).createHearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), overrideTestDataSet = IntegrationTestData.ADJUDICATION_2)
      .createChargeProved(overrideTestDataSet = IntegrationTestData.ADJUDICATION_2, caution = false)

    val result = createPunishments(type = PunishmentType.REMOVAL_WING)
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!

    createPunishments(adjudicationNumber = IntegrationTestData.ADJUDICATION_2.adjudicationNumber)

    webTestClient.put()
      .uri("/reported-adjudications/${IntegrationTestData.ADJUDICATION_2.adjudicationNumber}/punishments")
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
                activatedFrom = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(1)
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedFrom").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    webTestClient.get()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}")
      .headers(setHeaders())
      .exchange()
      .expectStatus().is2xxSuccessful
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.REMOVAL_WING.name)
      .jsonPath("$.reportedAdjudication.punishments[0].activatedBy").isEqualTo(IntegrationTestData.ADJUDICATION_2.adjudicationNumber)
  }

  @Test
  fun `get suspended punishments `() {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    initDataForOutcome().createHearing().createChargeProved(caution = false)

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
      .jsonPath("$.[0].punishment.schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @CsvSource("ADDITIONAL_DAYS", "PROSPECTIVE_DAYS")
  @ParameterizedTest
  fun `get additional days punishments `(punishmentType: PunishmentType) {
    prisonApiMockServer.stubCreateHearing(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateHearingResult(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanction(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
    prisonApiMockServer.stubCreateSanctions(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)

    initDataForOutcome().createHearing(oicHearingType = OicHearingType.INAD_ADULT).createChargeProved(caution = false)

    webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments")
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
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/for-consecutive?type=$punishmentType")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(1)
      .jsonPath("$.[0].reportNumber").isEqualTo(IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber)
      .jsonPath("$.[0].punishment.type").isEqualTo(punishmentType.name)
      .jsonPath("$.[0].punishment.schedule.days").isEqualTo(10)
      .jsonPath("$.[0].chargeProvedDate").isEqualTo("2010-11-19")
  }

  @Test
  fun `create punishment comment `() {
    initDataForOutcome()

    createPunishmentComment()
      .expectStatus().isCreated
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishmentComments[0].comment").isEqualTo("some text")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].createdByUserId").isEqualTo("ITAG_ALO")
      .jsonPath("$.reportedAdjudication.punishmentComments[0].dateTime").value<String> { assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE)) }
  }

  @Test
  fun `update punishment comment `() {
    initDataForOutcome()

    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments/comment")
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
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments/comment")
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
    initDataForOutcome()

    val reportedAdjudicationResponse = webTestClient.post()
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments/comment")
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
      .uri("/reported-adjudications/${IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber}/punishments/comment/$punishmentCommentToAmend")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishmentComments.size()").isEqualTo(0)
  }

  @Deprecated("to remove on completion of NN-5319")
  private fun createPunishments(
    adjudicationNumber: Long = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
    type: PunishmentType = PunishmentType.CONFINEMENT,
    consecutiveReportNumber: Long? = null,
  ): WebTestClient.ResponseSpec {
    val suspendedUntil = LocalDate.now().plusMonths(1)

    return webTestClient.post()
      .uri("/reported-adjudications/$adjudicationNumber/punishments")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = type,
                days = 10,
                suspendedUntil = suspendedUntil,
                consecutiveReportNumber = consecutiveReportNumber,
              ),
            ),
        ),
      )
      .exchange()
  }

  private fun createPunishmentsV2(
    adjudicationNumber: Long = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
    type: PunishmentType = PunishmentType.CONFINEMENT,
    consecutiveReportNumber: Long? = null,
  ): WebTestClient.ResponseSpec {
    val suspendedUntil = LocalDate.now().plusMonths(1)

    return webTestClient.post()
      .uri("/reported-adjudications/$adjudicationNumber/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequestV2(
                type = type,
                days = 10,
                suspendedUntil = suspendedUntil,
                consecutiveReportNumber = consecutiveReportNumber,
              ),
            ),
        ),
      )
      .exchange()
  }

  private fun createPunishmentComment(
    adjudicationNumber: Long = IntegrationTestData.DEFAULT_ADJUDICATION.adjudicationNumber,
  ): WebTestClient.ResponseSpec {
    return webTestClient.post()
      .uri("/reported-adjudications/$adjudicationNumber/punishments/comment")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        PunishmentCommentRequest(
          comment = "some text",
        ),
      )
      .exchange()
  }
}
