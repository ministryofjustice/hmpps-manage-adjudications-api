package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(value = [HearingController::class])
class HearingControllerTest : TestControllerBase() {

  @MockBean
  lateinit var hearingService: HearingService

  @Nested
  inner class CreateHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.createHearing(
          ArgumentMatchers.anyLong(),
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing`() {
      createHearingRequest(1, HEARING_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(hearingService).createHearing(1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
    }

    private fun createHearingRequest(
      id: Long,
      hearing: HearingRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class DeleteHearing {

    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.deleteHearing(
          ArgumentMatchers.anyLong(),
          ArgumentMatchers.anyLong(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteHearingRequest(1, 1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteHearingRequest(1, 1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteHearingRequest(1, 1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete a hearing`() {
      deleteHearingRequest(1, 1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).deleteHearing(1, 1)
    }

    private fun deleteHearingRequest(
      id: Long,
      hearingId: Long
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/hearing/$hearingId")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class AmendHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.amendHearing(
          ArgumentMatchers.anyLong(),
          ArgumentMatchers.anyLong(),
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      amendHearingRequest(
        1, 1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendHearingRequest(
        1, 1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      amendHearingRequest(
        1, 1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a hearing`() {
      amendHearingRequest(1, 1, HEARING_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).amendHearing(1, 1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
    }

    private fun amendHearingRequest(
      id: Long,
      hearingId: Long,
      hearing: HearingRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/hearing/$hearingId")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class AllHearings {

    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.getAllHearingsByAgencyIdAndDate(
          ArgumentMatchers.anyString(),
          any(),
        )
      ).thenReturn(ALL_HEARINGS_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      allHearingsRequest("MDI", LocalDate.now()).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with a forbidden status code for non ALO`() {
      allHearingsRequest("MDI", LocalDate.now()).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `get all hearings for agency `() {
      val now = LocalDate.now()
      allHearingsRequest("MDI", now)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).getAllHearingsByAgencyIdAndDate("MDI", now)
    }

    private fun allHearingsRequest(
      agency: String,
      date: LocalDate,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/hearings/agency/$agency?hearingDate=$date")
            .header("Content-Type", "application/json")
        )
    }
  }

  @Nested
  inner class CreateHearingOutcome {

    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.createHearingOutcome(
          ArgumentMatchers.anyLong(),
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createHearingOutcomeRequest(
        1,
        1,
        HEARING_OUTCOME_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createHearingOutcomeRequest(
        1,
        1,
        HEARING_OUTCOME_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing`() {
      createHearingOutcomeRequest(1, 1, HEARING_OUTCOME_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(hearingService).createHearingOutcome(1, 1, "test", HearingOutcomeCode.REFER_POLICE)
    }

    private fun createHearingOutcomeRequest(
      id: Long,
      hearingId: Long,
      hearingOutcome: HearingOutcomeRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearingOutcome)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/$hearingId/outcome")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  companion object {
    private val HEARING_REQUEST = HearingRequest(locationId = 1L, dateTimeOfHearing = LocalDateTime.now(), oicHearingType = OicHearingType.GOV)
    private val HEARING_OUTCOME_REQUEST = HearingOutcomeRequest(adjudicator = "test", code = HearingOutcomeCode.REFER_POLICE)

    private val ALL_HEARINGS_DTO =
      listOf(
        HearingSummaryDto(
          id = 1,
          dateTimeOfHearing = LocalDateTime.now(),
          dateTimeOfDiscovery = LocalDateTime.now(),
          adjudicationNumber = 123,
          prisonerNumber = "123",
          oicHearingType = OicHearingType.GOV,
        )
      )
  }
}
