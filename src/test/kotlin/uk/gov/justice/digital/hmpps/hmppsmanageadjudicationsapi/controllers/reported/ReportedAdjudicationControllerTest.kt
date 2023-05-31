package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService
import java.time.LocalDateTime

@WebMvcTest(
  ReportedAdjudicationController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class ReportedAdjudicationControllerTest : TestControllerBase() {

  @MockBean
  lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @MockBean
  lateinit var eventPublishService: EventPublishService

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `responds with a unauthorised status code`() {
      makeGetAdjudicationRequest(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `returns the adjudication for a given id`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenReturn(
        REPORTED_ADJUDICATION_DTO,
      )
      makeGetAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.reportedAdjudication.adjudicationNumber").value(1))
      verify(reportedAdjudicationService).getReportedAdjudicationDetails(1)
    }

    @Test
    @WithMockUser(username = "ITAG_USER")
    fun `responds with an not found status code`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyLong())).thenThrow(EntityNotFoundException::class.java)

      makeGetAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetAdjudicationRequest(
      adjudicationNumber: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          get("/reported-adjudications/$adjudicationNumber")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

    private fun makeReportedAdjudicationSetStatusRequest(
      adjudicationNumber: Long,
      body: Map<String, Any>,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/status")
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(body)),
        )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when the maximum details length has been exceeded`() {
      val largeStatement = IntRange(0, 4001).joinToString("") { "A" }
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to ReportedAdjudicationStatus.RETURNED, "statusDetails" to largeStatement),
      ).andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The details of why the status has been set exceeds the maximum character limit of 4000"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `returns a bad request when the maximum reason length has been exceeded`() {
      val largeStatement = IntRange(0, 128).joinToString("") { "A" }
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf(
          "status" to ReportedAdjudicationStatus.RETURNED,
          "statusReason" to largeStatement,
        ),
      ).andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The reason the status has been set exceeds the maximum character limit of 128"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `makes a call to set the status of the reported adjudication`() {
      whenever(reportedAdjudicationService.setStatus(123, ReportedAdjudicationStatus.RETURNED, "reason", "details")).thenReturn(REPORTED_ADJUDICATION_DTO)

      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to ReportedAdjudicationStatus.RETURNED, "statusReason" to "reason", "statusDetails" to "details"),
      )

      verify(eventPublishService).publishAdjudicationUpdate(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeReportedAdjudicationSetStatusRequest(123, mapOf("status" to ReportedAdjudicationStatus.RETURNED)).andExpect(
        status().isUnauthorized,
      )
    }
  }

  @Nested
  inner class Issued {

    private val now = LocalDateTime.now()

    private fun makeIssuedRequest(
      adjudicationNumber: Long,
      issuedRequest: IssueRequest,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/issue")
            .header("Content-Type", "application/json")
            .content(objectMapper.writeValueAsString(issuedRequest)),
        )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeIssuedRequest(1, IssueRequest(now))
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds successfully from issued details request `() {
      whenever(
        reportedAdjudicationService.setIssued(anyLong(), any()),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      makeIssuedRequest(1, IssueRequest(now))
        .andExpect(status().isOk)
      verify(reportedAdjudicationService).setIssued(1, now)
    }
  }
}
