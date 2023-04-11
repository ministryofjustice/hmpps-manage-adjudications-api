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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamagesRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.DamagesService

@WebMvcTest(
  DamagesController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class DamagesControllerTest : TestControllerBase() {

  @MockBean
  lateinit var damagesService: DamagesService

  @BeforeEach
  fun beforeEach() {
    whenever(
      damagesService.updateDamages(
        ArgumentMatchers.anyLong(),
        any(),
      ),
    ).thenReturn(REPORTED_ADJUDICATION_DTO)
  }

  @Test
  fun `responds with a unauthorised status code`() {
    setDamagesRequest(1, DAMAGES_REQUEST).andExpect(MockMvcResultMatchers.status().isUnauthorized)
  }

  @Test
  @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
  fun `makes a call to set the damages`() {
    setDamagesRequest(1, DAMAGES_REQUEST)
      .andExpect(MockMvcResultMatchers.status().isOk)

    verify(damagesService).updateDamages(1, DAMAGES_REQUEST.damages)
  }

  private fun setDamagesRequest(
    id: Long,
    damages: DamagesRequest?,
  ): ResultActions {
    val body = objectMapper.writeValueAsString(damages)
    return mockMvc
      .perform(
        MockMvcRequestBuilders.put("/reported-adjudications/$id/damages/edit")
          .header("Content-Type", "application/json")
          .content(body),
      )
  }

  companion object {
    private val DAMAGES_REQUEST = DamagesRequest(listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
  }
}
