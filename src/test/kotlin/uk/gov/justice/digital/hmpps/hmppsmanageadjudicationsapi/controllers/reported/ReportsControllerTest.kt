package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
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
import java.util.Optional

@WebMvcTest(value = [ReportsController::class])
class ReportsControllerTest : TestControllerBase() {
  @MockBean
  lateinit var reportsService: ReportsService

  @Nested
  inner class MyReportedAdjudications {
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
        LocalDate.now().minusDays(3), LocalDate.now(), Optional.empty(),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `makes a call to return all reported adjudications`() {
      getAllAdjudications().andExpect(MockMvcResultMatchers.status().isOk)
      verify(reportsService).getAllReportedAdjudications(
        "MDI",
        LocalDate.now().minusDays(3), LocalDate.now(), Optional.empty(),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
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
      getMyAdjudicationsWithFilter(LocalDate.now().plusDays(5), ReportedAdjudicationStatus.AWAITING_REVIEW)
        .andExpect(MockMvcResultMatchers.status().isOk)
        .andExpect(MockMvcResultMatchers.jsonPath("$.totalPages").value(1))
        .andExpect(MockMvcResultMatchers.jsonPath("$.size").value(20))
        .andExpect(MockMvcResultMatchers.jsonPath("$.number").value(0))
        .andExpect(MockMvcResultMatchers.jsonPath("$.content[0].adjudicationNumber").value(1))

      verify(reportsService).getMyReportedAdjudications(
        "MDI",
        LocalDate.now().plusDays(5),
        LocalDate.now().plusDays(5),
        Optional.of(ReportedAdjudicationStatus.AWAITING_REVIEW),
        PageRequest.ofSize(20).withPage(0).withSort(
          Sort.by(
            Sort.Direction.DESC,
            "incidentDate"
          )
        )
      )
    }

    @Test
    fun `paged responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    private fun getMyAdjudications(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/my/agency/MDI?page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }

    private fun getAllAdjudications(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/agency/MDI?page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }

    private fun getMyAdjudicationsWithFilter(date: LocalDate, reportedAdjudicationStatus: ReportedAdjudicationStatus): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/my/agency/MDI?startDate=$date&endDate=$date&status=$reportedAdjudicationStatus&page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }
  }
}
