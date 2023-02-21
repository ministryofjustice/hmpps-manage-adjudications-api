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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService
import java.time.LocalDateTime

@WebMvcTest(value = [HearingController::class])
class HearingControllerV1Test : TestControllerBase() {

  @MockBean
  lateinit var hearingService: HearingService

  @MockBean
  lateinit var hearingOutcomeService: HearingOutcomeService

  @MockBean
  lateinit var referralService: ReferralService

  @Nested
  inner class CreateHearing {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingService.createHearingV1(
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
      verify(hearingService).createHearingV1(1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
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
        hearingService.deleteHearingV1(
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
      verify(hearingService).deleteHearingV1(1, 1)
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
        hearingService.amendHearingV1(
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
      verify(hearingService).amendHearingV1(1, 1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
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

  companion object {
    private val HEARING_REQUEST = HearingRequest(locationId = 1L, dateTimeOfHearing = LocalDateTime.now(), oicHearingType = OicHearingType.GOV)
  }
}
