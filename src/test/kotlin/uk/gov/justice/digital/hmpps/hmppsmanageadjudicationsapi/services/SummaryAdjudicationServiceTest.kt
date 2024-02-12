package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.util.CollectionUtils
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

class SummaryAdjudicationServiceTest : ReportedAdjudicationTestBase() {

  private val legacyNomisGateway: LegacyNomisGateway = mock()

  private val prisonerNumber = "A1234AB"

  private val summaryAdjudicationService = SummaryAdjudicationService(
    legacyNomisGateway,
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  @Nested
  inner class Legacy {

    @Test
    fun `can paginate results from NOMIS`() {
      val today = LocalDate.now()

      val offenceId = 1L
      val agencyId = "MDI"
      val finding = Finding.PROVED
      val fromDate = today.minusDays(30)
      val toDate = today.minusDays(1)
      val pageable = PageRequest.of(1, 10)

      val headers = CollectionUtils.unmodifiableMultiValueMap(
        CollectionUtils.toMultiValueMap(
          mapOf(
            "Page-Offset" to listOf("10"),
            "Page-Limit" to listOf("10"),
            "Total-Records" to listOf("30"),
          ),
        ),
      )

      whenever(
        legacyNomisGateway.getAdjudicationsForPrisoner(
          prisonerNumber,
          offenceId,
          agencyId,
          finding,
          fromDate,
          toDate,
          pageable,
        ),
      ).thenReturn(
        ResponseEntity(
          AdjudicationResponse(
            results = listOf(),
            offences = listOf(),
            agencies = listOf(),
          ),
          headers,
          HttpStatusCode.valueOf(200),
        ),
      )

      val adjudications = summaryAdjudicationService.getAdjudications(
        prisonerNumber = prisonerNumber,
        offenceId = offenceId,
        agencyId = agencyId,
        finding = finding,
        fromDate = fromDate,
        toDate = toDate,
        pageable = pageable,
      )

      assertThat(adjudications.results.totalElements).isEqualTo(30)
      assertThat(adjudications.results.totalPages).isEqualTo(3)
      assertThat(adjudications.results.pageable.pageNumber).isEqualTo(1)
      assertThat(adjudications.results.numberOfElements).isEqualTo(0)
    }

    @Test
    fun `can get results for a prisoner number`() {
      val adjudicationNumber = 10000L
      whenever(legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber = prisonerNumber, chargeId = 1L)).thenReturn(
        AdjudicationDetail(
          adjudicationNumber = adjudicationNumber,
        ),
      )
      val adjudicationDetail = summaryAdjudicationService.getAdjudication(prisonerNumber, 1L)

      assertThat(adjudicationDetail.adjudicationNumber).isEqualTo(adjudicationNumber)
    }
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
            PunishmentSchedule(days = 0, startDate = LocalDate.now(), endDate = LocalDate.now()).also {
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
              days = 0,
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
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(2)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)
      assertThat(response.adjudicationCount).isEqualTo(2)
      assertThat(response.awards.size).isEqualTo(2)
      assertThat(response.bookingId).isEqualTo(1L)

      response.awards.forEach {
        assertThat(it.months).isNull()
        assertThat(it.hearingSequence).isNull()
        assertThat(it.bookingId).isEqualTo(1)
        assertThat(it.effectiveDate).isEqualTo(LocalDate.now())
      }
    }

    @Test
    fun `suspended award should not return - original was filter in DPS, connect team not maintained it, but its better to be placed in api`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.TV,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(1)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.isEmpty())
    }

    @Test
    fun `with effective date`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            privilegeType = PrivilegeType.TV,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1), endDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(1)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().effectiveDate).isEqualTo(LocalDate.now().plusDays(1))
    }

    @CsvSource("DAMAGES_OWED", "EARNINGS")
    @ParameterizedTest
    fun `with amount`(punishmentType: PunishmentType) {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = punishmentType,
            amount = if (punishmentType == PunishmentType.DAMAGES_OWED) 100.0 else null,
            stoppagePercentage = if (punishmentType == PunishmentType.EARNINGS) 100 else null,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1), endDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(1)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().limit!!.toDouble()).isEqualTo(100.0)
      assertThat(response.awards.first().comment).contains("100")
    }

    @Test
    fun `with privilege`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.FACILITIES,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1), endDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(1)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().sanctionCodeDescription).contains(PrivilegeType.FACILITIES.name)
    }

    @Test
    fun `with other privilege`() {
      basicData.also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "playstation",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, startDate = LocalDate.now().plusDays(1), endDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
      }

      whenever(
        reportedAdjudicationRepository.countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
          1L,
          ReportedAdjudicationStatus.CHARGE_PROVED,
          LocalDate.now().minusMonths(3).atStartOfDay(),
        ),
      ).thenReturn(1)

      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(basicData))

      val response = summaryAdjudicationService.getAdjudicationSummary(bookingId = 1L, awardCutoffDate = null, adjudicationCutoffDate = null)

      assertThat(response.awards.first().sanctionCodeDescription).contains("playstation")
    }
  }

  @Nested
  inner class HasAdjudications {

    @Test
    fun `prisoner has adjudications`() {
      whenever(reportedAdjudicationRepository.countByOffenderBookingId(1)).thenReturn(1)

      val result = summaryAdjudicationService.hasAdjudications(1)
      assertThat(result.hasAdjudications).isTrue
    }

    @Test
    fun `prisoner has no adjudications`() {
      whenever(reportedAdjudicationRepository.countByOffenderBookingId(1)).thenReturn(0)

      val result = summaryAdjudicationService.hasAdjudications(1)
      assertThat(result.hasAdjudications).isFalse
    }
  }
}
