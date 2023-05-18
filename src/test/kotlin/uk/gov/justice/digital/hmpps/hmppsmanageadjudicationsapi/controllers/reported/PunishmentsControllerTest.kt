package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService
import java.time.LocalDate

@WebMvcTest(
  PunishmentsController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
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
        .andExpect(MockMvcResultMatchers.status().isOk)

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

  @Nested
  inner class GetSuspendedPunishments {
    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.getSuspendedPunishments(
          any(),
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

      verify(punishmentsService).getSuspendedPunishments("AE1234")
    }

    private fun getSuspendedPunishmentsRequest(): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.get("/reported-adjudications/punishments/AE1234/suspended")
            .header("Content-Type", "application/json"),
        )
    }
  }

  @Nested
  inner class CreatePunishmentComment {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.createPunishmentComment(
          ArgumentMatchers.anyLong(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      createPunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      createPunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      createPunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to create punishment comment`() {
      createPunishmentCommentRequest(1, PUNISHMENT_COMMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isCreated)

      verify(punishmentsService).createPunishmentComment(
        adjudicationNumber = 1,
        PUNISHMENT_COMMENT_REQUEST,
      )
    }

    private fun createPunishmentCommentRequest(
      adjudicationNumber: Long,
      punishmentCommentRequest: PunishmentCommentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(punishmentCommentRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.post("/reported-adjudications/$adjudicationNumber/punishments/comment")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  @Nested
  inner class UpdatePunishmentComment {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentsService.updatePunishmentComment(
          ArgumentMatchers.anyLong(),
          any(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns status 404 if EntityNotFoundException is thrown`() {
      doThrow(EntityNotFoundException("")).`when`(punishmentsService)
        .updatePunishmentComment(any(), any())

      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns status Forbidden if ForbiddenException is thrown`() {
      doThrow(ForbiddenException("")).`when`(punishmentsService)
        .updatePunishmentComment(any(), any())

      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to update punishment comment`() {
      updatePunishmentCommentRequest(1, PUNISHMENT_COMMENT_REQUEST)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentsService).updatePunishmentComment(
        adjudicationNumber = 1,
        PUNISHMENT_COMMENT_REQUEST,
      )
    }

    private fun updatePunishmentCommentRequest(
      adjudicationNumber: Long,
      punishmentCommentRequest: PunishmentCommentRequest,
    ): ResultActions {
      val body = objectMapper.writeValueAsString(punishmentCommentRequest)
      return mockMvc
        .perform(
          MockMvcRequestBuilders.put("/reported-adjudications/$adjudicationNumber/punishments/comment")
            .header("Content-Type", "application/json")
            .content(body),
        )
    }
  }

  companion object {
    val PUNISHMENT_REQUEST = PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 10)
    val PUNISHMENT_COMMENT_REQUEST = PunishmentCommentRequest(comment = "some text")
    val SUSPENDED_PUNISHMENTS_DTO = listOf(
      SuspendedPunishmentDto(
        reportNumber = 1,
        punishment =
        PunishmentDto(
          type = PunishmentType.REMOVAL_WING,
          schedule = PunishmentScheduleDto(
            days = 10,
            suspendedUntil = LocalDate.now(),
          ),
        ),
      ),
    )
  }
}
