package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService

@WebMvcTest(value = [DraftAdjudicationWorkflowController::class])
class DraftAdjudicationWorkflowControllerTest : TestControllerBase() {
  @MockBean
  lateinit var adjudicationWorkflowService: AdjudicationWorkflowService

  @Test
  fun `responds with a unauthorised status code`() {
    completeDraftAdjudication(1)
      .andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
  fun `make a call to complete a draft adjudication`() {
    completeDraftAdjudication(1)
      .andExpect(MockMvcResultMatchers.status().isCreated)

    verify(adjudicationWorkflowService).completeDraftAdjudication(1)
  }

  fun completeDraftAdjudication(id: Long): ResultActions = mockMvc
    .perform(
      MockMvcRequestBuilders.post("/draft-adjudications/$id/complete-draft-adjudication")
        .header("Content-Type", "application/json"),
    )
}
