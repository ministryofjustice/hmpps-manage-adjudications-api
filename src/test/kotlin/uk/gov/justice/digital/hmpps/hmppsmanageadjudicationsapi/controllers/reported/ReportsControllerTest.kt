package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService
import java.time.LocalDate

@WebMvcTest(value = [ReportsController::class])
class ReportsControllerTest : TestControllerBase() {
  @MockBean
  lateinit var reportsService: ReportsService

  private val pageRequest = PageRequest.ofSize(20).withPage(0).withSort(
    Sort.by(
      Sort.Direction.DESC,
      "dateTimeOfDiscovery"
    )
  )

  @BeforeEach
  fun beforeEach() {
    whenever(
      reportsService.getMyReportedAdjudications(
        any(), any(), any(), any(), any()
      )
    ).thenReturn(
      PageImpl(
        listOf(REPORTED_ADJUDICATION_DTO),
        Pageable.ofSize(20).withPage(0),
        1
      ),
    )
  }

  @Test
  fun `responds with a unauthorised status code`() {
    getMyAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `makes a call to return my reported adjudications`() {
    getMyAdjudications().andExpect(MockMvcResultMatchers.status().isOk)
    verify(reportsService).getMyReportedAdjudications(
      "MDI",
      LocalDate.now().minusDays(3), LocalDate.now(), listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.SCHEDULED), pageRequest
    )
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
  fun `makes a call to return all reported adjudications`() {
    getAllAdjudications().andExpect(MockMvcResultMatchers.status().isOk)
    verify(reportsService).getAllReportedAdjudications(
      "MDI",
      LocalDate.now().minusDays(3), LocalDate.now(), listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.SCHEDULED),
      pageRequest
    )
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `returns my reported adjudications`() {
    getMyAdjudications()
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
      .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
      .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
      .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].adjudicationNumber").value(1))
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `returns my reported adjudications with date and status filter`() {
    getMyAdjudicationsWithFilter(LocalDate.now().plusDays(5))
      .andExpect(MockMvcResultMatchers.status().isOk)
      .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
      .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
      .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
      .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].adjudicationNumber").value(1))

    verify(reportsService).getMyReportedAdjudications(
      "MDI",
      LocalDate.now().plusDays(5),
      LocalDate.now().plusDays(5),
      listOf(ReportedAdjudicationStatus.AWAITING_REVIEW),
      pageRequest
    )
  }

  @Test
  fun `paged responds with a unauthorised status code`() {
    getMyAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  fun `paged responds with a unauthorised status code for all adjudications`() {
    getAllAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `paged responds with a unauthorised status code for all adjudications without role for ALO`() {
    getAllAdjudications().andExpect(MockMvcResultMatchers.status().isForbidden)
  }

  @Test
  fun `responds with a unauthorised status code for adjudications to issue`() {
    getAdjudicationsForIssue().andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER")
  fun `get adjudications for issue with defaulted dates`() {
    whenever(reportsService.getAdjudicationsForIssue("MDI", 1L, LocalDate.now().minusDays(2), LocalDate.now(), pageRequest))
      .thenReturn(Page.empty())

    getAdjudicationsForIssue().andExpect(MockMvcResultMatchers.status().isOk)
    verify(reportsService).getAdjudicationsForIssue("MDI", 1L, LocalDate.now().minusDays(2), LocalDate.now(), pageRequest)
  }

  private fun getMyAdjudications(): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get("/reported-adjudications/my/agency/MDI?status=UNSCHEDULED,SCHEDULED&page=0&size=20&sort=dateTimeOfDiscovery,DESC")
          .header("Content-Type", "application/json")
      )
  }

  private fun getAllAdjudications(): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get("/reported-adjudications/agency/MDI?status=UNSCHEDULED,SCHEDULED&page=0&size=20&sort=dateTimeOfDiscovery,DESC")
          .header("Content-Type", "application/json")
      )
  }

  private fun getMyAdjudicationsWithFilter(date: LocalDate): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get("/reported-adjudications/my/agency/MDI?status=AWAITING_REVIEW&startDate=$date&endDate=$date&page=0&size=20&sort=dateTimeOfDiscovery,DESC")
          .header("Content-Type", "application/json")
      )
  }

  private fun getAdjudicationsForIssue(): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get("/reported-adjudications/agency/MDI/issue?locationId=1&page=0&size=20&sort=dateTimeOfDiscovery,DESC")
          .header("Content-Type", "application/json")
      )
  }
}
