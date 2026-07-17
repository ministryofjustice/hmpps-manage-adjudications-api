package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDate
import java.time.LocalDateTime

enum class LossOfVisitsChangeType {
  AWARDED,
  UPDATED,
  REMOVED,
  QUASHED,
  UNQUASHED,
}

data class LossOfVisitsPunishmentDto(
  val punishmentId: Long,
  val type: PunishmentType,
  val duration: Int,
  val measurement: Measurement,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val suspendedUntil: LocalDate? = null,
  val activatedByChargeNumber: String? = null,
  val hasChildUnder18: Boolean,
)

data class LossOfVisitsDetailsDto(
  val changeType: LossOfVisitsChangeType,
  val punishments: List<LossOfVisitsPunishmentDto>,
)

data class LossOfVisitsEventDto(
  val chargeNumber: String,
  val prisonerNumber: String,
  val prisonId: String,
  val status: ReportedAdjudicationStatus,
  val details: LossOfVisitsDetailsDto,
)

fun ReportedAdjudicationDto.toLossOfVisitsEvent(changeType: LossOfVisitsChangeType) = LossOfVisitsEventDto(
  chargeNumber = chargeNumber,
  prisonerNumber = prisonerNumber,
  prisonId = originatingAgencyId,
  status = status,
  details = LossOfVisitsDetailsDto(
    changeType = changeType,
    punishments = punishments
      .filter { it.type.isVisitsPunishment() }
      .map { it.toLossOfVisitsPunishment() }
      .sortedBy { it.punishmentId },
  ),
)

fun ReportedAdjudication.toLossOfVisitsEvent(changeType: LossOfVisitsChangeType) = LossOfVisitsEventDto(
  chargeNumber = chargeNumber,
  prisonerNumber = prisonerNumber,
  prisonId = originatingAgencyId,
  status = status,
  details = LossOfVisitsDetailsDto(
    changeType = changeType,
    punishments = getPunishments()
      .filter { it.type.isVisitsPunishment() }
      .map { it.toLossOfVisitsPunishment() }
      .sortedBy { it.punishmentId },
  ),
)

private fun PunishmentDto.toLossOfVisitsPunishment() = LossOfVisitsPunishmentDto(
  punishmentId = requireNotNull(id) { "Social visits punishment must be persisted before publishing an event" },
  type = type,
  duration = requireNotNull(schedule.duration) { "Social visits punishment must have a duration" },
  measurement = schedule.measurement,
  startDate = schedule.startDate,
  endDate = schedule.endDate,
  suspendedUntil = schedule.suspendedUntil,
  activatedByChargeNumber = activatedBy,
  hasChildUnder18 = requireNotNull(hasChildUnder18) { "Social visits punishment must record hasChildUnder18" },
)

private fun Punishment.toLossOfVisitsPunishment(): LossOfVisitsPunishmentDto {
  val latestSchedule = latestScheduleForLossOfVisits()

  return LossOfVisitsPunishmentDto(
    punishmentId = requireNotNull(id) { "Social visits punishment must be persisted before publishing an event" },
    type = type,
    duration = requireNotNull(latestSchedule.duration) { "Social visits punishment must have a duration" },
    measurement = latestSchedule.measurement,
    startDate = latestSchedule.startDate,
    endDate = latestSchedule.endDate,
    suspendedUntil = latestSchedule.suspendedUntil,
    activatedByChargeNumber = activatedByChargeNumber,
    hasChildUnder18 = requireNotNull(hasChildUnder18) { "Social visits punishment must record hasChildUnder18" },
  )
}

private fun Punishment.latestScheduleForLossOfVisits() = getSchedule()
  .withIndex()
  .maxWith(
    compareBy<IndexedValue<PunishmentSchedule>>(
      { it.value.createDateTime ?: LocalDateTime.MAX },
      { it.index },
    ),
  ).value
