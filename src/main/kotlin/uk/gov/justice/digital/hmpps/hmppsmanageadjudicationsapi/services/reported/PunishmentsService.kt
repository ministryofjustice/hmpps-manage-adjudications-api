package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ActivePunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
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
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }

    punishments.validateCaution()

    punishments.forEach {
      it.validateRequest(reportedAdjudication.getLatestHearing())
      if (it.activatedFrom != null) {
        reportedAdjudication.addPunishment(
          activateSuspendedPunishment(chargeNumber = chargeNumber, punishmentRequest = it),
        )
      } else {
        reportedAdjudication.addPunishment(
          createNewPunishment(
            punishmentRequest = it,
            hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
          ),
        )
      }
    }

    return saveToDto(reportedAdjudication)
  }

  fun update(
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }

    punishments.validateCaution()

    val idsToUpdate = punishments.filter { it.id != null }.map { it.id }
    reportedAdjudication.getPunishments().filter { !idsToUpdate.contains(it.id) }.forEach { punishment ->
      punishment.type.consecutiveReportValidation(chargeNumber).let {
        punishment.deleted = true
      }
    }

    punishments.forEach { punishmentRequest ->
      punishmentRequest.validateRequest(reportedAdjudication.getLatestHearing())

      when (punishmentRequest.id) {
        null -> when (punishmentRequest.activatedFrom) {
          null -> reportedAdjudication.addPunishment(
            createNewPunishment(
              punishmentRequest = punishmentRequest,
              hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
            ),
          )
          // this is related to manual activation (to be removed post migration) where there is no id to clone
          else -> reportedAdjudication.addPunishment(activateSuspendedPunishment(chargeNumber = chargeNumber, punishmentRequest = punishmentRequest))
        }
        else -> {
          when (punishmentRequest.activatedFrom) {
            null -> {
              val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
              when (punishmentToAmend.type) {
                punishmentRequest.type -> {
                  punishmentRequest.suspendedUntil?.let {
                    punishmentRequest.type.consecutiveReportValidation(chargeNumber)
                  }
                  updatePunishment(punishmentToAmend, punishmentRequest)
                }
                else -> {
                  punishmentToAmend.type.consecutiveReportValidation(chargeNumber).let {
                    punishmentToAmend.deleted = true
                    reportedAdjudication.addPunishment(
                      createNewPunishment(
                        punishmentRequest = punishmentRequest,
                        hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
                      ),
                    )
                  }
                }
              }
            }
            else ->
              // this check is stop activating it again if the id exists.  otherwise the id is reference to the item it will clone
              if (reportedAdjudication.getPunishments().getPunishment(punishmentRequest.id) == null) {
                reportedAdjudication.addPunishment(activateSuspendedPunishment(chargeNumber = chargeNumber, punishmentRequest = punishmentRequest))
              }
          }
        }
      }
    }

    return saveToDto(
      reportedAdjudication.also {
        if (it.status == ReportedAdjudicationStatus.INVALID_SUSPENDED) it.calculateStatus()
      },
    )
  }

  fun getSuspendedPunishments(prisonerNumber: String, chargeNumber: String): List<SuspendedPunishmentDto> {
    val reportsWithSuspendedPunishments = getReportsWithSuspendedPunishments(prisonerNumber = prisonerNumber).toMutableList()
      .union(getCorruptedReportsWithSuspendedPunishmentsInLast6Months(prisonerNumber = prisonerNumber))
      .filter { it.chargeNumber != chargeNumber }

    val includeAdditionalDays = includeAdditionalDays(chargeNumber)

    return reportsWithSuspendedPunishments.map {
      it.getPunishments().suspendedPunishmentsToActivate()
        .filter { punishment -> punishment.type.includeInSuspendedPunishments(includeAdditionalDays) }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          SuspendedPunishmentDto(
            chargeNumber = it.chargeNumber,
            corrupted = ReportedAdjudicationStatus.corruptedStatuses().contains(it.status),
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              privilegeType = punishment.privilegeType,
              otherPrivilege = punishment.otherPrivilege,
              stoppagePercentage = punishment.stoppagePercentage,
              schedule = PunishmentScheduleDto(days = schedule.days, suspendedUntil = schedule.suspendedUntil),
            ),
          )
        }
    }.flatten()
  }

  fun getReportsWithAdditionalDays(chargeNumber: String, prisonerNumber: String, punishmentType: PunishmentType): List<AdditionalDaysDto> {
    if (!PunishmentType.additionalDays().contains(punishmentType)) throw ValidationException("Punishment type must be ADDITIONAL_DAYS or PROSPECTIVE_DAYS")

    val reportedAdjudication = findByChargeNumber(chargeNumber)

    return getReportsWithActiveAdditionalDays(
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    ).filter { it.includeAdaWithSameHearingDateAndSeparateCharge(reportedAdjudication) }
      .map {
        it.getPunishments().filter { punishment -> punishment.type == punishmentType }.map { punishment ->
          val schedule = punishment.schedule.latestSchedule()

          AdditionalDaysDto(
            chargeNumber = it.chargeNumber,
            chargeProvedDate = it.getLatestHearing()?.dateTimeOfHearing?.toLocalDate()!!,
            punishment = PunishmentDto(
              id = punishment.id,
              type = punishment.type,
              consecutiveChargeNumber = punishment.consecutiveChargeNumber,
              schedule = PunishmentScheduleDto(days = schedule.days),
            ),
          )
        }
      }.flatten()
  }

  fun getActivePunishments(offenderBookingId: Long): List<ActivePunishmentDto> =
    getReportsWithActivePunishments(offenderBookingId = offenderBookingId)
      .map { chargeAndPunishments ->
        chargeAndPunishments.second.map {
          val latestSchedule = it.schedule.latestSchedule()
          ActivePunishmentDto(
            punishmentType = it.type,
            privilegeType = it.privilegeType,
            otherPrivilege = it.otherPrivilege,
            chargeNumber = chargeAndPunishments.first,
            startDate = latestSchedule.startDate,
            lastDay = latestSchedule.endDate,
            days = if (latestSchedule.days == 0) null else latestSchedule.days,
            amount = it.amount,
            stoppagePercentage = it.stoppagePercentage,
            activatedFrom = it.activatedFromChargeNumber,
          )
        }
      }.flatten().sortedByDescending { it.startDate }

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

  private fun PunishmentType.consecutiveReportValidation(chargeNumber: String) {
    if (PunishmentType.additionalDays().contains(this)) {
      if (isLinkedToReport(chargeNumber, listOf(this))) {
        throw ValidationException("Unable to modify: $this is linked to another report")
      }
    }
  }

  private fun includeAdditionalDays(chargeNumber: String): Boolean {
    val reportedAdjudication = findByChargeNumber(chargeNumber)
    return OicHearingType.inadTypes().contains(reportedAdjudication.getLatestHearing()?.oicHearingType)
  }

  private fun createNewPunishment(punishmentRequest: PunishmentRequest, hearingDate: LocalDate? = null): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      consecutiveChargeNumber = punishmentRequest.consecutiveChargeNumber,
      amount = punishmentRequest.damagesOwedAmount,
      schedule = when (punishmentRequest.type) {
        PunishmentType.CAUTION -> mutableListOf(PunishmentSchedule(days = 0))
        PunishmentType.DAMAGES_OWED -> mutableListOf(PunishmentSchedule(days = 0, startDate = hearingDate))
        else -> mutableListOf(
          PunishmentSchedule(
            days = punishmentRequest.days!!,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      },
    )

  private fun updatePunishment(punishment: Punishment, punishmentRequest: PunishmentRequest) =
    punishment.let {
      it.privilegeType = punishmentRequest.privilegeType
      it.otherPrivilege = punishmentRequest.otherPrivilege
      it.stoppagePercentage = punishmentRequest.stoppagePercentage
      it.suspendedUntil = punishmentRequest.suspendedUntil
      it.consecutiveChargeNumber = punishmentRequest.consecutiveChargeNumber
      it.amount = punishmentRequest.damagesOwedAmount
      if (it.schedule.latestSchedule().hasScheduleBeenUpdated(punishmentRequest) && !PunishmentType.damagesAndCaution().contains(it.type)) {
        it.schedule.add(
          PunishmentSchedule(
            days = punishmentRequest.days!!,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
          ),
        )
      }
    }

  private fun activateSuspendedPunishment(chargeNumber: String, punishmentRequest: PunishmentRequest): Punishment {
    var suspendedPunishment: Punishment? = null
    punishmentRequest.id?.run {
      val activatedFromReport = findByChargeNumber(punishmentRequest.activatedFrom!!)
      suspendedPunishment = activatedFromReport.getPunishments().getSuspendedPunishment(punishmentRequest.id).also {
        it.activatedByChargeNumber = chargeNumber
      }
    }

    return cloneSuspendedPunishment(
      punishment = suspendedPunishment ?: createNewPunishment(punishmentRequest = punishmentRequest),
      days = punishmentRequest.days!!,
      startDate = punishmentRequest.startDate,
      endDate = punishmentRequest.endDate,
    ).also {
      it.activatedFromChargeNumber = punishmentRequest.activatedFrom
      it.consecutiveChargeNumber = punishmentRequest.consecutiveChargeNumber
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
      this.filter { p -> p.activatedFromChargeNumber == null && p.activatedByChargeNumber == null && p.schedule.latestSchedule().suspendedUntil != null }

    fun List<Punishment>.getSuspendedPunishment(id: Long): Punishment = this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")

    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean =
      this.days != punishmentRequest.days || this.endDate != punishmentRequest.endDate || this.startDate != punishmentRequest.startDate ||
        this.suspendedUntil != punishmentRequest.suspendedUntil

    private fun List<Punishment>.getPunishment(id: Long): Punishment? =
      this.firstOrNull { it.id == id }
    fun List<Punishment>.getPunishmentToAmend(id: Long): Punishment =
      this.getPunishment(id) ?: throw EntityNotFoundException("Punishment $id is not associated with ReportedAdjudication")

    fun List<PunishmentComment>.getPunishmentComment(id: Long): PunishmentComment =
      this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("Punishment comment id $id is not found")

    fun PunishmentRequest.validateRequest(latestHearing: Hearing?) {
      when (this.type) {
        PunishmentType.DAMAGES_OWED -> this.damagesOwedAmount ?: throw ValidationException("amount missing for type DAMAGES_OWED")
        PunishmentType.PRIVILEGE -> {
          this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
          if (this.privilegeType == PrivilegeType.OTHER) {
            this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
          }
        }
        PunishmentType.EARNINGS -> this.stoppagePercentage ?: throw ValidationException("stoppage percentage missing for type EARNINGS")
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> if (!OicHearingType.inadTypes().contains(latestHearing?.oicHearingType)) throw ValidationException("Punishment ${this.type} is invalid as the punishment decision was not awarded by an independent adjudicator")
        else -> {}
      }
      when (this.type) {
        PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS, PunishmentType.DAMAGES_OWED, PunishmentType.CAUTION -> {}
        else -> {
          this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
          this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
          this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }
    }

    fun ReportedAdjudication.validateCanAddPunishments() {
      if (!listOf(ReportedAdjudicationStatus.CHARGE_PROVED, ReportedAdjudicationStatus.INVALID_SUSPENDED, ReportedAdjudicationStatus.INVALID_SUSPENDED).contains(this.status)) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
    }

    fun String.validatePunishmentCommentAction(username: String) {
      if (this != username) {
        throw ForbiddenException("Only $this can carry out action on punishment comment. attempt by $username")
      }
    }

    fun PunishmentType.includeInSuspendedPunishments(includeAdditionalDays: Boolean): Boolean {
      return if (!PunishmentType.additionalDays().contains(this)) {
        true
      } else {
        includeAdditionalDays
      }
    }

    fun List<PunishmentRequest>.validateCaution() {
      if (this.any { it.type == PunishmentType.CAUTION }) {
        if (!this.all { PunishmentType.damagesAndCaution().contains(it.type) }) {
          throw ValidationException("CAUTION can only include DAMAGES_OWED")
        }
      }
    }

    fun ReportedAdjudication.includeAdaWithSameHearingDateAndSeparateCharge(currentAdjudication: ReportedAdjudication): Boolean =
      this.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() == currentAdjudication.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() &&
        this.chargeNumber != currentAdjudication.chargeNumber
  }
}
