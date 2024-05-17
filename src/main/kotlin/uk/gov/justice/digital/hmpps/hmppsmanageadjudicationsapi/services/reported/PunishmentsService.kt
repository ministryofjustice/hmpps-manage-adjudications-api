package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.RehabilitativeActivityRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.RehabilitativeActivity
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
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
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      if (it.getPunishments().isNotEmpty()) throw RuntimeException("This charge already has punishments - back key detected")
      it.validateCanAddPunishments()
    }

    punishments.validateCaution()
    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()

    punishments.filter { it.activatedFrom != null }.let {
      if (it.isNotEmpty()) {
        suspendedPunishmentEvents.addAll(
          activateSuspendedPunishments(
            reportedAdjudication = reportedAdjudication,
            toActivate = it,
          ),
        )
      }
    }

    punishments.filter { it.activatedFrom == null }.forEach {
      it.validateRequest(reportedAdjudication.getLatestHearing())
      reportedAdjudication.addPunishment(
        createNewPunishment(
          punishmentRequest = it,
          hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
        ),
      )
    }

    return saveToDto(reportedAdjudication).also {
      it.suspendedPunishmentEvents = suspendedPunishmentEvents
    }
  }

  fun update(
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }

    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()

    punishments.validateCaution()
    val idsToUpdate = punishments.filter { it.id != null }.map { it.id!! }
    suspendedPunishmentEvents.addAll(
      deactivateActivatedPunishments(
        chargeNumber = chargeNumber,
        idsToIgnore = idsToUpdate,
      ),
    )
    reportedAdjudication.deletePunishments(idsToIgnore = idsToUpdate)

    punishments.filter { it.activatedFrom != null }.let {
      if (it.isNotEmpty()) {
        suspendedPunishmentEvents.addAll(
          activateSuspendedPunishments(
            reportedAdjudication = reportedAdjudication,
            toActivate = it,
          ),
        )
      }
    }

    punishments.filter { it.activatedFrom == null }.forEach { punishmentRequest ->
      punishmentRequest.validateRequest(reportedAdjudication.getLatestHearing())

      if (punishmentRequest.id == null) {
        reportedAdjudication.addPunishment(
          createNewPunishment(
            punishmentRequest = punishmentRequest,
            hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
          ),
        )
      } else {
        val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
        when (punishmentToAmend.type) {
          punishmentRequest.type -> {
            punishmentRequest.suspendedUntil?.let {
              punishmentRequest.type.consecutiveReportValidation(chargeNumber)
            }
            updatePunishment(punishmentToAmend, punishmentRequest)
          }
          // punishment type has changed, so needs to be removed and recreated
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
    }

    return saveToDto(
      reportedAdjudication.also {
        if (it.status == ReportedAdjudicationStatus.INVALID_SUSPENDED) it.calculateStatus()
      },
    ).also {
      it.suspendedPunishmentEvents = suspendedPunishmentEvents
    }
  }

  fun updateRehabilitativeActivity(chargeNumber: String, id: Long, rehabilitativeActivityRequest: RehabilitativeActivityRequest): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber)
    reportedAdjudication.getPunishmentForRehabilitativeActivity(id = id)
      .rehabilitativeActivities.first { it.id == id }.let {
        it.details = rehabilitativeActivityRequest.details
        it.endDate = rehabilitativeActivityRequest.endDate
        it.monitor = rehabilitativeActivityRequest.monitor
        it.totalSessions = rehabilitativeActivityRequest.totalSessions
      }

    return saveToDto(reportedAdjudication)
  }

  private fun activateSuspendedPunishments(
    reportedAdjudication: ReportedAdjudication,
    toActivate: List<PunishmentRequest>,
  ): Set<SuspendedPunishmentEvent> {
    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()
    val reportsActivatedFrom = findByChargeNumberIn(toActivate.map { it.activatedFrom!! }.distinct())

    toActivate.forEach { punishment ->
      punishment.id ?: throw ValidationException("Suspended punishment activation missing punishment id to activate")
      punishment.validateRequest(reportedAdjudication.getLatestHearing())
      val reportToUpdate = reportsActivatedFrom.firstOrNull { it.chargeNumber == punishment.activatedFrom!! } ?: throw EntityNotFoundException("activated from charge ${punishment.activatedFrom} not found")

      reportToUpdate.getPunishments().getSuspendedPunishmentToActivate(id = punishment.id)?.let {
        it.suspendedUntil = null
        it.activatedByChargeNumber = reportedAdjudication.chargeNumber
        it.schedule.add(
          PunishmentSchedule(
            duration = it.schedule.latestSchedule().duration,
            startDate = punishment.startDate,
            endDate = punishment.endDate,
            measurement = punishment.type.measurement,
          ),
        )
        suspendedPunishmentEvents.add(
          SuspendedPunishmentEvent(
            agencyId = reportToUpdate.originatingAgencyId,
            chargeNumber = reportToUpdate.chargeNumber,
            status = reportToUpdate.status,
          ),
        )
      }
    }

    return suspendedPunishmentEvents
  }

  private fun PunishmentType.consecutiveReportValidation(chargeNumber: String) {
    if (PunishmentType.additionalDays().contains(this)) {
      if (isLinkedToReport(chargeNumber, listOf(this))) {
        throw ValidationException("Unable to modify: $this is linked to another report")
      }
    }
  }

  private fun ReportedAdjudication.deletePunishments(idsToIgnore: List<Long>) {
    val punishmentsToRemove = this.getPunishments().filter { idsToIgnore.none { id -> id == it.id } }
    punishmentsToRemove.forEach { punishment ->
      punishment.type.consecutiveReportValidation(this.chargeNumber).let {
        punishment.deleted = true
      }
    }
  }

  private fun createNewPunishment(punishmentRequest: PunishmentRequest, hearingDate: LocalDate? = null): Punishment =
    Punishment(
      type = punishmentRequest.type,
      privilegeType = punishmentRequest.privilegeType,
      otherPrivilege = punishmentRequest.otherPrivilege,
      stoppagePercentage = punishmentRequest.stoppagePercentage,
      suspendedUntil = punishmentRequest.suspendedUntil,
      consecutiveToChargeNumber = punishmentRequest.consecutiveChargeNumber,
      amount = punishmentRequest.damagesOwedAmount,
      paybackNotes = punishmentRequest.paybackNotes,
      rehabilitativeActivities = punishmentRequest.rehabilitativeActivities.map {
        RehabilitativeActivity(
          details = it.details,
          monitor = it.monitor,
          endDate = it.endDate,
          totalSessions = it.totalSessions,
        )
      }.toMutableList(),
      schedule = when (punishmentRequest.type) {
        PunishmentType.CAUTION -> mutableListOf(
          PunishmentSchedule(
            duration = 0,
            measurement = Measurement.DAYS,
          ),
        )
        PunishmentType.DAMAGES_OWED -> mutableListOf(
          PunishmentSchedule(
            duration = 0,
            startDate = hearingDate,
            measurement = Measurement.DAYS,
          ),
        )
        PunishmentType.PAYBACK -> mutableListOf(
          PunishmentSchedule(
            duration = punishmentRequest.duration,
            measurement = punishmentRequest.type.measurement,
            startDate = hearingDate,
            endDate = punishmentRequest.endDate,
          ),
        )
        else -> mutableListOf(
          PunishmentSchedule(
            duration = punishmentRequest.duration!!,
            startDate = punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
            measurement = punishmentRequest.type.measurement,
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
      it.consecutiveToChargeNumber = punishmentRequest.consecutiveChargeNumber
      it.amount = punishmentRequest.damagesOwedAmount
      it.paybackNotes = punishmentRequest.paybackNotes
      it.rehabilitativeActivities = punishmentRequest.rehabilitativeActivities.map { ra ->
        RehabilitativeActivity(
          details = ra.details,
          monitor = ra.monitor,
          endDate = ra.endDate,
          totalSessions = ra.totalSessions,
        )
      }.toMutableList()
      val latestSchedule = it.schedule.latestSchedule()
      if (latestSchedule.hasScheduleBeenUpdated(punishmentRequest) && !PunishmentType.damagesAndCaution().contains(it.type)) {
        it.schedule.add(
          PunishmentSchedule(
            duration = punishmentRequest.duration,
            startDate = if (it.type == PunishmentType.PAYBACK) latestSchedule.startDate else punishmentRequest.startDate,
            endDate = punishmentRequest.endDate,
            suspendedUntil = punishmentRequest.suspendedUntil,
            measurement = punishmentRequest.type.measurement,
          ),
        )
      }
    }

  private fun PunishmentRequest.validateRequest(latestHearing: Hearing?) {
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
      PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS, PunishmentType.DAMAGES_OWED, PunishmentType.CAUTION, PunishmentType.PAYBACK -> {}
      else -> {
        this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
        this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
        this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
      }
    }
    if (this.rehabilitativeActivities.isNotEmpty()) {
      if (OicHearingType.govTypes().none { it == latestHearing?.oicHearingType }) {
        throw ValidationException("only GOV can award rehabilitative activities")
      }
      if (!this.type.rehabilitativeActivitiesAllowed) throw ValidationException("punishment type does not support rehabilitative activities")
      this.suspendedUntil ?: throw ValidationException("only suspended punishments can have rehabilitative activities")
    }
  }

  companion object {

    fun List<PunishmentSchedule>.latestSchedule() = this.maxBy { it.createDateTime!! }

    fun List<Punishment>.getSuspendedPunishmentToActivate(id: Long): Punishment? {
      val punishmentToActivate = this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")
      // we do no activate punishments if already activated.  Can be presented again via batch update
      return if (punishmentToActivate.activatedByChargeNumber == null) punishmentToActivate else null
    }

    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean =
      this.duration != punishmentRequest.duration || this.endDate != punishmentRequest.endDate || this.startDate != punishmentRequest.startDate ||
        this.suspendedUntil != punishmentRequest.suspendedUntil

    private fun List<Punishment>.getPunishment(id: Long): Punishment? =
      this.firstOrNull { it.id == id }
    fun List<Punishment>.getPunishmentToAmend(id: Long): Punishment =
      this.getPunishment(id) ?: throw EntityNotFoundException("Punishment $id is not associated with ReportedAdjudication")

    fun ReportedAdjudication.validateCanAddPunishments() {
      if (!listOf(ReportedAdjudicationStatus.CHARGE_PROVED, ReportedAdjudicationStatus.INVALID_SUSPENDED, ReportedAdjudicationStatus.INVALID_SUSPENDED).contains(this.status)) {
        throw ValidationException("status is not CHARGE_PROVED")
      }
    }

    fun List<PunishmentRequest>.validateCaution() {
      if (this.any { it.type == PunishmentType.CAUTION }) {
        if (!this.all { PunishmentType.damagesAndCaution().contains(it.type) }) {
          throw ValidationException("CAUTION can only include DAMAGES_OWED")
        }
      }
    }
  }
}
