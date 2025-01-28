package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportService
import java.time.LocalDate

@WebMvcTest(
  PunishmentsReportController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class PunishmentsReportControllerTest : TestControllerBase() {
  @MockitoBean
  lateinit var punishmentsReportsService: PunishmentsReportService

  @Nested
  inner class GetSuspendedPunishments {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsReportsService.getSuspendedPunishments(
          any(),
          anyOrNull(),
        ),
      ).thenReturn(SUSPENDED_PUNISHMENTS_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getSuspendedPunishmentsRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      getSuspendedPunishmentsRequest().andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      getSuspendedPunishmentsRequest().andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to get a set of suspended punishments`() {
      getSuspendedPunishmentsRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsReportsService).getSuspendedPunishments("AE1234", "12345")
    }

    private fun getSuspendedPunishmentsRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/punishments/AE1234/suspended/v2?chargeNumber=12345")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class GetReportsWithAdditionalDays {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsReportsService.getReportsWithAdditionalDays(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(ADDITIONAL_DAYS_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      getReportsWithAdditionalDaysRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      getReportsWithAdditionalDaysRequest().andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      getReportsWithAdditionalDaysRequest().andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to get reports with additional days`() {
      getReportsWithAdditionalDaysRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsReportsService).getReportsWithAdditionalDays("12345", "AE1234", PunishmentType.ADDITIONAL_DAYS)
    }

    private fun getReportsWithAdditionalDaysRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/punishments/AE1234/for-consecutive?type=ADDITIONAL_DAYS&chargeNumber=12345")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class GetActivePunishments {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsReportsService.getActivePunishments(
          ArgumentMatchers.anyLong(),
        ),
      ).thenReturn(
        listOf(
          ActivePunishmentDto(
            chargeNumber = "1234",
            punishmentType = PunishmentType.REMOVAL_WING,
          ),
        ),
      )
    }

    @Test
    fun `responds with a unauthorised status code`() {
      activePunishmentsRequest().andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
    fun `makes a call to get active punishments`() {
      activePunishmentsRequest()
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsReportsService).getActivePunishments(any())
    }

    private fun activePunishmentsRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/punishments/1234567/active")
            .header("Content-Type", "application/json"),
        )
    }
  }

  companion object {
    val SUSPENDED_PUNISHMENTS_DTO = listOf(
      SuspendedPunishmentDto(
        chargeNumber = "1",
        corrupted = false,
        punishment =
        PunishmentDto(
          type = PunishmentType.REMOVAL_WING,
          schedule = PunishmentScheduleDto(
            days = 10,
            suspendedUntil = LocalDate.now(),
            duration = 10,
            measurement = Measurement.DAYS,
          ),
        ),
      ),
    )
    val ADDITIONAL_DAYS_DTO = listOf(
      AdditionalDaysDto(
        chargeNumber = "1",
        chargeProvedDate = LocalDate.now(),
        punishment = PunishmentDto(
          type = PunishmentType.ADDITIONAL_DAYS,
          schedule = PunishmentScheduleDto(
            days = 10,
            duration = 10,
            measurement = Measurement.DAYS,
          ),
        ),
      ),
    )
  }
}
