package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService

@WebMvcTest(
  DraftAdjudicationWorkflowController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DraftAdjudicationWorkflowControllerTest : TestControllerBase() {
  @MockBean
  lateinit var adjudicationWorkflowService: AdjudicationWorkflowService

  @MockBean
  lateinit var eventPublishService: EventPublishService

  @Nested
  inner class CompleteDraft {
    @Test
    fun `responds with a unauthorised status code`() {
      completeDraftAdjudication(1)
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `make a call to complete a draft adjudication`() {
      whenever(adjudicationWorkflowService.completeDraftAdjudication(1)).thenReturn(REPORTED_ADJUDICATION_DTO)
      completeDraftAdjudication(1)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(eventPublishService).publishAdjudicationCreation(REPORTED_ADJUDICATION_DTO)
    }

    private fun completeDraftAdjudication(id: Long): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.post("/draft-adjudications/$id/complete-draft-adjudication")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class AloOffenceEdit {
    @Test
    fun `responds with a unauthorised status code`() {
      makeAloSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a unauthorised status code as missing ALO role`() {
      makeAloSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a unauthorised status code as missing write role`() {
      makeAloSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to set the offence details`() {
      whenever(
        adjudicationWorkflowService.setOffenceDetailsAndCompleteDraft(
          1,
          OffenceDetailsRequestItem(
            offenceCode = BASIC_OFFENCE_REQUEST.offenceCode,
          ),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      makeAloSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(eventPublishService).publishAdjudicationUpdate(REPORTED_ADJUDICATION_DTO)
    }

    private fun makeAloSetOffenceDetailsRequest(id: Long, offenceDetails: OffenceDetailsRequestItem): ResultActions {
      val body = objectMapper.writeValueAsString(mapOf("offenceDetails" to offenceDetails))

      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/draft-adjudications/$id/alo-offence-details")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }
  companion object {
    private val BASIC_OFFENCE_REQUEST = OffenceDetailsRequestItem(offenceCode = 3)
  }
}
