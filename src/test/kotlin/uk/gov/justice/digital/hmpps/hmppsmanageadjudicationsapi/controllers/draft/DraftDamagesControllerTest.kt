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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftDamagesService

@WebMvcTest(
  DraftDamagesController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DraftDamagesControllerTest : TestControllerBase() {
  @MockitoBean
  lateinit var damagesService: DraftDamagesService

  @BeforeEach
  fun beforeEach() {
    whenever(
      damagesService.setDamages(
        ArgumentMatchers.anyLong(),
        any(),
      ),
    ).thenReturn(draftAdjudicationDto())
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setDamagesRequest(1, DAMAGES_REQUEST).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
  fun `makes a call to set the damages`() {
    setDamagesRequest(1, DAMAGES_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isCreated)

    verify(damagesService).setDamages(1, DAMAGES_REQUEST.damages)
  }

  private fun setDamagesRequest(
    id: Long,
    damages: DamagesRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(damages)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/draft-adjudications/$id/damages")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val DAMAGES_REQUEST = DamagesRequest(listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
  }
}
