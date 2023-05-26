package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@WebMvcTest(
  OutcomeController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class OutcomeControllerTest : TestControllerBase() {

  @MockBean
  lateinit var outcomeService: OutcomeService

  @MockBean
  lateinit var referralService: ReferralService

  @MockBean
  lateinit var completedHearingService: CompletedHearingService

  @Nested
  inner class CreateProsecution {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createProsecution(
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`() {
      createOutcomeRequest(1)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createProsecution("1")
    }

    private fun createOutcomeRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/prosecution")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class CreateNotProceed {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createNotProceed(
          anyString(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
        NOT_PROCEED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`() {
      createOutcomeRequest(
        1,
        NOT_PROCEED_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createNotProceed("1", NotProceedReason.NOT_FAIR, "details")
    }

    private fun createOutcomeRequest(
      id: Long,
      notProceedRequest: NotProceedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(notProceedRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/not-proceed")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CreateReferPolice {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createReferral(
          anyString(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`() {
      createOutcomeRequest(
        1,
        POLICE_REFER_REQUEST,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(outcomeService).createReferral("1", OutcomeCode.REFER_POLICE, "details")
    }

    private fun createOutcomeRequest(
      id: Long,
      policeReferralRequest: PoliceReferralRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(policeReferralRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/refer-police")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class CreateHearingCompletedOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.createNotProceed(
          anyString(),
          any(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      whenever(
        completedHearingService.createDismissed(
          anyString(),
          any(),
          any(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    fun `responds with a unauthorised status code`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
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
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
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
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @CsvSource("NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create an outcome`(code: OutcomeCode) {
      createOutcomeRequest(
        1,
        code,
        if (code != OutcomeCode.NOT_PROCEED) COMPLETED_DISMISSED_REQUEST else null,
        if (code == OutcomeCode.NOT_PROCEED) COMPLETED_NOT_PROCEED_REQUEST else null,
      )
        .andExpect(MockMvcResultMatchers.status().isCreated)

      when (code) {
        OutcomeCode.DISMISSED -> verify(completedHearingService).createDismissed(
          "1",
          "test",
          HearingOutcomePlea.UNFIT,
          "details",
        )
        OutcomeCode.NOT_PROCEED -> verify(completedHearingService).createNotProceed(
          "1",
          "test",
          HearingOutcomePlea.UNFIT,
          NotProceedReason.NOT_FAIR,
          "details",
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
            .content(body),
        )
    }
  }

  @Nested
  inner class RemoveReferral {
    @BeforeEach
    fun beforeEach() {
      whenever(
        referralService.removeReferral(
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      removeReferralRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to remove a referral`() {
      removeReferralRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(referralService).removeReferral("1")
    }

    private fun removeReferralRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/remove-referral")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class DeleteOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.deleteOutcome(
          anyString(),
          anyOrNull(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deleteOutcomeRequest(
        1,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deleteOutcomeRequest(
        1,
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
        "1",
      )
    }

    private fun deleteOutcomeRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/outcome")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class CreateChargeProved {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.createChargeProved(
          anyString(),
          any(),
          any(),
          anyOrNull(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createChargeProvedRequest(
        1,
        CHARGE_PROVED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a proven outcome`() {
      createChargeProvedRequest(1, CHARGE_PROVED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
      verify(completedHearingService).createChargeProved(
        "1",
        CHARGE_PROVED_REQUEST.adjudicator,
        CHARGE_PROVED_REQUEST.plea,
        CHARGE_PROVED_REQUEST.amount,
        CHARGE_PROVED_REQUEST.caution,
      )
    }

    private fun createChargeProvedRequest(
      id: Long,
      proven: HearingCompletedChargeProvedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(proven)

      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/complete-hearing/charge-proved")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class RemoveCompletedHearingOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        completedHearingService.removeOutcome(
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      removeCompletedHearingOutcomeRequest(1).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to remove a completed hearing`() {
      removeCompletedHearingOutcomeRequest(1)
        .andExpect(MockMvcResultMatchers.status().isOk)
      verify(completedHearingService).removeOutcome("1")
    }

    private fun removeCompletedHearingOutcomeRequest(
      id: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$id/remove-completed-hearing")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class CreateQuashed {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.createQuashed(
          anyString(),
          any(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createQuashedRequest(
        1,
        QUASHED_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a quashed outcome`() {
      createQuashedRequest(1, QUASHED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(outcomeService).createQuashed("1", QUASHED_REQUEST.reason, QUASHED_REQUEST.details)
    }

    private fun createQuashedRequest(
      id: Long,
      request: QuashedRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(request)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/outcome/quashed")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class AmendOutcome {
    @BeforeEach
    fun beforeEach() {
      whenever(
        outcomeService.amendOutcomeViaApi(
          anyString(),
          any(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      amendOutcomeRequest(
        1,
        AMEND_REFER_POLICE_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a not proceed outcome`() {
      amendOutcomeRequest(1, AMEND_NOT_PROCEED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        adjudicationNumber = "1",
        details = "details",
        reason = AMEND_NOT_PROCEED_REQUEST.reason,
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a refer police outcome`() {
      amendOutcomeRequest(1, AMEND_REFER_POLICE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        adjudicationNumber = "1",
        details = "details",
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to amend a quashed outcome`() {
      amendOutcomeRequest(1, AMEND_QUASHED_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(outcomeService).amendOutcomeViaApi(
        adjudicationNumber = "1",
        details = "details",
        quashedReason = AMEND_QUASHED_REQUEST.quashedReason,
      )
    }

    private fun amendOutcomeRequest(
      id: Long,
      request: AmendOutcomeRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(request)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/outcome")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    private val POLICE_REFER_REQUEST = PoliceReferralRequest(details = "details")
    private val NOT_PROCEED_REQUEST = NotProceedRequest(reason = NotProceedReason.NOT_FAIR, details = "details")
    private val COMPLETED_NOT_PROCEED_REQUEST = HearingCompletedNotProceedRequest(adjudicator = "test", plea = HearingOutcomePlea.UNFIT, reason = NotProceedReason.NOT_FAIR, details = "details")
    private val COMPLETED_DISMISSED_REQUEST = HearingCompletedDismissedRequest(adjudicator = "test", plea = HearingOutcomePlea.UNFIT, details = "details")
    private val CHARGE_PROVED_REQUEST = HearingCompletedChargeProvedRequest(adjudicator = "test", plea = HearingOutcomePlea.GUILTY, caution = false)
    private val QUASHED_REQUEST = QuashedRequest(reason = QuashedReason.APPEAL_UPHELD, details = "details")
    private val AMEND_REFER_POLICE_REQUEST = AmendOutcomeRequest(details = "details")
    private val AMEND_NOT_PROCEED_REQUEST = AmendOutcomeRequest(details = "details", reason = NotProceedReason.NOT_FAIR)
    private val AMEND_QUASHED_REQUEST = AmendOutcomeRequest(details = "details", quashedReason = QuashedReason.JUDICIAL_REVIEW)
  }
}
