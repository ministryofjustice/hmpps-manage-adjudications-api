package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferOutAndHearingsToScheduledCutOffDate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferOutStatuses
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferReviewStatuses
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class ReportsServiceTest : ReportedAdjudicationTestBase() {

  private val reportsService = ReportsService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  @Nested
  inner class AllReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(chargeNumber = "1", dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(chargeNumber = "2", dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)

      whenever(
        reportedAdjudicationRepository.findAllReportsByAgency(
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2),
        ),
      )
    }

    @Test
    fun `makes a call to the reported adjudication repository to get the page of adjudications`() {
      reportsService.getAllReportedAdjudications(
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.entries,
        Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository).findAllReportsByAgency(
        "MDI",
        LocalDate.now().atStartOfDay(),
        LocalDate.now().atTime(LocalTime.MAX),
        ReportedAdjudicationStatus.entries.map { it.name },
        Pageable.ofSize(20).withPage(0),
      )
    }

    @Test
    fun `returns all reported adjudications`() {
      val myReportedAdjudications = reportsService.getAllReportedAdjudications(
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.entries,
        Pageable.ofSize(20).withPage(0),
      )

      assertThat(myReportedAdjudications.content)
        .extracting("chargeNumber", "prisonerNumber", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple("1", "A12345", "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple("2", "A12345", "P_SMITH", REPORTED_DATE_TIME.plusDays(2)),
        )
    }
  }

  @Nested
  inner class TransferReportedAdjudications {
    @Test
    fun `find by transfers in `() {
      whenever(reportedAdjudicationRepository.findTransfersInByAgency(any(), any(), any())).thenReturn(
        Page.empty(),
      )

      reportsService.getTransferReportedAdjudications(
        ReportedAdjudicationStatus.entries,
        TransferType.IN,
        Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findTransfersInByAgency(any(), any(), any())
    }

    @Test
    fun `find by transfers out`() {
      whenever(reportedAdjudicationRepository.findTransfersOutByAgency(any(), any(), any(), any())).thenReturn(
        Page.empty(),
      )

      reportsService.getTransferReportedAdjudications(
        ReportedAdjudicationStatus.entries,
        TransferType.OUT,
        Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findTransfersOutByAgency(any(), any(), any(), any())
    }

    @Test
    fun `find by transfers all`() {
      whenever(reportedAdjudicationRepository.findTransfersAllByAgency(any(), any(), any(), any())).thenReturn(
        Page.empty(),
      )

      reportsService.getTransferReportedAdjudications(
        ReportedAdjudicationStatus.entries,
        TransferType.ALL,
        Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findTransfersAllByAgency(any(), any(), any(), any())
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(chargeNumber = "1", dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(chargeNumber = "2", dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByCreatedByUserIdAndOriginatingAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2),
        ),
      )
    }

    @Test
    fun `returns my reported adjudications`() {
      val myReportedAdjudications = reportsService.getMyReportedAdjudications(
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.entries,
        Pageable.ofSize(20).withPage(0),
      )

      assertThat(myReportedAdjudications.content)
        .extracting("chargeNumber", "prisonerNumber", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple("1", "A12345", "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple("2", "A12345", "P_SMITH", REPORTED_DATE_TIME.plusDays(2)),
        )
    }
  }

  @Nested
  inner class ReportAdjudicationsForIssue {

    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationRepository.findReportsForIssue(
          "MDI",
          LocalDate.now().atStartOfDay().minusDays(2),
          LocalDate.now().atTime(LocalTime.MAX),
          ReportedAdjudicationStatus.issuableStatuses().map { it.name },
        ),
      ).thenReturn(
        listOf(first, second),
      )
    }

    @Test
    fun `returns adjudications for issue (All locations) with correct issue details and order for status SCHEDULED and UNSCHEDULED only `() {
      val response = reportsService.getAdjudicationsForIssue(
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
      )

      assertThat(response)
        .extracting("chargeNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple("3", "A12345", "testing", now),
          Tuple.tuple("2", "A12345", null, null),
        )

      assertThat(response.first().dateTimeOfDiscovery).isEqualTo(first.dateTimeOfDiscovery)
    }
  }

  @Nested
  inner class ReportedAdjudicationsForPrint {

    @BeforeEach
    fun beforeEach() {
      whenever(
        reportedAdjudicationRepository.findReportsForPrint(
          "MDI",
          LocalDate.now().atStartOfDay().minusDays(2),
          LocalDate.now().atTime(LocalTime.MAX),
          listOf(ReportedAdjudicationStatus.SCHEDULED).map { it.name },
        ),
      ).thenReturn(
        listOf(first, second, third),
      )
    }

    @Test
    fun `returns adjudications for issue (All locations) with correct issue details and order for status SCHEDULED only for all issue statuses `() {
      val response = reportsService.getAdjudicationsForPrint(
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
        issueStatuses = IssuedStatus.entries,
      )

      assertThat(response.size).isEqualTo(3)

      assertThat(response)
        .extracting("chargeNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple("3", "A12345", "testing", now),
          Tuple.tuple("4", "A12345", "testing", now),
          Tuple.tuple("2", "A12345", null, null),
        )

      assertThat(response.first { it.chargeNumber == "3" }.incidentDetails)
        .extracting("dateTimeOfDiscovery", "locationId")
        .contains(first.dateTimeOfDiscovery, 2L)
    }

    @Test
    fun `returns issued adjudications for all locations `() {
      val response = reportsService.getAdjudicationsForPrint(
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
        issueStatuses = listOf(IssuedStatus.ISSUED),
      )

      assertThat(response.size).isEqualTo(2)

      assertThat(response)
        .extracting("chargeNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple("4", "A12345", "testing", now),
        )

      assertThat(response.first().incidentDetails)
        .extracting("dateTimeOfDiscovery", "locationId")
        .contains(third.dateTimeOfDiscovery, 2L)
    }

    @Test
    fun `returns not issued adjudications for all locations `() {
      val response = reportsService.getAdjudicationsForPrint(
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now(),
        issueStatuses = listOf(IssuedStatus.NOT_ISSUED),
      )

      assertThat(response.size).isEqualTo(1)

      assertThat(response)
        .extracting("chargeNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple("2", "A12345", null, null),
        )

      assertThat(response.first().incidentDetails)
        .extracting("dateTimeOfDiscovery", "locationId")
        .contains(second.dateTimeOfDiscovery, 3L)
    }
  }

  @Nested
  inner class ReportCounts {

    @Test
    fun `get reports count for agency`(): Unit = runBlocking {
      whenever(reportedAdjudicationRepository.countByOriginatingAgencyIdAndStatus("MDI", ReportedAdjudicationStatus.AWAITING_REVIEW)).thenReturn(2)
      whenever(reportedAdjudicationRepository.countTransfersIn("MDI", transferReviewStatuses.map { it.name })).thenReturn(1)
      whenever(reportedAdjudicationRepository.countTransfersOut("MDI", transferOutStatuses.map { it.name }, transferOutAndHearingsToScheduledCutOffDate)).thenReturn(2)
      whenever(reportedAdjudicationRepository.countByOriginatingAgencyIdAndOverrideAgencyIdIsNullAndStatusInAndDateTimeOfDiscoveryAfter("MDI", ReportsService.hearingsToScheduleStatuses, transferOutAndHearingsToScheduledCutOffDate)).thenReturn(3)
      whenever(reportedAdjudicationRepository.countByOverrideAgencyIdAndStatusInAndDateTimeOfDiscoveryAfter("MDI", ReportsService.hearingsToScheduleStatuses, transferOutAndHearingsToScheduledCutOffDate)).thenReturn(3)

      val result = reportsService.getReportCounts()

      assertThat(result.reviewTotal).isEqualTo(2)
      assertThat(result.transferReviewTotal).isEqualTo(1)
      assertThat(result.transferOutTotal).isEqualTo(2)
      assertThat(result.transferAllTotal).isEqualTo(3)
      assertThat(result.hearingsToScheduleTotal).isEqualTo(6)
    }
  }

  @Nested
  inner class AdjudicationHistoryForBooking {

    @Test
    fun `gets all reports for agency and status without any dates`() {
      whenever(
        reportedAdjudicationRepository.findAdjudicationsForBooking(any(), any(), any(), any(), any(), any()),
      ).thenReturn(
        PageImpl(
          listOf(
            entityBuilder.reportedAdjudication(),
          ),
        ),
      )

      val response = reportsService.getAdjudicationsForBooking(
        bookingId = 1,
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED),
        agencies = listOf("MDI"),
        ada = false,
        pada = false,
        suspended = false,
        pageable = Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findAdjudicationsForBooking(any(), any(), any(), any(), any(), any())

      assertThat(response.content.size).isEqualTo(1)
    }

    @Test
    fun `gets all reports for agency, punishment and status without any dates`() {
      whenever(
        reportedAdjudicationRepository.findAdjudicationsForBookingWithPunishments(any(), any(), any(), any(), any(), any(), any(), any(), any()),
      ).thenReturn(
        PageImpl(
          listOf(
            entityBuilder.reportedAdjudication(),
          ),
        ),
      )

      val response = reportsService.getAdjudicationsForBooking(
        bookingId = 1,
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED),
        agencies = listOf("MDI"),
        ada = true,
        pada = false,
        suspended = false,
        pageable = Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findAdjudicationsForBookingWithPunishments(any(), any(), any(), any(), any(), any(), any(), any(), any())

      assertThat(response.content.size).isEqualTo(1)
    }

    @CsvSource("true", "false")
    @ParameterizedTest
    fun `can action from history is true`(overrideAgency: Boolean) {
      whenever(authenticationFacade.isAlo).thenReturn(true)
      whenever(
        reportedAdjudicationRepository.findAdjudicationsForBooking(any(), any(), any(), any(), any(), any()),
      ).thenReturn(
        PageImpl(
          listOf(
            entityBuilder.reportedAdjudication().also {
              it.overrideAgencyId = if (overrideAgency) it.originatingAgencyId else null
            },
          ),
        ),
      )

      val response = reportsService.getAdjudicationsForBooking(
        bookingId = 1,
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED),
        agencies = listOf("MDI"),
        ada = false,
        pada = false,
        suspended = false,
        pageable = Pageable.ofSize(20).withPage(0),
      )
      assertThat(response.content.first().canActionFromHistory).isTrue
    }
  }

  @Nested
  inner class AdjudicationHistoryForPrisoner {

    @Test
    fun `gets all reports for agency and status without any dates`() {
      whenever(
        reportedAdjudicationRepository.findAdjudicationsForPrisoner(any(), any(), any(), any(), any()),
      ).thenReturn(
        PageImpl(
          listOf(
            entityBuilder.reportedAdjudication(),
          ),
        ),
      )

      val response = reportsService.getAdjudicationsForPrisoner(
        prisonerNumber = "A1234",
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED),
        ada = false,
        pada = false,
        suspended = false,
        pageable = Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findAdjudicationsForPrisoner(any(), any(), any(), any(), any())

      assertThat(response.content.size).isEqualTo(1)
    }

    @Test
    fun `gets all reports for agency, punishment and status without any dates`() {
      whenever(
        reportedAdjudicationRepository.findAdjudicationsForPrisonerWithPunishments(any(), any(), any(), any(), any(), any(), any(), any()),
      ).thenReturn(
        PageImpl(
          listOf(
            entityBuilder.reportedAdjudication(),
          ),
        ),
      )

      val response = reportsService.getAdjudicationsForPrisoner(
        prisonerNumber = "A1234",
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED),
        ada = true,
        pada = false,
        suspended = false,
        pageable = Pageable.ofSize(20).withPage(0),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findAdjudicationsForPrisonerWithPunishments(any(), any(), any(), any(), any(), any(), any(), any())

      assertThat(response.content.size).isEqualTo(1)
    }
  }

  @Nested
  inner class ReportsByPrisoner {

    @Test
    fun `returns reports for a prisoner`() {
      whenever(reportedAdjudicationRepository.findByPrisonerNumber("A12345")).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(),
        ),
      )
      val response = reportsService.getReportsForPrisoner("A12345")
      assertThat(response.isNotEmpty()).isTrue
    }
  }

  @Nested
  inner class ReportsByBooking {

    @Test
    fun `returns reports for a booking`() {
      whenever(reportedAdjudicationRepository.findByOffenderBookingId(12345)).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(),
        ),
      )
      val response = reportsService.getReportsForBooking(12345)
      assertThat(response.isNotEmpty()).isTrue
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not required.
  }

  companion object {
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)

    private val now = LocalDateTime.now()

    private val third = entityBuilder.reportedAdjudication(
      chargeNumber = "4",
      dateTime = now.minusDays(3),
    ).also {
      it.status = ReportedAdjudicationStatus.SCHEDULED
      it.issuingOfficer = "testing"
      it.dateTimeOfIssue = now
      it.dateTimeOfFirstHearing = now
      it.createDateTime = now
      it.createdByUserId = "testing"
    }

    private val second = entityBuilder.reportedAdjudication(
      chargeNumber = "2",
      dateTime = now.minusDays(2),
    ).also {
      it.status = ReportedAdjudicationStatus.UNSCHEDULED
      it.locationId = 3
      it.locationUuid = UUID.fromString("0194ac91-0968-75b1-b304-73e905ab934d")
      it.createDateTime = now
      it.createdByUserId = "testing"
    }

    private val first = entityBuilder.reportedAdjudication(
      chargeNumber = "3",
      dateTime = now.minusDays(3),
    ).also {
      it.status = ReportedAdjudicationStatus.SCHEDULED
      it.issuingOfficer = "testing"
      it.dateTimeOfIssue = now
      it.dateTimeOfFirstHearing = now
      it.createDateTime = now
      it.createdByUserId = "testing"
    }
  }
}
