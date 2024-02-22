package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Dis5PrintSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PrintSupportService
import java.time.LocalDate

@WebMvcTest(
  PrintSupportController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class PrintSupportControllerTest : TestControllerBase() {

  @MockBean
  lateinit var printSupportService: PrintSupportService

  @Nested
  inner class Dis5 {
    @Test
    fun `responds with a unauthorised status code`() {
      dis5Request("12345").andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS", "SCOPE_write"])
    fun `gets dis5 print support successfully`() {
      whenever(printSupportService.getDis5Data("12345")).thenReturn(
        Dis5PrintSupportDto(
          chargeNumber = "12345",
          dateOfDiscovery = LocalDate.now(),
          dateOfIncident = LocalDate.now(),
          previousCount = 0,
          previousAtCurrentEstablishmentCount = 0,
          sameOffenceCount = 0,
          chargesWithSuspendedPunishments = emptyList(),
        ),
      )

      dis5Request("12345")
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(printSupportService, atLeastOnce()).getDis5Data("12345")
    }

    private fun dis5Request(
      chargeNumber: String,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/$chargeNumber/print-support/dis5")
            .header("Content-Type", "application/json"),
        )
    }
  }
}
