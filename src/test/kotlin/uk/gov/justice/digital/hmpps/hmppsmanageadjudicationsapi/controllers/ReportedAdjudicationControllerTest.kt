package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

@WebMvcTest(value = [ReportedAdjudicationController::class])
class ReportedAdjudicationControllerTest : TestControllerBase() {

  @MockBean
  lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `responds with a unauthorised status code`() {
      makeGetAdjudicationRequest(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns the adjudication for a given id`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenReturn(
        ReportedAdjudicationDto(
          adjudicationNumber = 1,
          prisonerNumber = "A12345",
          bookingId = 123,
          incidentDetails = IncidentDetailsDto(
            locationId = 2,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES,
          offences = listOf(
            OffenceDto(
              offenceCode = 2,
              OffenceRuleDto(
                paragraphNumber = "3",
                paragraphDescription = "A paragraph description",
              )
            )
          ),
          incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
          createdByUserId = "A_SMITH",
          createdDateTime = REPORTED_DATE_TIME,
        )
      )
      makeGetAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.reportedAdjudication.adjudicationNumber").value(1))
      verify(reportedAdjudicationService).getReportedAdjudicationDetails(1)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with an not found status code`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenThrow(EntityNotFoundException::class.java)

      makeGetAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetAdjudicationRequest(
      adjudicationNumber: Long
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/$adjudicationNumber")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(reportedAdjudicationService.getMyReportedAdjudications(any(), any())).thenReturn(
        PageImpl(
          listOf(
            ReportedAdjudicationDto(
              adjudicationNumber = 1,
              prisonerNumber = "A12345",
              bookingId = 123,
              incidentDetails = IncidentDetailsDto(
                locationId = 2,
                dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
                handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
              ),
              incidentRole = INCIDENT_ROLE_WITH_ALL_VALUES,
              offences = listOf(
                OffenceDto(
                  offenceCode = 2,
                  OffenceRuleDto(
                    paragraphNumber = "3",
                    paragraphDescription = "A paragraph description",
                  )
                )
              ),
              incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
              createdByUserId = "A_SMITH",
              createdDateTime = REPORTED_DATE_TIME,
            )
          ),
          Pageable.ofSize(20).withPage(0),
          1
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `makes a call to return my reported adjudications`() {
      getMyAdjudications().andExpect(status().isOk)
      verify(reportedAdjudicationService).getMyReportedAdjudications(
        "MDI",
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
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.totalPages").value(1))
        .andExpect(jsonPath("$.size").value(20))
        .andExpect(jsonPath("$.number").value(0))
        .andExpect(jsonPath("$.content[0].adjudicationNumber").value(1))
    }

    @Test
    fun `paged responds with a unauthorised status code`() {
      getMyAdjudications().andExpect(status().isUnauthorized)
    }

    private fun getMyAdjudications(): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/my/agency/MDI?page=0&size=20&sort=incidentDate,DESC")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class CreateDraftFromReportedAdjudication {
    @BeforeEach
    fun beforeEach() {
      whenever(reportedAdjudicationService.createDraftFromReportedAdjudication(any())).thenReturn(
        DraftAdjudicationDto(
          id = 1,
          adjudicationNumber = 123L,
          prisonerNumber = "A12345",
          incidentDetails = IncidentDetailsDto(
            locationId = 2,
            dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
            handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE
          ),
          incidentRole = IncidentRoleDto(
            roleCode = "25a",
            associatedPrisonersNumber = "B2345BB"
          ),
          incidentStatement = IncidentStatementDto(statement = INCIDENT_STATEMENT),
        )
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeCreateDraftFromReportedAdjudicationRequest(123).andExpect(status().isUnauthorized)
    }

    private fun makeCreateDraftFromReportedAdjudicationRequest(
      adjudicationNumber: Long
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/$adjudicationNumber/create-draft-adjudication")
            .header("Content-Type", "application/json")
        )
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0, 0)
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private val INCIDENT_ROLE_WITH_ALL_VALUES = IncidentRoleDto("25a", "B23456")
    private const val INCIDENT_STATEMENT = "A statement"
  }
}
