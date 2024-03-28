package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException

@Transactional
@Service
class PunishmentCommentService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  authenticationFacade,
) {

  fun createPunishmentComment(
    chargeNumber: String,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)

    reportedAdjudication.punishmentComments.add(
      PunishmentComment(
        comment = punishmentComment.comment,
        reasonForChange = punishmentComment.reasonForChange,
      ),
    )

    return saveToDto(reportedAdjudication)
  }

  fun updatePunishmentComment(
    chargeNumber: String,
    punishmentComment: PunishmentCommentRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val punishmentCommentToUpdate = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentComment.id!!)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    punishmentCommentToUpdate.comment = punishmentComment.comment

    return saveToDto(reportedAdjudication)
  }

  fun deletePunishmentComment(
    chargeNumber: String,
    punishmentCommentId: Long,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    val punishmentComment = reportedAdjudication.punishmentComments.getPunishmentComment(punishmentCommentId)
      .also {
        it.createdByUserId?.validatePunishmentCommentAction(authenticationFacade.currentUsername!!)
      }

    reportedAdjudication.punishmentComments.remove(punishmentComment)

    return saveToDto(reportedAdjudication)
  }

  companion object {
    fun List<PunishmentComment>.getPunishmentComment(id: Long): PunishmentComment =
      this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Punishment comment id $id is not found")

    fun String.validatePunishmentCommentAction(username: String) {
      if (this != username) {
        throw ForbiddenException("Only $this can carry out action on punishment comment. attempt by $username")
      }
    }
  }
}
