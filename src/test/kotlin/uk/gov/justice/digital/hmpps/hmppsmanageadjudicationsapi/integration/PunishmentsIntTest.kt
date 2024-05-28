package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.CompleteRehabilitativeActivityRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.RehabilitativeActivityRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotCompletedOutcome
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
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.duration").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.measurement").isEqualTo(Measurement.DAYS.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @CsvSource("true", "false")
  @ParameterizedTest
  fun `create a payback punishment`(hasDetails: Boolean) {
    val hearingDate = LocalDate.of(2024, 1, 1)
    val scenario = initDataForUnScheduled().createHearing(dateTimeOfHearing = hearingDate.atStartOfDay()).createChargeProved()

    val body = webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = null,
                type = PunishmentType.PAYBACK,
                suspendedUntil = null,
                startDate = null,
                endDate = if (hasDetails) LocalDate.now().plusDays(10) else null,
                duration = if (hasDetails) 10 else null,
                paybackNotes = if (hasDetails) "some payback notes" else null,
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .expectBody()

    if (hasDetails) {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PAYBACK.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.duration").isEqualTo(10)
        .jsonPath("$.reportedAdjudication.punishments[0].paybackNotes").isEqualTo("some payback notes")
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.measurement").isEqualTo(Measurement.HOURS.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo("2024-01-01")
    } else {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PAYBACK.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo("2024-01-01")
    }
  }

  @CsvSource("true", "false")
  @ParameterizedTest
  fun `create a rehabilitative activity punishment`(hasDetails: Boolean) {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    val rehabRequest = if (hasDetails) RehabilitativeActivityRequest(details = "some details", monitor = "monitor", endDate = LocalDate.now().plusDays(10)) else RehabilitativeActivityRequest()

    val body = webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now(),
                duration = 10,
                rehabilitativeActivities = listOf(rehabRequest),
              ),
            ),
        ),
      )
      .exchange()
      .expectBody()
    if (hasDetails) {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
        .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].details")
        .isEqualTo("some details")
        .jsonPath("$.reportedAdjudication.punishments[0].canEdit")
        .isEqualTo(false)
        .jsonPath("$.reportedAdjudication.punishments[0].canRemove")
        .isEqualTo(true)
        .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].monitor")
        .isEqualTo("monitor")
        .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].endDate")
        .exists()
    } else {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
        .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0]").exists()
    }
  }

  @Test
  fun `update a rehabilitative activity`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now(),
                duration = 10,
                rehabilitativeActivities = listOf(
                  RehabilitativeActivityRequest(
                    details = "details",
                    monitor = "monitor",
                    endDate = LocalDate.now().plusDays(10),
                    totalSessions = 4,
                  ),
                ),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].details").isEqualTo("details")
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].monitor").isEqualTo("monitor")
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].endDate").exists()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities[0].totalSessions").isEqualTo(4)
  }

  @Test
  fun `delete a rehabilitative activity`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()

    createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now(),
                duration = 10,
                rehabilitativeActivities = emptyList(),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivities.size()").isEqualTo(0)
  }

  @Test
  fun `completes a rehabilitative activity successfully`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val punishmentId = createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    completeRehabilitativeActivity(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      punishmentId = punishmentId,
      completed = true,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesCompleted").isEqualTo(true)
      .jsonPath("$.reportedAdjudication.punishments[0].canEdit").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].canRemove").isEqualTo(false)
  }

  @Test
  fun `completes a rehabilitative activity - no action`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val punishmentId = createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    completeRehabilitativeActivity(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      punishmentId = punishmentId,
      completed = false,
      notCompletedOutcome = NotCompletedOutcome.NO_ACTION,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesCompleted").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesNotCompletedOutcome").isEqualTo(NotCompletedOutcome.NO_ACTION.name)
      .jsonPath("$.reportedAdjudication.punishments[0].canEdit").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].canRemove").isEqualTo(false)
  }

  @Test
  fun `completes a rehabilitative activity - full activation`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val punishmentId = createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    completeRehabilitativeActivity(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      punishmentId = punishmentId,
      completed = false,
      notCompletedOutcome = NotCompletedOutcome.FULL_ACTIVATE,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesCompleted").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesNotCompletedOutcome").isEqualTo(NotCompletedOutcome.FULL_ACTIVATE.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").doesNotExist()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(10)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.reportedAdjudication.punishments[0].canEdit").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].canRemove").isEqualTo(false)
  }

  @Test
  fun `completes a rehabilitative activity - partial activation`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val punishmentId = createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    completeRehabilitativeActivity(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      punishmentId = punishmentId,
      completed = false,
      days = 5,
      notCompletedOutcome = NotCompletedOutcome.PARTIAL_ACTIVATE,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesCompleted").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesNotCompletedOutcome").isEqualTo(NotCompletedOutcome.PARTIAL_ACTIVATE.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").doesNotExist()
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.days").isEqualTo(5)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo(LocalDate.now().format(DateTimeFormatter.ISO_DATE))
      .jsonPath("$.reportedAdjudication.punishments[0].canEdit").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].canRemove").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].previousSuspendedUntilDate").doesNotExist()
  }

  @Test
  fun `completes a rehabilitative activity - extend suspension`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    val punishmentId = createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    completeRehabilitativeActivity(
      chargeNumber = scenario.getGeneratedChargeNumber(),
      punishmentId = punishmentId,
      completed = false,
      date = LocalDate.now().plusDays(10),
      notCompletedOutcome = NotCompletedOutcome.EXT_SUSPEND,
    ).expectStatus().isOk
      .expectBody()
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesCompleted").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].rehabilitativeActivitiesNotCompletedOutcome").isEqualTo(NotCompletedOutcome.EXT_SUSPEND.name)
      .jsonPath("$.reportedAdjudication.punishments[0].schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusDays(10).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
      .jsonPath("$.reportedAdjudication.punishments[0].canEdit").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].canRemove").isEqualTo(false)
      .jsonPath("$.reportedAdjudication.punishments[0].previousSuspendedUntilDate").exists()
  }

  @CsvSource("true", "false")
  @ParameterizedTest
  fun `update a payback punishment`(hasDuration: Boolean) {
    val hearingDate = LocalDate.of(2024, 1, 1)
    val scenario = initDataForUnScheduled().createHearing(dateTimeOfHearing = hearingDate.atStartOfDay()).createChargeProved()

    webTestClient.post()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.PAYBACK,
                endDate = LocalDate.now().plusDays(10),
                duration = 10,
                paybackNotes = "some payback notes",
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated

    val body = webTestClient.put()
      .uri("/reported-adjudications/${scenario.getGeneratedChargeNumber()}/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                id = 1,
                type = PunishmentType.PAYBACK,
                endDate = LocalDate.now().plusDays(10),
                duration = if (hasDuration) 12 else null,
                paybackNotes = "update payback notes",
              ),
            ),
        ),
      )
      .exchange()
      .expectBody()

    if (hasDuration) {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PAYBACK.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.duration").isEqualTo(12)
        .jsonPath("$.reportedAdjudication.punishments[0].paybackNotes").isEqualTo("update payback notes")
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.measurement").isEqualTo(Measurement.HOURS.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo("2024-01-01")
    } else {
      body.jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.PAYBACK.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.duration").doesNotExist()
        .jsonPath("$.reportedAdjudication.punishments[0].paybackNotes").isEqualTo("update payback notes")
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.measurement").isEqualTo(Measurement.HOURS.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo("2024-01-01")
    }
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
                duration = 10,
                suspendedUntil = suspendedUntil,
              ),
              PunishmentRequest(
                type = PunishmentType.REMOVAL_ACTIVITY,
                duration = 10,
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

    punishmentToAmend?.let {
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
                  startDate = suspendedUntil,
                  endDate = suspendedUntil,
                  duration = 15,
                ),
                PunishmentRequest(
                  type = PunishmentType.EXCLUSION_WORK,
                  duration = 8,
                  suspendedUntil = suspendedUntil,
                ),
              ),
          ),
        )
        .exchange()
        .expectStatus().isOk
        .expectBody()
        .jsonPath("$.reportedAdjudication.punishments.size()").isEqualTo(2)
        .jsonPath("$.reportedAdjudication.punishments[0].id").isEqualTo(it)
        .jsonPath("$.reportedAdjudication.punishments[0].type").isEqualTo(PunishmentType.CONFINEMENT.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.duration").isEqualTo(15)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.measurement").isEqualTo(Measurement.DAYS.name)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.startDate").isEqualTo(formattedDate)
        .jsonPath("$.reportedAdjudication.punishments[0].schedule.endDate").isEqualTo(formattedDate)
        .jsonPath("$.reportedAdjudication.punishments[1].type").isEqualTo(PunishmentType.EXCLUSION_WORK.name)
        .jsonPath("$.reportedAdjudication.punishments[1].schedule.duration").isEqualTo(8)
        .jsonPath("$.reportedAdjudication.punishments[1].schedule.suspendedUntil").isEqualTo(formattedDate)
    }
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
      .jsonPath("$.[0].punishment.schedule.duration").isEqualTo(10)
      .jsonPath("$.[0].punishment.schedule.measurement").isEqualTo(Measurement.DAYS.name)
      .jsonPath("$.[0].punishment.schedule.suspendedUntil").isEqualTo(
        LocalDate.now().plusMonths(1).format(
          DateTimeFormatter.ISO_DATE,
        ),
      )
  }

  @Test
  fun `get suspended punishments should not include any with rehabilitative activities`() {
    val scenario = initDataForUnScheduled().createHearing().createChargeProved()
    createRehabilitativeActivity(scenario.getGeneratedChargeNumber(), RehabilitativeActivityRequest())

    initDataForUnScheduled(IntegrationTestData.DEFAULT_ADJUDICATION_OVERRIDE).createHearing().createChargeProved()

    webTestClient.get()
      .uri("/reported-adjudications/punishments/${IntegrationTestData.DEFAULT_ADJUDICATION.prisonerNumber}/suspended/v2?chargeNumber=${IntegrationTestData.DEFAULT_ADJUDICATION_OVERRIDE.chargeNumber}")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("$.size()").isEqualTo(0)
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
                duration = 10,
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
      .jsonPath("$.[0].punishment.schedule.duration").isEqualTo(10)
      .jsonPath("$.[0].punishment.schedule.measurement").isEqualTo(Measurement.DAYS.name)
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
      .jsonPath("$.[0].duration").isEqualTo(10)
      .jsonPath("$.[0].measurement").isEqualTo(Measurement.DAYS.name)
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

  private fun createRehabilitativeActivity(
    chargeNumber: String,
    rehabilitativeActivityRequest: RehabilitativeActivityRequest,
  ): Long =
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/v2")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        mapOf(
          "punishments" to
            listOf(
              PunishmentRequest(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now(),
                duration = 10,
                rehabilitativeActivities = listOf(rehabilitativeActivityRequest),
              ),
            ),
        ),
      )
      .exchange()
      .expectStatus().isCreated
      .returnResult(ReportedAdjudicationResponse::class.java)
      .responseBody
      .blockFirst()!!
      .reportedAdjudication.punishments.first().id!!

  private fun completeRehabilitativeActivity(
    chargeNumber: String,
    punishmentId: Long,
    completed: Boolean,
    notCompletedOutcome: NotCompletedOutcome? = null,
    days: Int? = null,
    date: LocalDate? = null,
  ): WebTestClient.ResponseSpec =
    webTestClient.post()
      .uri("/reported-adjudications/$chargeNumber/punishments/$punishmentId/complete-rehabilitative-activity")
      .headers(setHeaders(username = "ITAG_ALO", roles = listOf("ROLE_ADJUDICATIONS_REVIEWER")))
      .bodyValue(
        CompleteRehabilitativeActivityRequest(
          completed = completed,
          outcome = notCompletedOutcome,
          daysToActivate = days,
          suspendedUntil = date,
        ),
      )
      .exchange()
}
