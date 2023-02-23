package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(value = [HearingController::class])
class HearingControllerTest : TestControllerBase() {

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
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/v2")
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
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteHearingRequest(1,).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteHearingRequest(1,).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteHearingRequest(1,).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete a hearing`() {
      deleteHearingRequest(1,)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).deleteHearing(1,)
    }

    private fun deleteHearingRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/hearing/v2")
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
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      amendHearingRequest(
        1,
        HEARING_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a hearing`() {
      amendHearingRequest(1, HEARING_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(hearingService).amendHearing(1, HEARING_REQUEST.locationId, HEARING_REQUEST.dateTimeOfHearing, HEARING_REQUEST.oicHearingType)
    }

    private fun amendHearingRequest(
      id: Long,
      hearing: HearingRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearing)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/hearing/v2")
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
        hearingOutcomeService.createHearingOutcome(
          ArgumentMatchers.anyLong(),
          any(),
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        referralService.createReferral(
          ArgumentMatchers.anyLong(),
          any(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createHearingOutcomeRequest(
        1,
        hearingOutcomeRequest()
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createHearingOutcomeRequest(
        1,
        hearingOutcomeRequest()
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns bad request if adjudicator not set when required`() {
      createHearingOutcomeRequest(
        1,
        NO_ADJUDICATOR_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns bad request if details not set when required`() {
      createHearingOutcomeRequest(
        1,
        NO_DETAILS_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "COMPLETE", "ADJOURN")
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a hearing outcome`(code: HearingOutcomeCode) {
      createHearingOutcomeRequest(1, hearingOutcomeRequest(code))
        .andExpect(MockMvcResultMatchers.status().isCreated)
      if (code.outcomeCode != null) verify(referralService).createReferral(
        adjudicationNumber = 1,
        code = code,
        adjudicator = "test",
        details = "details"
      )
      else verify(hearingOutcomeService).createHearingOutcome(
        adjudicationNumber = 1,
        code = code,
        adjudicator = "test",
        reason = null,
        details = "details"
      )
    }

    private fun createHearingOutcomeRequest(
      id: Long,
      hearingOutcome: HearingOutcomeRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearingOutcome)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/hearing/outcome")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class UpdateHearingOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        hearingOutcomeService.updateHearingOutcome(
          ArgumentMatchers.anyLong(),
          any(),
          any(),
          anyOrNull(),
          anyOrNull(),
          anyOrNull(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        referralService.updateReferral(
          ArgumentMatchers.anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      updateHearingOutcomeRequest(
        1,
        hearingOutcomeRequest()
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      updateHearingOutcomeRequest(
        1,
        hearingOutcomeRequest()
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns bad request if adjudicator not set when required`() {
      updateHearingOutcomeRequest(
        1,
        NO_ADJUDICATOR_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns bad request if details not set when required`() {
      updateHearingOutcomeRequest(
        1,
        NO_DETAILS_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isBadRequest)
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "COMPLETE", "ADJOURN")
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to update a hearing outcome`(code: HearingOutcomeCode) {
      updateHearingOutcomeRequest(1, hearingOutcomeRequest(code))
        .andExpect(MockMvcResultMatchers.status().isOk)
      if (code.outcomeCode != null) verify(referralService).updateReferral(
        adjudicationNumber = 1,
        code = code,
        details = "details"
      )
      else verify(hearingOutcomeService).updateHearingOutcome(
        adjudicationNumber = 1,
        code = code,
        adjudicator = "test",
        details = "details"
      )
    }

    private fun updateHearingOutcomeRequest(
      id: Long,
      hearingOutcome: HearingOutcomeRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(hearingOutcome)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/hearing/outcome")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }
  companion object {
    private val HEARING_REQUEST = HearingRequest(locationId = 1L, dateTimeOfHearing = LocalDateTime.now(), oicHearingType = OicHearingType.GOV)
    private fun hearingOutcomeRequest(code: HearingOutcomeCode? = HearingOutcomeCode.REFER_POLICE) = HearingOutcomeRequest(adjudicator = "test", code = code!!, details = "details")
    private val NO_ADJUDICATOR_REQUEST = HearingOutcomeRequest(
      code = HearingOutcomeCode.ADJOURN
    )
    private val NO_DETAILS_REQUEST = HearingOutcomeRequest(
      code = HearingOutcomeCode.REFER_INAD
    )

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
