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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessesRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.WitnessesService

@WebMvcTest(
  WitnessesController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class WitnessesControllerTest : TestControllerBase() {

  @MockBean
  lateinit var witnessesService: WitnessesService

  @BeforeEach
  fun beforeEach() {
    whenever(
      witnessesService.updateWitnesses(
        ArgumentMatchers.anyString(),
        any(),
      ),
    ).thenReturn(REPORTED_ADJUDICATION_DTO)
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setWitnessesRequest(
      1,
      WITNESSES_REQUEST,
    ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
  fun `makes a call to set the witnesses`() {
    setWitnessesRequest(1, WITNESSES_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(witnessesService).updateWitnesses("1", WITNESSES_REQUEST.witnesses)
  }

  private fun setWitnessesRequest(
    id: Long,
    witnesses: WitnessesRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(witnesses)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/reported-adjudications/$id/witnesses/edit")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val WITNESSES_REQUEST = WitnessesRequest(listOf(WitnessRequestItem(WitnessCode.STAFF, "first", "last")))
  }
}
