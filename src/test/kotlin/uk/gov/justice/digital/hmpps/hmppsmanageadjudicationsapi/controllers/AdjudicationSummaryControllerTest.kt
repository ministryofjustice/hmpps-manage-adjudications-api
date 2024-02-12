package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.SummaryAdjudicationService

@WebMvcTest(
  AdjudicationSummaryController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class AdjudicationSummaryControllerTest : TestControllerBase() {

  @MockBean
  lateinit var summaryAdjudicationService: SummaryAdjudicationService

  @Nested
  inner class Profile {
    @BeforeEach
    fun beforeEach() {
      whenever(
        summaryAdjudicationService.getAdjudicationSummary(
          any(),
          anyOrNull(),
          anyOrNull(),
        ),
      ).thenReturn(
        AdjudicationSummary(
          adjudicationCount = 1,
          awards = emptyList(),
          bookingId = 1,
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      profileRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to get the profile`() {
      profileRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(summaryAdjudicationService).getAdjudicationSummary(
        any(),
        anyOrNull(),
        anyOrNull(),
      )
    }

    private fun profileRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/adjudications/by-booking-id/1")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class HasAdjudications {
    @BeforeEach
    fun beforeEach() {
      whenever(summaryAdjudicationService.hasAdjudications(any()))
        .thenReturn(HasAdjudicationsResponse(true))
    }

    @Test
    fun `responds with a unauthorised status code`() {
      hasAdjudicationsRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to get the profile`() {
      hasAdjudicationsRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(summaryAdjudicationService).hasAdjudications(any())
    }

    private fun hasAdjudicationsRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/adjudications/booking/1/exists")
            .header("Content-Type", "application/json"),
        )
    }
  }
}
