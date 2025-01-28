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
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentCommentService

@WebMvcTest(
  PunishmentCommentController::class,
  excludeAutoConfiguration = [SecurityAutoConfiguration::class, OAuth2ClientAutoConfiguration::class, OAuth2ResourceServerAutoConfiguration::class],
)
class PunishmentCommentControllerTest : TestControllerBase() {

  @MockitoBean
  lateinit var punishmentCommentService: PunishmentCommentService

  @Nested
  inner class CreatePunishmentComment {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentCommentService.createPunishmentComment(
          ArgumentMatchers.anyString(),
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

      verify(punishmentCommentService).createPunishmentComment(
        chargeNumber = "1",
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
        punishmentCommentService.updatePunishmentComment(
          ArgumentMatchers.anyString(),
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
      doThrow(EntityNotFoundException("")).`when`(punishmentCommentService)
        .updatePunishmentComment(any(), any())

      updatePunishmentCommentRequest(
        1,
        PUNISHMENT_COMMENT_REQUEST,
      ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns status Forbidden if ForbiddenException is thrown`() {
      doThrow(ForbiddenException("")).`when`(punishmentCommentService)
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

      verify(punishmentCommentService).updatePunishmentComment(
        chargeNumber = "1",
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

  @Nested
  inner class DeletePunishmentComment {

    @BeforeEach
    fun beforeEach() {
      whenever(
        punishmentCommentService.deletePunishmentComment(
          ArgumentMatchers.anyString(),
          ArgumentMatchers.anyLong(),
        ),
      ).thenReturn(REPORTED_ADJUDICATION_DTO)
    }

    @Test
    fun `responds with a unauthorised status code`() {
      deletePunishmentCommentRequest(
        1,
        2,
      ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["SCOPE_write"])
    fun `responds with a forbidden status code for non ALO`() {
      deletePunishmentCommentRequest(
        1,
        2,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER"])
    fun `responds with a forbidden status code for ALO without write scope`() {
      deletePunishmentCommentRequest(
        1,
        2,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns status 404 if EntityNotFoundException is thrown`() {
      doThrow(EntityNotFoundException("")).`when`(punishmentCommentService)
        .deletePunishmentComment(any(), any())

      deletePunishmentCommentRequest(
        1,
        -1,
      ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `returns status Forbidden if ForbiddenException is thrown`() {
      doThrow(ForbiddenException("")).`when`(punishmentCommentService)
        .deletePunishmentComment(any(), any())

      deletePunishmentCommentRequest(
        1,
        2,
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    @Test
    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_ADJUDICATIONS_REVIEWER", "SCOPE_write"])
    fun `makes a call to delete punishment comment`() {
      deletePunishmentCommentRequest(1, 2)
        .andExpect(MockMvcResultMatchers.status().isOk)

      verify(punishmentCommentService).deletePunishmentComment(
        chargeNumber = "1",
        punishmentCommentId = 2,
      )
    }

    private fun deletePunishmentCommentRequest(
      adjudicationNumber: Long,
      punishmentCommentId: Long,
    ): ResultActions {
      return mockMvc
        .perform(
          MockMvcRequestBuilders.delete("/reported-adjudications/$adjudicationNumber/punishments/comment/$punishmentCommentId")
            .header("Content-Type", "application/json"),
        )
    }
  }

  companion object {
    val PUNISHMENT_COMMENT_REQUEST = PunishmentCommentRequest(comment = "some text")
  }
}
