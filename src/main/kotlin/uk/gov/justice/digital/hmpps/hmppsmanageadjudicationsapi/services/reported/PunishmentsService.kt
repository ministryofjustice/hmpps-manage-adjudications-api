package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate

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
      if (it.activatedFrom != null) {
        reportedAdjudication.punishments.add(
          activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.punishments.add(createNewPunishment(punishmentRequest = it))
      }
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
        null -> reportedAdjudication.punishments.add(createNewPunishment(punishmentRequest = it))
        else -> {
          when (it.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.punishments.getPunishmentToAmend(it.id)
              when (punishmentToAmend.type) {
                it.type -> updatePunishment(punishmentToAmend, it)
                else -> {
                  reportedAdjudication.punishments.remove(punishmentToAmend)
                  reportedAdjudication.punishments.add(createNewPunishment(punishmentRequest = it))
                }
              }
            }
            else -> reportedAdjudication.punishments.add(
              activateSuspendedPunishment(adjudicationNumber = adjudicationNumber, punishmentRequest = it),
            )
          }
        }
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun getSuspendedPunishments(prisonerNumber: String): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber)

    return reportsWithSuspendedPunishments.map {
      it.punishments.suspendedPunishmentsToActivate().map { p ->
        val schedule = p.schedule.latestSchedule()

        SuspendedPunishmentDto(
          reportNumber = it.reportNumber,
          punishment = PunishmentDto(
            id = p.id,
            type = p.type,
            privilegeType = p.privilegeType,
            otherPrivilege = p.otherPrivilege,
            stoppagePercentage = p.stoppagePercentage,
            schedule = PunishmentScheduleDto(days = schedule.days, suspendedUntil = schedule.suspendedUntil),
          ),
        )
      }
    }.flatten()
  }

  private fun createNewPunishment(punishmentRequest: PunishmentRequest): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      schedule = mutableListOf(
        PunishmentSchedule(
          days = punishmentRequest.days,
          startDate = punishmentRequest.startDate,
          endDate = punishmentRequest.endDate,
          suspendedUntil = punishmentRequest.suspendedUntil,
        ),
      ),
    )

  private fun updatePunishment(punishment: Punishment, punishmentRequest: PunishmentRequest) =
    punishment.let {
      it.privilegeType = punishmentRequest.privilegeType
      it.otherPrivilege = punishmentRequest.otherPrivilege
      it.stoppagePercentage = punishmentRequest.stoppagePercentage
      it.suspendedUntil = punishmentRequest.suspendedUntil
      if (it.schedule.latestSchedule().hasScheduleBeenUpdated(punishmentRequest)) {
        it.schedule.add(
          PunishmentSchedule(
            days = punishmentRequest.days,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      }
    }

  private fun activateSuspendedPunishment(adjudicationNumber: Long, punishmentRequest: PunishmentRequest): Punishment {
    val activatedFromReport = findByAdjudicationNumber(punishmentRequest.activatedFrom!!)
    val suspendedPunishment = activatedFromReport.punishments.getSuspendedPunishment(punishmentRequest.id!!)

    suspendedPunishment.activatedBy = adjudicationNumber

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment,
      days = punishmentRequest.days,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFrom = punishmentRequest.activatedFrom
    }
  }
  private fun cloneSuspendedPunishment(punishment: Punishment, days: Int, startDate: LocalDate?, endDate: LocalDate?) = Punishment(
    type = punishment.type,
    privilegeType = punishment.privilegeType,
    otherPrivilege = punishment.otherPrivilege,
    stoppagePercentage = punishment.stoppagePercentage,
    schedule = mutableListOf(
      PunishmentSchedule(days = days, startDate = startDate, endDate = endDate),
    ),
  )

  companion object {

    fun List<PunishmentSchedule>.latestSchedule() = this.maxBy { it.createDateTime!! }
    fun List<Punishment>.suspendedPunishmentsToActivate() =
      this.filter { p -> p.activatedFrom == null && p.activatedBy == null && p.schedule.latestSchedule().suspendedUntil != null }
    fun List<Punishment>.getSuspendedPunishment(id: Long): Punishment = this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")

    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean =
      this.days != punishmentRequest.days || this.endDate != punishmentRequest.endDate || this.startDate != punishmentRequest.startDate ||
        this.suspendedUntil != punishmentRequest.suspendedUntil

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
