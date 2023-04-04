package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftEvidenceService

@WebMvcTest(value = [DraftEvidenceController::class])
class DraftEvidenceControllerTest : TestControllerBase() {
  @MockBean
  lateinit var evidenceService: DraftEvidenceService

  @BeforeEach
  fun beforeEach() {
    whenever(
      evidenceService.setEvidence(
        ArgumentMatchers.anyLong(),
        any(),
      ),
    ).thenReturn(draftAdjudicationDto())
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setEvidenceRequest(1, EVIDENCE_REQUEST).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
  fun `makes a call to update the evidence`() {
    setEvidenceRequest(1, EVIDENCE_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isCreated)

    verify(evidenceService).setEvidence(1, EVIDENCE_REQUEST.evidence)
  }

  private fun setEvidenceRequest(
    id: Long,
    evidence: EvidenceRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(evidence)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/draft-adjudications/$id/evidence")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val EVIDENCE_REQUEST = EvidenceRequest(listOf(EvidenceRequestItem(code = EvidenceCode.PHOTO, details = "details")))
  }
}
