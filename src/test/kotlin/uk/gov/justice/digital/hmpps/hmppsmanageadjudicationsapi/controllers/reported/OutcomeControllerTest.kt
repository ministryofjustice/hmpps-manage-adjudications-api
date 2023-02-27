package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyLong
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@WebMvcTest(value = [OutcomeController::class])
class OutcomeControllerTest : TestControllerBase() {

  @MockBean
  lateinit var outcomeService: OutcomeService

  @MockBean
  lateinit var referralService: ReferralService

  @MockBean
  lateinit var completedHearingService: CompletedHearingService

  @Nested
  inner class CreateOutcome {

    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createNotProceed(
          anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        outcomeService.createProsecution(
          anyLong(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        outcomeService.createReferral(
          anyLong(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @CsvSource("REFER_POLICE", "PROSECUTION", "NOT_PROCEED")
    @ParameterizedTest
    fun `responds with a unauthorised status code`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) OUTCOME_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @CsvSource("REFER_POLICE", "PROSECUTION", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) OUTCOME_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("REFER_POLICE", "PROSECUTION", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) OUTCOME_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("REFER_POLICE", "PROSECUTION", "NOT_PROCEED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`(code: OutcomeCode) {
      createOutcomeRequest(
        1, code,
        if (code != OutcomeCode.NOT_PROCEED) OUTCOME_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) NOT_PROCEED_REQUEST else null
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      when (code) {
        OutcomeCode.REFER_POLICE -> verify(outcomeService).createReferral(
          1, OutcomeCode.REFER_POLICE, "details"
        )
        OutcomeCode.NOT_PROCEED -> verify(outcomeService).createNotProceed(
          1, NotProceedReason.NOT_FAIR, "details"
        )
        OutcomeCode.PROSECUTION -> verify(outcomeService).createProsecution(
          1, "details"
        )

        else -> {}
      }
    }

    private fun createOutcomeRequest(
      id: Long,
      code: OutcomeCode,
      outcomeRequest: OutcomeRequest? = null,
      notProceedRequest: NotProceedRequest? = null,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(outcomeRequest ?: notProceedRequest)
      val path = when (code) {
        OutcomeCode.REFER_POLICE -> "refer-police"
        OutcomeCode.NOT_PROCEED -> "not-proceed"
        OutcomeCode.PROSECUTION -> "prosecution"
        else -> ""
      }
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/$path")
            .header("Content-Type", "application/json")
            .content(body)
        )
    }
  }

  @Nested
  inner class CreateHearingCompletedOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.createNotProceed(
          anyLong(),
          any(),
          any(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        completedHearingService.createDismissed(
          anyLong(),
          any(),
          any(),
          any(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    fun `responds with a unauthorised status code`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`(code: OutcomeCode) {
      createOutcomeRequest(
        1, code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      when (code) {
        OutcomeCode.DISMISSED -> verify(completedHearingService).createDismissed(
          1, "test", HearingOutcomePlea.UNFIT, "details"
        )
        OutcomeCode.NOT_PROCEED -> verify(completedHearingService).createNotProceed(
          1, "test", HearingOutcomePlea.UNFIT, NotProceedReason.NOT_FAIR, "details"
        )

        else -> {}
      }
    }

    private fun createOutcomeRequest(
      id: Long,
      code: OutcomeCode,
      dismissed: HearingCompletedDismissedRequest? = null,
      notProceed: HearingCompletedNotProceedRequest? = null,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(notProceed ?: dismissed)
      val path = when (code) {
        OutcomeCode.NOT_PROCEED -> "not-proceed"
        OutcomeCode.DISMISSED -> "dismissed"
        else -> ""
      }
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/complete-hearing/$path")
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
          anyLong(),
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
    fun `makes a call to remove a referral`() {
      removeReferralRequest(1,)
        .andExpect(MockMvcResultMatchers.status().isOk)
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

  @Nested
  inner class DeleteOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.deleteOutcome(
          anyLong(),
          anyOrNull(),
        )
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteOutcomeRequest(
        1
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteOutcomeRequest(
        1
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deleteOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete an outcome`() {
      deleteOutcomeRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(outcomeService).deleteOutcome(
        1,
      )
    }

    private fun deleteOutcomeRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/outcome/not-proceed")
            .header("Content-Type", "application/json")
        )
    }
  }
  companion object {
    private val OUTCOME_REQUEST = OutcomeRequest(details = "details")
    private val NOT_PROCEED_REQUEST = NotProceedRequest(reason = NotProceedReason.NOT_FAIR, details = "details")

    private val COMPLETED_NOT_PROCEED_REQUEST = HearingCompletedNotProceedRequest(adjudicator = "test", plea = HearingOutcomePlea.UNFIT, reason = NotProceedReason.NOT_FAIR, details = "details")
    private val COMPLETED_DISMISSED_REQUEST = HearingCompletedDismissedRequest(adjudicator = "test", plea = HearingOutcomePlea.UNFIT, details = "details")
  }
}
