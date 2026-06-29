package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.TestOAuth2Config
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDateTime

@Import(TestOAuth2Config::class)
class ConsecutivePunishmentCorrectionIntTest : SqsIntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var eventPublishService: EventPublishService

  private val entityBuilder = EntityBuilder()

  private val olderCharge = "INT-1"
  private val newerCharge = "INT-2"
  private val prisoner = "INT123"

  @BeforeEach
  fun setUp() {
    // seed via the repository directly, so populate the security context that JPA auditing
    // (@CreatedBy / @LastModifiedBy) reads to fill the non-null create_user_id / modify_user_id
    SecurityContextHolder.getContext().authentication =
      UsernamePasswordAuthenticationToken("ITAG_USER", null, emptyList())

    // older charge, created first, consecutive to the newer charge (the link to clear)
    saveReport(chargeNumber = olderCharge, consecutiveTo = newerCharge, createdAt = LocalDateTime.of(2023, 1, 1, 9, 0))
    // newer charge, created second, consecutive to the older charge (the link to keep)
    saveReport(chargeNumber = newerCharge, consecutiveTo = olderCharge, createdAt = LocalDateTime.of(2023, 1, 2, 9, 0))
  }

  @AfterEach
  fun tearDown() {
    SecurityContextHolder.clearContext()
  }

  @Test
  fun `clears the older charge's consecutive link and fires a re-sync event for it`() {
    webTestClient.post()
      .uri("/scheduled-tasks/fix-consecutive-punishment-loops")
      .exchange()
      .expectStatus().isOk

    assertConsecutiveLink(olderCharge, expected = null)
    assertConsecutiveLink(newerCharge, expected = olderCharge)

    verify(eventPublishService, times(1)).publishEvent(
      eq(AdjudicationDomainEventType.PUNISHMENTS_UPDATED),
      argThat<ReportedAdjudicationDto> { chargeNumber == olderCharge },
    )
  }

  @Test
  fun `is idempotent - a second run clears nothing and fires no events`() {
    webTestClient.post().uri("/scheduled-tasks/fix-consecutive-punishment-loops").exchange().expectStatus().isOk
    reset(eventPublishService)

    webTestClient.post().uri("/scheduled-tasks/fix-consecutive-punishment-loops").exchange().expectStatus().isOk

    verify(eventPublishService, never()).publishEvent(any(), any())
    assertConsecutiveLink(olderCharge, expected = null)
    assertConsecutiveLink(newerCharge, expected = olderCharge)
  }

  private fun saveReport(chargeNumber: String, consecutiveTo: String, createdAt: LocalDateTime) {
    setAuditTime(createdAt)
    val report: ReportedAdjudication = entityBuilder.reportedAdjudication(
      chargeNumber = chargeNumber,
      prisonerNumber = prisoner,
      hearingId = null,
    ).also {
      it.addPunishment(
        Punishment(
          type = PunishmentType.ADDITIONAL_DAYS,
          consecutiveToChargeNumber = consecutiveTo,
          schedule = mutableListOf(PunishmentSchedule(duration = 5)),
        ),
      )
    }
    reportedAdjudicationRepository.save(report)
  }

  private fun assertConsecutiveLink(chargeNumber: String, expected: String?) {
    val body = webTestClient.get()
      .uri("/reported-adjudications/$chargeNumber/v2")
      .headers(setHeaders())
      .exchange()
      .expectStatus().isOk
      .expectBody()

    val path = "$.reportedAdjudication.punishments[0].consecutiveChargeNumber"
    if (expected == null) {
      body.jsonPath(path).doesNotExist()
    } else {
      body.jsonPath(path).isEqualTo(expected)
    }
  }
}
