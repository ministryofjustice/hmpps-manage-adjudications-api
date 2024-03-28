package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
import java.time.LocalDateTime

class PunishmentCommentServiceTest : ReportedAdjudicationTestBase() {

  private val punishmentCommentService = PunishmentCommentService(
    reportedAdjudicationRepository,
    authenticationFacade,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      punishmentCommentService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = ""),

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      punishmentCommentService.updatePunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = ""),

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      punishmentCommentService.deletePunishmentComment(
        chargeNumber = "1",
        punishmentCommentId = 1,

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreatePunishmentComment {

    @Test
    fun `Punishment comment created`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication()

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentCommentService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = "some text"),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      Assertions.assertThat(argumentCaptor.value.punishmentComments[0].comment).isEqualTo("some text")
    }

    @Test
    fun `punishment comment with reason for change`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentCommentService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = "some text", reasonForChange = ReasonForChange.APPEAL),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      Assertions.assertThat(argumentCaptor.value.punishmentComments[0].reasonForChange).isEqualTo(ReasonForChange.APPEAL)
    }
  }

  @Nested
  inner class UpdatePunishmentComment {

    @Test
    fun `Punishment comment not found`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        punishmentCommentService.updatePunishmentComment(
          chargeNumber = "1",
          punishmentComment = PunishmentCommentRequest(id = -1, comment = "new text"),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment comment id -1 is not found")
    }

    @Test
    fun `Only author can update comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        punishmentCommentService.updatePunishmentComment(
          chargeNumber = "1",
          punishmentComment = PunishmentCommentRequest(id = 2, comment = "new text"),
        )
      }.isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("Only author can carry out action on punishment comment. attempt by ITAG_USER")
    }

    @Test
    fun `Update punishment comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("author")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentCommentService.updatePunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(id = 2, comment = "new text"),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      Assertions.assertThat(argumentCaptor.value.punishmentComments[0].comment).isEqualTo("new text")
    }
  }

  @Nested
  inner class DeletePunishmentComment {

    @Test
    fun `Punishment comment not found`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        punishmentCommentService.deletePunishmentComment(
          chargeNumber = "1",
          punishmentCommentId = -1,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment comment id -1 is not found")
    }

    @Test
    fun `Only author can delete comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        punishmentCommentService.deletePunishmentComment(
          chargeNumber = "1",
          punishmentCommentId = 2,
        )
      }.isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("Only author can carry out action on punishment comment. attempt by ITAG_USER")
    }

    @Test
    fun `Delete punishment comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "some text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("author")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentCommentService.deletePunishmentComment(
        chargeNumber = "1",
        punishmentCommentId = 2,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      Assertions.assertThat(argumentCaptor.value.punishmentComments.size).isEqualTo(0)
    }
  }
}
