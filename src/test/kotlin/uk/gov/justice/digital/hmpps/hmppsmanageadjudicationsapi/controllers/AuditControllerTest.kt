package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AuditService

@WebMvcTest(value = [AuditController::class])
class AuditControllerTest : TestControllerBase() {

  @MockBean
  lateinit var auditService: AuditService

  private fun auditRequest(url: String): ResultActions {
    return mockMvc
      .perform(
        MockMvcRequestBuilders.get(url)
          .header("Content-Type", "application/json")
      )
  }

  @Test
  fun `responds with a unauthorised status code for each report`() {
    auditRequest("/adjudications-audit/draft").andExpect(MockMvcResultMatchers.status().isUnauthorized)
    auditRequest("/adjudications-audit/reported").andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @WithMockUser(username = "ITAG_AUDIT_USER", authorities = ["ADJUDICATIONS_AUDIT"])
  @Test
  fun `get draft adjudications report`() {
    auditRequest("/adjudications-audit/draft").andExpect(MockMvcResultMatchers.status().isOk)
    verify(auditService).getDraftAdjudicationReport(any(), anyOrNull())
  }

  @WithMockUser(username = "ITAG_AUDIT_USER", authorities = ["ADJUDICATIONS_AUDIT"])
  @Test
  fun `get reported adjudications report`() {
    auditRequest("/adjudications-audit/reported").andExpect(MockMvcResultMatchers.status().isOk)
    verify(auditService).getReportedAdjudicationReport(any(), anyOrNull())
  }
}
