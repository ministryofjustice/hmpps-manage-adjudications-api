package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Transactional
@Service
class PunishmentsService(
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  fun create(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateCanAddPunishments()
    }

    punishments.forEach {
      it.validateRequest()
      reportedAdjudication.punishments.add(createNewPunishment(it))
    }

    return saveToDto(reportedAdjudication)
  }

  fun update(
    adjudicationNumber: Long,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByAdjudicationNumber(adjudicationNumber).also {
      it.status.validateCanAddPunishments()
    }

    val ids = punishments.filter { it.id != null }.map { it.id }
    val toRemove = reportedAdjudication.punishments.filter { !ids.contains(it.id) }
    reportedAdjudication.punishments.removeAll(toRemove)

    punishments.forEach {
      it.validateRequest()

      when (it.id) {
        null -> reportedAdjudication.punishments.add(createNewPunishment(it))
        else -> {
          val punishmentToAmend = reportedAdjudication.punishments.getPunishmentToAmend(it.id)
          when (punishmentToAmend.type) {
            it.type -> updatePunishment(punishmentToAmend, it)
            else -> {
              reportedAdjudication.punishments.remove(punishmentToAmend)
              reportedAdjudication.punishments.add(createNewPunishment(it))
            }
          }
        }
      }
    }

    return saveToDto(reportedAdjudication)
  }
  private fun createNewPunishment(punishmentRequest: PunishmentRequest): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      schedule = mutableListOf(
        PunishmentSchedule(days = punishmentRequest.days, startDate = punishmentRequest.startDate, endDate = punishmentRequest.endDate, suspendedUntil = punishmentRequest.suspendedUntil),
      ),
    )

  private fun updatePunishment(punishment: Punishment, punishmentRequest: PunishmentRequest) =
    punishment.let {
      it.privilegeType = punishmentRequest.privilegeType
      it.otherPrivilege = punishmentRequest.otherPrivilege
      it.stoppagePercentage = punishmentRequest.stoppagePercentage
      it.schedule.add(
        PunishmentSchedule(days = punishmentRequest.days, startDate = punishmentRequest.startDate, endDate = punishmentRequest.endDate, suspendedUntil = punishmentRequest.suspendedUntil),
      )
    }

  companion object {

    fun List<Punishment>.getPunishmentToAmend(id: Long) =
      this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Punishment $id is not associated with ReportedAdjudication")
    fun PunishmentRequest.validateRequest() {
      when (this.type) {
        PunishmentType.PRIVILEGE -> {
          this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (this.privilegeType == PrivilegeType.OTHER) {
            this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> this.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
        else -> {}
      }
      when (this.type) {
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> {}
        else -> {
          this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
          this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
          this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }
    }

    fun ReportedAdjudicationStatus.validateCanAddPunishments() {
      if (this != ReportedAdjudicationStatus.CHARGE_PROVED) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
    }
  }
}
