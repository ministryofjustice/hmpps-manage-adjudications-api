package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.MigrateService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrateFixtures

@WebMvcTest(
  MigrateController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class MigrateControllerTest : TestControllerBase() {

  @MockBean
  lateinit var migrateService: MigrateService

  @Nested
  inner class Accept {

    private val migrateFixtures = MigrateFixtures()

    @Test
    fun `responds with a unauthorised status code`() {
      createMigrationRequest(
        migrateFixtures.ADULT_SINGLE_OFFENCE,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a 201`() {
      createMigrationRequest(
        migrateFixtures.ADULT_SINGLE_OFFENCE,
      ).andExpect(MockMvcResultMatchers.status().isCreated)
    }

    private fun createMigrationRequest(
      adjudication: AdjudicationMigrateDto,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(adjudication)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/migrate")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class Reset {

    @Test
    fun `responds with a unauthorised status code`() {
      createResetRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a 200`() {
      createResetRequest().andExpect(MockMvcResultMatchers.status().isOk)
      verify(migrateService, atLeastOnce()).reset()
    }

    private fun createResetRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/migrate/reset")
            .header("Content-Type", "application/json"),
        )
    }
  }
}
