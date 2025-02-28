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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftWitnessesService

@WebMvcTest(
  DraftWitnessesController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DraftWitnessesControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var witnessesService: DraftWitnessesService

  @BeforeEach
  fun beforeEach() {
    whenever(
      witnessesService.setWitnesses(
        ArgumentMatchers.anyLong(),
        any(),
      ),
    ).thenReturn(draftAdjudicationDto())
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setWitnessesRequest(1, WITNESSES_REQUEST).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
  fun `makes a call to update the witnesses`() {
    setWitnessesRequest(1, WITNESSES_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isCreated)

    verify(witnessesService).setWitnesses(1, WITNESSES_REQUEST.witnesses)
  }

  private fun setWitnessesRequest(
    id: Long,
    witnesses: WitnessesRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(witnesses)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/draft-adjudications/$id/witnesses")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val WITNESSES_REQUEST = WitnessesRequest(
      listOf(
        WitnessRequestItem(
          code = WitnessCode.OFFICER,
          firstName = "prison",
          lastName = "officer",
        ),
      ),
    )
  }
}
