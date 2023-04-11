package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService

@WebMvcTest(value = [PunishmentsController::class])
class PunishmentsControllerTest : TestControllerBase() {

  @MockBean
  lateinit var punishmentsService: PunishmentsService

  @Nested
  inner class CreatePunishments {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.create(
          ArgumentMatchers.anyLong(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createPunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create a set of punishments`() {
      createPunishmentsRequest(1, PUNISHMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(punishmentsService).create(
        adjudicationNumber = 1,
        listOf(PunishmentRequest(type = PUNISHMENT_REQUEST.type, days = PUNISHMENT_REQUEST.days)),
      )
    }

    private fun createPunishmentsRequest(
      id: Long,
      punishmentRequest: PunishmentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        PunishmentsRequest(
          punishments = listOf(punishmentRequest),
        ),
      )
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$id/punishments")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class UpdatePunishments {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.update(
          ArgumentMatchers.anyLong(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      updatePunishmentsRequest(
        1,
        PUNISHMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to update a set of punishments`() {
      updatePunishmentsRequest(1, PUNISHMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(punishmentsService).update(
        adjudicationNumber = 1,
        listOf(PunishmentRequest(type = PUNISHMENT_REQUEST.type, days = PUNISHMENT_REQUEST.days)),
      )
    }

    private fun updatePunishmentsRequest(
      id: Long,
      punishmentRequest: PunishmentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(
        PunishmentsRequest(
          punishments = listOf(punishmentRequest),
        ),
      )
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$id/punishments")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    val PUNISHMENT_REQUEST = PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 10)
  }
}
