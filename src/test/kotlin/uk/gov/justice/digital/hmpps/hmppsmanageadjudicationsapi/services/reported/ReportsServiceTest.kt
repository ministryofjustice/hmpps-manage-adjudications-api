package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReportsServiceTest : ReportedAdjudicationTestBase() {

  private val reportsService = ReportsService(
    reportedAdjudicationRepository, authenticationFacade, offenceCodeLookupService
  )

  @Nested
  inner class AllReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(reportNumber = 1L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(reportNumber = 2L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
          any(),
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
    }

    @Test
    fun `makes a call to the reported adjudication repository to get the page of adjudications`() {
      reportsService.getAllReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(20).withPage(0)
      )

      verify(reportedAdjudicationRepository).findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        "MDI",
        LocalDate.now().atStartOfDay(),
        LocalDate.now().atTime(LocalTime.MAX),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(20).withPage(0)
      )
    }

    @Test
    fun `returns all reported adjudications`() {
      val myReportedAdjudications = reportsService.getAllReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(20).withPage(0)
      )

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple(1L, "A12345", 234L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "A12345", 234L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(reportNumber = 1L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(reportNumber = 2L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
    }

    @Test
    fun `returns my reported adjudications`() {
      val myReportedAdjudications = reportsService.getMyReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(20).withPage(0)
      )

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple(1L, "A12345", 234L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "A12345", 234L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class ReportAdjudicationsForIssue {

    private val now = LocalDateTime.now()

    private val third = entityBuilder.reportedAdjudication(
      reportNumber = 1L,
      dateTime = now.minusDays(1)
    )

    private val second = entityBuilder.reportedAdjudication(
      reportNumber = 2L,
      dateTime = now.minusDays(2)
    )

    private val first = entityBuilder.reportedAdjudication(
      reportNumber = 3L,
      dateTime = now.minusDays(3),
    )

    @BeforeEach
    fun beforeEach() {

      first.issuingOfficer = "testing"
      first.dateTimeOfIssue = now
      first.status = ReportedAdjudicationStatus.SCHEDULED
      first.createDateTime = now
      first.createdByUserId = "testing"

      second.status = ReportedAdjudicationStatus.UNSCHEDULED
      second.locationId = 3
      second.createDateTime = now
      second.createdByUserId = "testing"

      third.createdByUserId = "testing"
      third.createDateTime = now
      third.locationId = 4
      third.status = ReportedAdjudicationStatus.AWAITING_REVIEW

      whenever(
        reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetween(
          "MDI",
          LocalDate.now().atStartOfDay().minusDays(2), LocalDate.now().atTime(LocalTime.MAX)
        )
      ).thenReturn(
        listOf(first, second, third)
      )
    }

    @Test
    fun `returns adjudications for issue (All locations) with correct issue details and order for status SCHEDULED and UNSCHEDULED only `() {
      val response = reportsService.getAdjudicationsForIssue(
        agencyId = "MDI",
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now()
      )

      assertThat(response)
        .extracting("adjudicationNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple(3L, "A12345", "testing", now),
          Tuple.tuple(2L, "A12345", null, null),
        )

      assertThat(response.first().incidentDetails)
        .extracting("dateTimeOfDiscovery", "locationId")
        .contains(first.dateTimeOfDiscovery, 2L)
    }

    @Test
    fun `returns adjudications for location id 2 with correct issue details and order for status SCHEDULED and UNSCHEDULED only `() {
      val response = reportsService.getAdjudicationsForIssue(
        agencyId = "MDI",
        locationId = 2,
        startDate = LocalDate.now().minusDays(2),
        endDate = LocalDate.now()
      )

      assertThat(response)
        .extracting("adjudicationNumber", "prisonerNumber", "issuingOfficer", "dateTimeOfIssue")
        .contains(
          Tuple.tuple(3L, "A12345", "testing", now),
        )

      assertThat(response.first().incidentDetails)
        .extracting("dateTimeOfDiscovery", "locationId")
        .contains(first.dateTimeOfDiscovery, 2L)
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // not required.
  }

  companion object {
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
  }
}
