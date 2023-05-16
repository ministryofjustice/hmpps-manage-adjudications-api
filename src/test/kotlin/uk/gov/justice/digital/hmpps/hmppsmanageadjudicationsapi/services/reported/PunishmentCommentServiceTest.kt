package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import java.time.LocalDateTime

class PunishmentCommentServiceTest : ReportedAdjudicationTestBase() {

  private val prisonApiGateway: PrisonApiGateway = mock()

  private val punishmentCommentService = PunishmentCommentService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
    prisonApiGateway,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    assertThatThrownBy {
      punishmentCommentService.create(
        adjudicationNumber = 1,
        PunishmentCommentRequest(id = 2, comment = ""),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentCommentService.update(
        adjudicationNumber = 1,
        PunishmentCommentRequest(id = 2, comment = ""),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentCommentService.delete(
        adjudicationNumber = 1,
        punishmentCommentId = 2,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreatePunishmentComment {

    @Test
    fun `punishments not found`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
      }

      whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentCommentService.create(
          adjudicationNumber = 1L,
          punishmentComment = PunishmentCommentRequest(id = 2, comment = ""),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishments not found for adjudication number 1")
    }
  }

  @Test
  fun `punishment comment created`() {
    val reportedAdjudication = entityBuilder.reportedAdjudication(reportNumber = 1).also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.addPunishment(Punishment(type = PunishmentType.CONFINEMENT, schedule = mutableListOf(PunishmentSchedule(days = 0))))
    }

    whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
    whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)

    val result = punishmentCommentService.create(
      adjudicationNumber = 1,
      punishmentComment = PunishmentCommentRequest(id = 2, comment = "some text"),
    )

    assertThat(result.punishmentComments[0].comment).isEqualTo("some text")
  }
}
