package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.IssuedStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService
import java.time.LocalDate

@WebMvcTest(
  ReportsController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class ReportsControllerTest : TestControllerBase() {
  @MockBean
  lateinit var reportsService: ReportsService

  private val pageRequest = PageRequest.ofSize(20).withPage(0).withSort(
    Sort.by(
      Sort.Direction.DESC,
      "date_time_of_discovery",
    ),
  )

  @BeforeEach
  fun beforeEach() {
    val pageable = PageRequest.of(0, 20, Sort.by("date_time_of_discovery").descending())

    whenever(
      reportsService.getMyReportedAdjudications(
        any(),
        any(),
        any(),
        any(),
      ),
    ).thenReturn(
      PageImpl(
        listOf(REPORTED_ADJUDICATION_DTO),
        pageable,
        1,
      ),
    )
  }

  @Nested
  inner class MyReports {

    @Test
    fun `responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `makes a call to return my reported adjudications`() {
      getMyAdjudications().andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService).getMyReportedAdjudications(
        LocalDate.now().minusDays(3),
        LocalDate.now(),
        listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.SCHEDULED),
        pageRequest,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `returns my reported adjudications`() {
      getMyAdjudications()
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
        .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
        .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].chargeNumber").value("1"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `returns my reported adjudications with date and status filter`() {
      getMyAdjudicationsWithFilter(LocalDate.now().plusDays(5))
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
        .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
        .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].chargeNumber").value("1"))

      verify(reportsService).getMyReportedAdjudications(
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(5),
        listOf(ReportedAdjudicationStatus.AWAITING_REVIEW),
        pageRequest,
      )
    }

    @Test
    fun `paged responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    private fun getMyAdjudications(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/my-reports?status=UNSCHEDULED,SCHEDULED&page=0&size=20&sort=date_time_of_discovery,DESC")
            .header("Content-Type", "application/json"),
        )
    }

    private fun getMyAdjudicationsWithFilter(date: LocalDate): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/my-reports?status=AWAITING_REVIEW&startDate=$date&endDate=$date&page=0&size=20&sort=date_time_of_discovery,DESC")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class AllReports {

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `makes a call to return all reported adjudications`() {
      getAllAdjudications().andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService).getAllReportedAdjudications(
        LocalDate.now().minusDays(3),
        LocalDate.now(),
        listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.SCHEDULED),
        false,
        pageRequest,
      )
    }

    @Test
    fun `paged responds with a unauthorised status code for all adjudications`() {
      getAllAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `paged responds with a unauthorised status code for all adjudications without role for ALO`() {
      getAllAdjudications().andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    private fun getAllAdjudications(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/reports?status=UNSCHEDULED,SCHEDULED&page=0&size=20&sort=date_time_of_discovery,DESC")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class ReportsForIssue {
    @Test
    fun `responds with a unauthorised status code for adjudications to issue`() {
      getAdjudicationsForIssue().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `get adjudications for issue with defaulted dates`() {
      whenever(reportsService.getAdjudicationsForIssue(LocalDate.now().minusDays(2), LocalDate.now()))
        .thenReturn(emptyList())

      getAdjudicationsForIssue().andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService).getAdjudicationsForIssue(LocalDate.now().minusDays(2), LocalDate.now())
    }

    private fun getAdjudicationsForIssue(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/for-issue")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class ReportsForPrint {
    @Test
    fun `responds with a unauthorised status code for adjudications to print`() {
      getAdjudicationsForPrint().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `get adjudications for print with defaulted dates`() {
      whenever(reportsService.getAdjudicationsForPrint(LocalDate.now(), LocalDate.now().plusDays(2), IssuedStatus.values().toList()))
        .thenReturn(emptyList())

      getAdjudicationsForPrint().andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService).getAdjudicationsForPrint(LocalDate.now(), LocalDate.now().plusDays(2), IssuedStatus.values().toList())
    }

    private fun getAdjudicationsForPrint(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/for-print?issueStatus=ISSUED,NOT_ISSUED")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class ReportCounters {

    @BeforeEach
    fun `init`() {
      whenever(reportsService.getReportCounts()).thenReturn(
        AgencyReportCountsDto(
          reviewTotal = 1,
          transferReviewTotal = 1,
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code for counters`() {
      getCounters().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with report counts `() {
      getCounters().andExpect(MockMvcResultMatchers.status().isOk)

      verify(reportsService, atLeastOnce()).getReportCounts()
    }

    private fun getCounters(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/report-counts")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class AdjudicationHistoryForBooking {
    @BeforeEach
    fun `init`() {
      val pageable = PageRequest.of(0, 20, Sort.by("date_time_of_discovery").descending())

      whenever(
        reportsService.getAdjudicationsForBooking(
          any(),
          anyOrNull(),
          anyOrNull(),
          any(),
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        PageImpl(
          listOf(REPORTED_ADJUDICATION_DTO),
          pageable,
          1,
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code for history`() {
      getAdjudicationHistory().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with adjudication history for prisoner `() {
      getAdjudicationHistory().andExpect(MockMvcResultMatchers.status().isOk)

      verify(reportsService, atLeastOnce()).getAdjudicationsForBooking(any(), anyOrNull(), anyOrNull(), any(), any(), any(), any(), any(), any())
    }

    private fun getAdjudicationHistory(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/booking/12345?status=UNSCHEDULED,SCHEDULED&agency=MDI&page=0&size=20&sort=date_time_of_discovery,DESC")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class AdjudicationHistoryForPrisoner {
    @BeforeEach
    fun `init`() {
      val pageable = PageRequest.of(0, 20, Sort.by("date_time_of_discovery").descending())

      whenever(
        reportsService.getAdjudicationsForPrisoner(
          any(),
          anyOrNull(),
          anyOrNull(),
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        PageImpl(
          listOf(REPORTED_ADJUDICATION_DTO),
          pageable,
          1,
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code for history`() {
      getAdjudicationHistory().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with adjudication history for prisoner `() {
      getAdjudicationHistory().andExpect(MockMvcResultMatchers.status().isOk)

      verify(reportsService, atLeastOnce()).getAdjudicationsForPrisoner(any(), anyOrNull(), anyOrNull(), any(), any(), any(), any(), any())
    }

    private fun getAdjudicationHistory(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/bookings/prisoner/AY12345?status=UNSCHEDULED,SCHEDULED&page=0&size=20&sort=date_time_of_discovery,DESC")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class GetAllReportsByPrisoner {

    @Test
    fun `responds with a unauthorised status code`() {
      getAllReportsByPrisonerRequest()
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ALL_ADJUDICATIONS"])
    fun `responds successfully`() {
      whenever(
        reportsService.getReportsForPrisoner(any()),
      ).thenReturn(listOf(REPORTED_ADJUDICATION_DTO))

      getAllReportsByPrisonerRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService, atLeastOnce()).getReportsForPrisoner("A12345")
    }

    private fun getAllReportsByPrisonerRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/prisoner/A12345")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class GetAllReportsByBooking {

    @Test
    fun `responds with a unauthorised status code`() {
      getAllReportsByBookingRequest()
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ALL_ADJUDICATIONS"])
    fun `responds successfully`() {
      whenever(
        reportsService.getReportsForBooking(any()),
      ).thenReturn(listOf(REPORTED_ADJUDICATION_DTO))

      getAllReportsByBookingRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService, atLeastOnce()).getReportsForBooking(12345)
    }

    private fun getAllReportsByBookingRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/all-by-booking/12345")
            .header("Content-Type", "application/json"),
        )
    }
  }
}
