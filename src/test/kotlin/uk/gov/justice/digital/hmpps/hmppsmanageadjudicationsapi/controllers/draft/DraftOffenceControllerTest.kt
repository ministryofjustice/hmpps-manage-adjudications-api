package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService

@WebMvcTest(
  DraftOffenceController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DraftOffenceControllerTest : TestControllerBase() {
  @MockBean
  lateinit var incidentOffenceService: DraftOffenceService

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
  @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
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
  @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
  fun `returns the draft adjudication including the new offence details`() {
    makeSetOffenceDetailsRequest(1, BASIC_OFFENCE_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isCreated)
      .andExpect(MockMvcResultMatchers.jsonPath("$.draftAdjudication.id").isNumber)
      .andExpect(MockMvcResultMatchers.jsonPath("$.draftAdjudication.prisonerNumber").value("A12345"))
      .andExpect(MockMvcResultMatchers.jsonPath("$.draftAdjudication.offenceDetails.offenceCode").value(BASIC_OFFENCE_RESPONSE_DTO.offenceCode))
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

  companion object {
    private val BASIC_OFFENCE_REQUEST = OffenceDetailsRequestItem(offenceCode = 3)
  }
}
