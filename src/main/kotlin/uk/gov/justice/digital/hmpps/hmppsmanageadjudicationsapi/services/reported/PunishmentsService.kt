package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.CompleteRehabilitativeActivityRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsChangeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.toLossOfVisitsEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotCompletedOutcome
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
import java.time.LocalDateTime

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
    validateNoConsecutiveLoop(chargeNumber, punishments)
    val lossOfVisitsAwarded = punishments.any { it.activatedFrom == null && it.type.isVisitsPunishment() }

    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      if (it.getPunishments()
          .isNotEmpty()
      ) {
        throw ValidationException("This charge already has punishments - back key detected")
      }
      it.validateCanAddPunishments()
    }

    punishments.validateCaution()
    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()
    val supplementalLossOfVisitsEvents = mutableListOf<LossOfVisitsEventDto>()

    punishments.filter { it.activatedFrom != null }.let {
      if (it.isNotEmpty()) {
        activateSuspendedPunishments(
          reportedAdjudication = reportedAdjudication,
          toActivate = it,
        ).also { updates ->
          suspendedPunishmentEvents.addAll(updates.events)
          supplementalLossOfVisitsEvents.addAll(updates.lossOfVisitsEvents)
        }
      }
    }

    punishments.filter { it.activatedFrom == null }.forEach {
      it.validateRequest(
        latestHearing = reportedAdjudication.getLatestHearing(),
        isYouthOffender = reportedAdjudication.isYouthOffender,
      )
      reportedAdjudication.addPunishment(
        createNewPunishment(
          punishmentRequest = it,
          hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
        ),
      )
    }

    return saveToDto(reportedAdjudication).also {
      it.suspendedPunishmentEvents = suspendedPunishmentEvents
      it.supplementalLossOfVisitsEvents = supplementalLossOfVisitsEvents
      it.lossOfVisitsChangeType = LossOfVisitsChangeType.AWARDED.takeIf { lossOfVisitsAwarded }
    }
  }

  fun update(
    chargeNumber: String,
    punishments: List<PunishmentRequest>,
  ): ReportedAdjudicationDto {
    validateNoConsecutiveLoop(chargeNumber, punishments)

    val reportedAdjudication = findByChargeNumber(chargeNumber).also {
      it.validateCanAddPunishments()
    }
    val visitsPunishmentsBefore = reportedAdjudication.getPunishments().toVisitsPunishmentStates()

    val suspendedPunishmentEvents = mutableSetOf<SuspendedPunishmentEvent>()
    val supplementalLossOfVisitsEvents = mutableListOf<LossOfVisitsEventDto>()

    punishments.validateCaution()
    val idsToUpdate = punishments.filter { it.id != null }.map { it.id!! }
    deactivateActivatedPunishments(
      chargeNumber = chargeNumber,
      idsToIgnore = idsToUpdate,
    ).also { updates ->
      suspendedPunishmentEvents.addAll(updates.events)
      supplementalLossOfVisitsEvents.addAll(updates.lossOfVisitsEvents)
    }
    reportedAdjudication.deletePunishments(idsToIgnore = idsToUpdate)

    punishments.filter { it.activatedFrom != null }.let {
      if (it.isNotEmpty()) {
        activateSuspendedPunishments(
          reportedAdjudication = reportedAdjudication,
          toActivate = it,
        ).also { updates ->
          suspendedPunishmentEvents.addAll(updates.events)
          supplementalLossOfVisitsEvents.addAll(updates.lossOfVisitsEvents)
        }
      }
    }

    punishments.filter { it.activatedFrom == null }.forEach { punishmentRequest ->

      if (punishmentRequest.id == null) {
        punishmentRequest.validateRequest(
          latestHearing = reportedAdjudication.getLatestHearing(),
          isYouthOffender = reportedAdjudication.isYouthOffender,
        )
        reportedAdjudication.addPunishment(
          createNewPunishment(
            punishmentRequest = punishmentRequest,
            hearingDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate(),
          ),
        )
      } else {
        val punishmentToAmend = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentRequest.id)
        punishmentRequest.validateRequest(
          latestHearing = reportedAdjudication.getLatestHearing(),
          isYouthOffender = reportedAdjudication.isYouthOffender,
          punishmentToAmend = punishmentToAmend,
        )
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
      it.supplementalLossOfVisitsEvents = supplementalLossOfVisitsEvents
      it.lossOfVisitsChangeType = lossOfVisitsChangeType(
        before = visitsPunishmentsBefore,
        after = it.punishments.toVisitsPunishmentDtoStates(),
      )
    }
  }

  fun completeRehabilitativeActivity(
    chargeNumber: String,
    punishmentId: Long,
    completeRehabilitativeActivityRequest: CompleteRehabilitativeActivityRequest,
  ): ReportedAdjudicationDto {
    val reportedAdjudication = findByChargeNumber(chargeNumber = chargeNumber)
    val visitsPunishmentsBefore = reportedAdjudication.getPunishments().toVisitsPunishmentStates()
    val punishment = reportedAdjudication.getPunishments().getPunishmentToAmend(punishmentId)
    if (punishment.rehabilitativeActivities.isEmpty()) throw ValidationException("punishment $punishmentId on charge $chargeNumber has no rehabilitative activities")
    if (!completeRehabilitativeActivityRequest.completed && completeRehabilitativeActivityRequest.outcome == null) {
      throw ValidationException(
        "completed false needs outcome",
      )
    }

    val latestSchedule = punishment.latestSchedule()

    when (completeRehabilitativeActivityRequest.outcome) {
      NotCompletedOutcome.FULL_ACTIVATE -> {
        val startDate = LocalDate.now()
        punishment.addSchedule(
          PunishmentSchedule(
            startDate = startDate,
            endDate = punishment.type.activationEndDate(startDate, requireNotNull(latestSchedule.duration)),
            duration = latestSchedule.duration,
          ),
        )
      }

      NotCompletedOutcome.PARTIAL_ACTIVATE -> {
        val daysToActivate = completeRehabilitativeActivityRequest.daysToActivate
          ?: throw ValidationException("PARTIAL_ACTIVATE requires daysToActivate")
        if (punishment.type.isVisitsPunishment()) {
          val maximumDuration = minOf(
            requireNotNull(latestSchedule.duration),
            requireNotNull(punishment.type.maximumDuration),
          )
          if (daysToActivate !in 1..maximumDuration) {
            throw ValidationException("daysToActivate for ${punishment.type} must be between 1 and $maximumDuration days")
          }
        }
        val startDate = LocalDate.now()
        punishment.addSchedule(
          PunishmentSchedule(
            startDate = startDate,
            endDate = punishment.type.activationEndDate(startDate, daysToActivate),
            duration = daysToActivate,
          ),
        )
      }

      NotCompletedOutcome.EXT_SUSPEND -> {
        completeRehabilitativeActivityRequest.suspendedUntil
          ?: throw ValidationException("EXT_SUSPEND requires a suspendedUntil")
        punishment.addSchedule(
          PunishmentSchedule(
            suspendedUntil = completeRehabilitativeActivityRequest.suspendedUntil,
            duration = latestSchedule.duration,
          ),
        )
      }
      // SPIKE this logic is present in case we allow replay, and it will remove the activation
      NotCompletedOutcome.NO_ACTION, null -> {
        punishment.rehabCompleted?.let {
          punishment.rehabNotCompletedOutcome.let {
            when (it) {
              NotCompletedOutcome.FULL_ACTIVATE, NotCompletedOutcome.PARTIAL_ACTIVATE, NotCompletedOutcome.EXT_SUSPEND -> punishment.removeSchedule(
                punishment.latestSchedule(),
              )

              else -> {}
            }
          }
        }
      }
    }
    punishment.rehabCompleted = completeRehabilitativeActivityRequest.completed
    punishment.rehabNotCompletedOutcome = completeRehabilitativeActivityRequest.outcome

    return saveToDto(reportedAdjudication).also {
      it.lossOfVisitsChangeType = lossOfVisitsChangeType(
        before = visitsPunishmentsBefore,
        after = it.punishments.toVisitsPunishmentDtoStates(),
      )
    }
  }

  private fun activateSuspendedPunishments(
    reportedAdjudication: ReportedAdjudication,
    toActivate: List<PunishmentRequest>,
  ): SuspendedPunishmentUpdates {
    val updatedReports = linkedMapOf<String, ReportedAdjudication>()
    val reportsWithVisitsChanges = mutableSetOf<String>()
    val reportsActivatedFrom = findByChargeNumberIn(toActivate.map { it.activatedFrom!! }.distinct())

    toActivate.forEach { punishment ->
      punishment.id ?: throw ValidationException("Suspended punishment activation missing punishment id to activate")
      val reportToUpdate = reportsActivatedFrom.firstOrNull { it.chargeNumber == punishment.activatedFrom!! }
        ?: throw EntityNotFoundException("activated from charge ${punishment.activatedFrom} not found")

      reportToUpdate.getPunishments().getSuspendedPunishmentToActivate(id = punishment.id)?.let { punishmentToActivate ->
        if (
          punishment.type != punishmentToActivate.type &&
          (punishment.type.isVisitsPunishment() || punishmentToActivate.type.isVisitsPunishment())
        ) {
          throw ValidationException("punishment type does not match suspended punishment ${punishment.id}")
        }
        punishment.validateRequest(
          latestHearing = reportedAdjudication.getLatestHearing(),
          isYouthOffender = reportToUpdate.isYouthOffender,
        )
        if (punishment.type.isVisitsPunishment() && punishment.duration != punishmentToActivate.latestSchedule().duration) {
          throw ValidationException("duration does not match suspended social visits punishment ${punishment.id}")
        }
        if (punishment.type.isVisitsPunishment() && punishment.hasChildUnder18 != punishmentToActivate.hasChildUnder18) {
          throw ValidationException("hasChildUnder18 does not match suspended social visits punishment ${punishment.id}")
        }

        punishmentToActivate.activatedByChargeNumber = reportedAdjudication.chargeNumber
        punishmentToActivate.addSchedule(
          PunishmentSchedule(
            duration = punishmentToActivate.latestSchedule().duration,
            startDate = punishment.startDate,
            endDate = punishment.endDateForPersistence(),
            measurement = punishment.type.measurement,
          ),
        )
        updatedReports[reportToUpdate.chargeNumber] = reportToUpdate
        if (punishmentToActivate.type.isVisitsPunishment()) reportsWithVisitsChanges.add(reportToUpdate.chargeNumber)
      }
    }

    val events = updatedReports.values.map { report ->
      SuspendedPunishmentEvent(
        agencyId = report.originatingAgencyId,
        chargeNumber = report.chargeNumber,
        status = report.status,
      )
    }.toSet()

    return SuspendedPunishmentUpdates(
      events = events,
      lossOfVisitsEvents = updatedReports.values
        .filter { reportsWithVisitsChanges.contains(it.chargeNumber) }
        .map { it.toLossOfVisitsEvent(LossOfVisitsChangeType.UPDATED) },
    )
  }

  private fun validateNoConsecutiveLoop(chargeNumber: String, punishments: List<PunishmentRequest>) {
    val requestedConsecutiveTargets = punishments
      .filter { PunishmentType.additionalDays().contains(it.type) }
      .mapNotNull { it.consecutiveChargeNumber }
      .toSet()
    if (requestedConsecutiveTargets.isEmpty()) return

    if (requestedConsecutiveTargets.contains(chargeNumber)) {
      throw ValidationException("a punishment cannot be consecutive to its own charge $chargeNumber")
    }

    val chargesConsecutiveToThis = chargesConsecutiveTo(chargeNumber, PunishmentType.additionalDays()).toSet()
    requestedConsecutiveTargets.firstOrNull { chargesConsecutiveToThis.contains(it) }?.let {
      throw ValidationException("charge $chargeNumber cannot be consecutive to $it because $it is already consecutive to this charge")
    }
  }

  private fun PunishmentType.consecutiveReportValidation(chargeNumber: String) {
    if (PunishmentType.additionalDays().contains(this)) {
      if (isLinkedToReportV2(chargeNumber, listOf(this))) {
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

  private fun createNewPunishment(punishmentRequest: PunishmentRequest, hearingDate: LocalDate? = null): Punishment = Punishment(
    type = punishmentRequest.type,
    hasChildUnder18 = punishmentRequest.hasChildUnder18,
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
          endDate = punishmentRequest.endDateForPersistence(),
          suspendedUntil = punishmentRequest.suspendedUntil,
          measurement = punishmentRequest.type.measurement,
        ),
      )
    },
  )

  private fun updatePunishment(punishment: Punishment, punishmentRequest: PunishmentRequest) = punishment.let {
    it.hasChildUnder18 = punishmentRequest.hasChildUnder18
    it.privilegeType = punishmentRequest.privilegeType
    it.otherPrivilege = punishmentRequest.otherPrivilege
    it.stoppagePercentage = punishmentRequest.stoppagePercentage
    it.consecutiveToChargeNumber = punishmentRequest.consecutiveChargeNumber
    it.amount = punishmentRequest.damagesOwedAmount
    it.paybackNotes = punishmentRequest.paybackNotes
    // due to edit of activities in session, simpler to delete and insert
    it.rehabilitativeActivities.clear()
    it.rehabilitativeActivities.addAll(
      punishmentRequest.rehabilitativeActivities.map { ra ->
        RehabilitativeActivity(
          details = ra.details,
          monitor = ra.monitor,
          endDate = ra.endDate,
          totalSessions = ra.totalSessions,
        )
      },
    )
    val latestSchedule = it.latestSchedule()
    if (latestSchedule.hasScheduleBeenUpdated(punishmentRequest) &&
      !PunishmentType.damagesAndCaution()
        .contains(it.type)
    ) {
      it.addSchedule(
        PunishmentSchedule(
          duration = punishmentRequest.duration,
          startDate = if (it.type == PunishmentType.PAYBACK) latestSchedule.startDate else punishmentRequest.startDate,
          endDate = punishmentRequest.endDateForPersistence(),
          suspendedUntil = punishmentRequest.suspendedUntil,
          measurement = punishmentRequest.type.measurement,
        ),
      )
    }
  }

  private fun PunishmentRequest.validateRequest(
    latestHearing: Hearing?,
    isYouthOffender: Boolean,
    punishmentToAmend: Punishment? = null,
  ) {
    if (this.type.isVisitsPunishment()) {
      if (isYouthOffender) {
        throw ValidationException("social visits punishments are only valid for adult adjudications")
      }

      if (this.hasChildUnder18 == null) {
        throw ValidationException("hasChildUnder18 missing for social visits punishment")
      }

      val maximumDuration = this.type.maximumDuration!!
      if (this.duration == null || this.duration !in 1..maximumDuration) {
        throw ValidationException("duration for ${this.type} must be between 1 and $maximumDuration days")
      }

      if (this.suspendedUntil != null && (this.startDate != null || this.endDate != null)) {
        throw ValidationException("a suspended social visits punishment must not have start or end dates")
      }

      if (this.suspendedUntil == null) {
        this.startDate ?: throw ValidationException("missing start date for social visits punishment")
        val calculatedEndDate = requireNotNull(this.endDateForPersistence())
        if (this.endDate != null && this.endDate != calculatedEndDate) {
          throw ValidationException("social visits punishment dates must match its duration")
        }
      }
    }

    when (this.type) {
      PunishmentType.DAMAGES_OWED ->
        this.damagesOwedAmount
          ?: throw ValidationException("amount missing for type DAMAGES_OWED")

      PunishmentType.PRIVILEGE -> {
        this.privilegeType ?: throw ValidationException("subtype missing for type PRIVILEGE")
        if (this.privilegeType == PrivilegeType.OTHER) {
          this.otherPrivilege ?: throw ValidationException("description missing for type PRIVILEGE - sub type OTHER")
        }
      }

      PunishmentType.EARNINGS ->
        this.stoppagePercentage
          ?: throw ValidationException("stoppage percentage missing for type EARNINGS")

      PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS -> if (!OicHearingType.inadTypes()
          .contains(latestHearing?.oicHearingType)
      ) {
        throw ValidationException("Punishment ${this.type} is invalid as the punishment decision was not awarded by an independent adjudicator")
      }

      else -> {}
    }
    when (this.type) {
      PunishmentType.PROSPECTIVE_DAYS, PunishmentType.ADDITIONAL_DAYS, PunishmentType.DAMAGES_OWED, PunishmentType.CAUTION, PunishmentType.PAYBACK -> {}
      else -> {
        this.suspendedUntil ?: this.startDate ?: this.endDate ?: throw ValidationException("missing all schedule data")
        this.suspendedUntil ?: this.startDate ?: throw ValidationException("missing start date for schedule")
        if (!this.type.isVisitsPunishment()) {
          this.suspendedUntil ?: this.endDate ?: throw ValidationException("missing end date for schedule")
        }
      }
    }
    if (this.rehabilitativeActivities.isNotEmpty()) {
      if (OicHearingType.govTypes().none { it == latestHearing?.oicHearingType }) {
        throw ValidationException("only GOV can award rehabilitative activities")
      }
      if (!this.type.rehabilitativeActivitiesAllowed) throw ValidationException("punishment type does not support rehabilitative activities")
      if (punishmentToAmend?.rehabCompleted == null) {
        this.suspendedUntil
          ?: throw ValidationException("only suspended punishments can have rehabilitative activities")
      }
    }
  }

  companion object {

    fun List<Punishment>.getSuspendedPunishmentToActivate(id: Long): Punishment? {
      val punishmentToActivate =
        this.firstOrNull { it.id == id } ?: throw EntityNotFoundException("suspended punishment not found")
      // we do no activate punishments if already activated.  Can be presented again via batch update
      return if (punishmentToActivate.activatedByChargeNumber == null) punishmentToActivate else null
    }

    fun PunishmentSchedule.hasScheduleBeenUpdated(punishmentRequest: PunishmentRequest): Boolean = this.duration != punishmentRequest.duration ||
      this.endDate != punishmentRequest.endDateForPersistence() ||
      this.startDate != punishmentRequest.startDate ||
      this.suspendedUntil != punishmentRequest.suspendedUntil

    private fun List<Punishment>.getPunishment(id: Long): Punishment? = this.firstOrNull { it.id == id }

    fun List<Punishment>.getPunishmentToAmend(id: Long): Punishment = this.getPunishment(id)
      ?: throw EntityNotFoundException("Punishment $id is not associated with ReportedAdjudication")

    fun ReportedAdjudication.validateCanAddPunishments() {
      if (!listOf(
          ReportedAdjudicationStatus.CHARGE_PROVED,
          ReportedAdjudicationStatus.INVALID_SUSPENDED,
          ReportedAdjudicationStatus.INVALID_SUSPENDED,
        ).contains(this.status)
      ) {
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

private fun PunishmentRequest.endDateForPersistence(): LocalDate? = if (this.type.isVisitsPunishment() && this.suspendedUntil == null) {
  this.startDate?.plusDays(requireNotNull(this.duration).toLong() - 1)
} else {
  this.endDate
}

private fun PunishmentType.activationEndDate(startDate: LocalDate, duration: Int): LocalDate = startDate.plusDays(
  duration.toLong() - if (isVisitsPunishment()) 1 else 0,
)

private data class VisitsPunishmentState(
  val id: Long?,
  val type: PunishmentType,
  val hasChildUnder18: Boolean?,
  val duration: Int?,
  val startDate: LocalDate?,
  val endDate: LocalDate?,
  val suspendedUntil: LocalDate?,
  val activatedByChargeNumber: String?,
)

private fun List<Punishment>.toVisitsPunishmentStates(): Set<VisitsPunishmentState> = this
  .filter { it.type.isVisitsPunishment() }
  .map {
    val schedule = it.latestScheduleForLossOfVisits()
    VisitsPunishmentState(
      id = it.id,
      type = it.type,
      hasChildUnder18 = it.hasChildUnder18,
      duration = schedule.duration,
      startDate = schedule.startDate,
      endDate = schedule.endDate,
      suspendedUntil = schedule.suspendedUntil,
      activatedByChargeNumber = it.activatedByChargeNumber,
    )
  }.toSet()

private fun Punishment.latestScheduleForLossOfVisits() = getSchedule()
  .withIndex()
  .maxWith(
    compareBy<IndexedValue<PunishmentSchedule>>(
      { it.value.createDateTime ?: LocalDateTime.MAX },
      { it.index },
    ),
  ).value

private fun List<PunishmentDto>.toVisitsPunishmentDtoStates(): Set<VisitsPunishmentState> = this
  .filter { it.type.isVisitsPunishment() }
  .map {
    VisitsPunishmentState(
      id = it.id,
      type = it.type,
      hasChildUnder18 = it.hasChildUnder18,
      duration = it.schedule.duration,
      startDate = it.schedule.startDate,
      endDate = it.schedule.endDate,
      suspendedUntil = it.schedule.suspendedUntil,
      activatedByChargeNumber = it.activatedBy,
    )
  }.toSet()

private fun lossOfVisitsChangeType(
  before: Set<VisitsPunishmentState>,
  after: Set<VisitsPunishmentState>,
): LossOfVisitsChangeType? = when {
  before == after -> null
  before.isEmpty() -> LossOfVisitsChangeType.AWARDED
  after.isEmpty() -> LossOfVisitsChangeType.REMOVED
  else -> LossOfVisitsChangeType.UPDATED
}
