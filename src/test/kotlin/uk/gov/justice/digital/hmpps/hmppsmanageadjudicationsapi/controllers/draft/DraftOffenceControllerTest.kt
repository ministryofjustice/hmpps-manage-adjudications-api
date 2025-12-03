package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService

@WebMvcTest(
  DraftOffenceController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DraftOffenceControllerTest : TestControllerBase() {
  @MockitoBean
  lateinit var incidentOffenceService: DraftOffenceService

  @Nested
  inner class SetOffenceDetails {

    @BeforeEach
    fun beforeEach() {
      whenever(incidentOffenceService.setOffenceDetails(ArgumentMatchers.anyLong(), any())).thenReturn(
        draftAdjudicationDto(),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to set the offence details to the draft adjudication`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(incidentOffenceService).setOffenceDetails(
        1,
        OffenceDetailsRequestItem(
          offenceCode = BASIC_OFFENCE_REQUEST.offenceCode,
        ),
      )
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `returns the draft adjudication including the new offence details`() {
      makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)
        .andExpect(MockMvcResultMatchers.jsonPath("$.draftAdjudication.id").isNumber)
        .andExpect(MockMvcResultMatchers.jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
        .andExpect(
          MockMvcResultMatchers.jsonPath("$.draftAdjudication.offenceDetails.offenceCode")
            .value(BASIC_OFFENCE_RESPONSE_DTO.offenceCode),
        )
    }

    private fun makeSetOffenceDetailsRequest(id: Long, offenceDetails: OffenceDetailsRequestItem): ResultActions {
      val body = objectMapper.writeValueAsString(mapOf("offenceDetails" to offenceDetails))

      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/draft-adjudications/$id/offence-details")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class GetOffenceRule {
    @BeforeEach
    fun beforeEach() {
      whenever(incidentOffenceService.getRule(any(), any(), any())).thenReturn(
        OffenceRuleDetailsDto(paragraphDescription = "", paragraphNumber = ""),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getOffenceRuleRequest()
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call to get offence rule`() {
      getOffenceRuleRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(incidentOffenceService, atLeastOnce()).getRule(any(), any(), any())
    }

    private fun getOffenceRuleRequest(): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.get("/draft-adjudications/offence-rule/1000?youthOffender=false")
          .header("Content-Type", "application/json"),
      )
  }

  @Nested
  inner class GetAllOffenceRules {

    @BeforeEach
    fun beforeEach() {
      whenever(incidentOffenceService.getRule(any(), any(), any())).thenReturn(
        OffenceRuleDetailsDto(paragraphDescription = "", paragraphNumber = ""),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getAllOffenceRulesRequest()
        .andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `makes a call get all offences rules`() {
      getAllOffenceRulesRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(incidentOffenceService, atLeastOnce()).getRules(any(), any(), any())
    }

    private fun getAllOffenceRulesRequest(): ResultActions = mockMvc
      .perform(
        MockMvcRequestBuilders.get("/draft-adjudications/offence-rules?youthOffender=false&version=1")
          .header("Content-Type", "application/json"),
      )
  }

  companion object {
    private val BASIC_OFFENCE_REQUEST = OffenceDetailsRequestItem(offenceCode = 3)
  }
}
