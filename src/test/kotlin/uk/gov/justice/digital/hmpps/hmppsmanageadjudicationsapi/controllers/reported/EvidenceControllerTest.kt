package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.EvidenceService

@WebMvcTest(
  EvidenceController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class EvidenceControllerTest : TestControllerBase() {

  @MockBean
  lateinit var evidenceService: EvidenceService

  @BeforeEach
  fun beforeEach() {
    whenever(
      evidenceService.updateEvidence(
        ArgumentMatchers.anyString(),
        any(),
      ),
    ).thenReturn(REPORTED_ADJUDICATION_DTO)
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setEvidenceRequest(
      1,
      EVIDENCE_REQUEST,
    ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
  fun `makes a call to set the evidence`() {
    setEvidenceRequest(1, EVIDENCE_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(evidenceService).updateEvidence("1", EVIDENCE_REQUEST.evidence)
  }

  private fun setEvidenceRequest(
    id: Long,
    evidence: EvidenceRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(evidence)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/reported-adjudications/$id/evidence/edit")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val EVIDENCE_REQUEST = EvidenceRequest(listOf(EvidenceRequestItem(EvidenceCode.PHOTO, "identifier", "details")))
  }
}
