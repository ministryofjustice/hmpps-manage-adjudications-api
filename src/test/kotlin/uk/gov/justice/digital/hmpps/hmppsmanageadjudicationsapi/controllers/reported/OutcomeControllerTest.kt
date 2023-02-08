package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@WebMvcTest(value = [OutcomeController::class])
class OutcomeControllerTest : TestControllerBase() {

  @MockBean
  lateinit var outcomeService: OutcomeService

  @MockBean
  lateinit var referralService: ReferralService

  @Nested
  inner class CreateOutcome {

    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createOutcome(
          ArgumentMatchers.anyLong(),
          any(),
          anyOrNull(),
          anyOrNull(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
        OUTCOME_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
        OUTCOME_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
        OUTCOME_REQUEST
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`() {
      createOutcomeRequest(1, OUTCOME_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(outcomeService).createOutcome(
        1,
        OutcomeCode.REFER_POLICE,
      )
    }

    private fun createOutcomeRequest(
      id: Long,
      outcome: OutcomeRequest?
    ): ResultActions {
      val body = objectMapper.writeValueAsString(outcome)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class RemoveReferral {
    @BeforeEach
    fun beforeEach() {
      whenever(
        referralService.removeReferral(
          ArgumentMatchers.anyLong(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      removeReferralRequest(1,).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      removeReferralRequest(1,).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      removeReferralRequest(1,).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to reemove a referral`() {
      removeReferralRequest(1)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(referralService).removeReferral(1,)
    }

    private fun removeReferralRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/remove-referral")
            .header("Content-Type", "application/json")
        )
    }
  }
  companion object {
    private val OUTCOME_REQUEST = OutcomeRequest(code = OutcomeCode.REFER_POLICE)
  }
}
