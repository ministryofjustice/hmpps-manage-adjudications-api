package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService
import java.time.LocalDateTime

@WebMvcTest(
  ReportedAdjudicationController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class ReportedAdjudicationControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `responds with a unauthorised status code`() {
      makeGetAdjudicationRequest(1)
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `returns the adjudication for a given id`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyString(), any())).thenReturn(
        REPORTED_ADJUDICATION_DTO,
      )
      makeGetAdjudicationRequest(1)
        .andExpect(status().isOk)
        .andExpect(jsonPath("$.reportedAdjudication.chargeNumber").value("1"))
      verify(reportedAdjudicationService).getReportedAdjudicationDetails("1", false)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `responds with an not found status code`() {
      whenever(reportedAdjudicationService.getReportedAdjudicationDetails(anyString(), any())).thenThrow(
        EntityNotFoundException::class.java,
      )

      makeGetAdjudicationRequest(1).andExpect(status().isNotFound)
    }

    private fun makeGetAdjudicationRequest(
      adjudicationNumber: Long,
    ): ResultActions = mockMvc
      .perform(
        get("/reported-adjudications/$adjudicationNumber/v2")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

    private fun makeReportedAdjudicationSetStatusRequest(
      adjudicationNumber: Long,
      body: Map<String, Any>,
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/status")
          .header("Content-Type", "application/json")
          .content(objectMapper.writeValueAsString(body)),
      )

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `returns a bad request when the maximum details length has been exceeded`() {
      val largeStatement = IntRange(0, 4001).joinToString("") { "A" }
      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to ReportedAdjudicationStatus.RETURNED, "statusDetails" to largeStatement),
      ).andExpect(status().isBadRequest)
        .andExpect(jsonPath("$.userMessage").value("The details of why the status has been set exceeds the maximum character limit of 4000"))
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
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
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `does not call event service on exception`() {
      whenever(
        reportedAdjudicationService.setStatus(
          "123",
          ReportedAdjudicationStatus.RETURNED,
          "reason",
          "details",
        ),
      ).thenThrow(RuntimeException())

      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf(
          "status" to ReportedAdjudicationStatus.RETURNED,
          "statusReason" to "reason",
          "statusDetails" to "details",
        ),
      )

      verify(eventPublishService, never()).publishEvent(any(), any())
    }

    @CsvSource("RETURNED,false", "REJECTED,false", "UNSCHEDULED,true")
    @ParameterizedTest
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to set the status of the reported adjudication`(
      status: ReportedAdjudicationStatus,
      eventCalled: Boolean,
    ) {
      whenever(reportedAdjudicationService.setStatus("123", status, "reason", "details")).thenReturn(
        reportedAdjudicationDto(status),
      )

      makeReportedAdjudicationSetStatusRequest(
        123,
        mapOf("status" to status, "statusReason" to "reason", "statusDetails" to "details"),
      )

      val verificationMode = if (eventCalled) atLeastOnce() else never()

      verify(eventPublishService, verificationMode).publishEvent(
        AdjudicationDomainEventType.ADJUDICATION_CREATED,
        reportedAdjudicationDto((status)),
      )
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
    ): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/issue")
          .header("Content-Type", "application/json")
          .content(objectMapper.writeValueAsString(issuedRequest)),
      )

    @Test
    fun `responds with a unauthorised status code`() {
      makeIssuedRequest(1, IssueRequest(now))
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `responds successfully from issued details request `() {
      whenever(
        reportedAdjudicationService.setIssued(anyString(), any()),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      makeIssuedRequest(1, IssueRequest(now))
        .andExpect(status().isOk)
      verify(reportedAdjudicationService).setIssued("1", now)
    }
  }

  @Nested
  inner class SetCreatedOnBehalfOf {
    @Test
    fun `responds with a unauthorised status code`() {
      setCreatedOnBehalfOfRequest("1", "offender", "some reason")
        .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to set the created on behalf of`() {
      whenever(
        reportedAdjudicationService.setCreatedOnBehalfOf(
          anyString(),
          anyString(),
          anyString(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)

      setCreatedOnBehalfOfRequest("1", "offender", "some reason")
        .andExpect(status().isOk)

      verify(reportedAdjudicationService).setCreatedOnBehalfOf(
        "1",
        "offender",
        "some reason",
      )
    }

    private fun setCreatedOnBehalfOfRequest(
      id: String,
      createdOnBehalfOfOfficer: String,
      createdOnBehalfOfReason: String,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        mapOf(
          "createdOnBehalfOfOfficer" to createdOnBehalfOfOfficer,
          "createdOnBehalfOfReason" to createdOnBehalfOfReason,
        ),
      )
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/created-on-behalf-of")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }
}
