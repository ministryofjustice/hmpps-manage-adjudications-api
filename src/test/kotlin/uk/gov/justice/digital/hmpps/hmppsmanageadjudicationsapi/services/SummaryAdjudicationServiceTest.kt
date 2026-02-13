package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportQueryService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

class SummaryAdjudicationServiceTest : ReportedAdjudicationTestBase() {

  private val punishmentsReportQueryService = PunishmentsReportQueryService(reportedAdjudicationRepository)
  private val summaryAdjudicationService = SummaryAdjudicationService(
    punishmentsReportQueryService,
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Nested
  inner class AdjudicationsSummary {

    private val basicData = entityBuilder.reportedAdjudication().also {
      it.offenderBookingId = 1L
      it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          type = PunishmentType.CAUTION,
          schedule =
          mutableListOf(
            PunishmentSchedule(duration = 0, startDate = LocalDate.now(), endDate = LocalDate.now()).also {
              it.createDateTime = LocalDateTime.now()
            },
          ),
        ),
      )
      it.addPunishment(
        Punishment(
          type = PunishmentType.CAUTION,
          schedule =
          mutableListOf(
            PunishmentSchedule(
              duration = 0,
              startDate = LocalDate.now(),
              endDate = LocalDate.now(),
            ).also {
              it.createDateTime = LocalDateTime.now()
            },
          ),
        ),
      )
    }

    @Test
    fun `returns adjudication summary for prisoner - basic`() {
      whenever(
        reportedAdjudicationRepository.activeChargeProvedForBookingId(
          1L,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(2)

      whenever(
        reportedAdjudicationRepository.findIdsForActivePunishmentsByBookingId(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(1L))

      whenever(
        reportedAdjudicationRepository.findByIdsWithPunishments(any()),
      ).thenReturn(listOf(basicData))

      val response =
        summaryAdjudicationService.getAdjudicationSummary(
          bookingId = 1L,
          awardCutoffDate = null,
          adjudicationCutoffDate = null,
        )
      assertThat(response.adjudicationCount).isEqualTo(2)
      assertThat(response.awards.size).isEqualTo(2)
      assertThat(response.bookingId).isEqualTo(1L)
    }
  }

  @Nested
  inner class HasAdjudications {

    @Test
    fun `prisoner has adjudications`() {
      whenever(reportedAdjudicationRepository.existsByOffenderBookingId(1)).thenReturn(true)

      val result = summaryAdjudicationService.hasAdjudications(1)
      assertThat(result.hasAdjudications).isTrue
    }

    @Test
    fun `prisoner has no adjudications`() {
      whenever(reportedAdjudicationRepository.existsByOffenderBookingId(1)).thenReturn(false)

      val result = summaryAdjudicationService.hasAdjudications(1)
      assertThat(result.hasAdjudications).isFalse
    }
  }
}
