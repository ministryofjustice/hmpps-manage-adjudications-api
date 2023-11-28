package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate
import java.time.LocalDateTime

enum class OicSanctionCode {
  ADA, CAUTION, CC, EXTRA_WORK, EXTW, FORFEIT, OTHER, REMACT, REMWIN, STOP_PCT, PADA, STOP_EARN
}

enum class Status {
  IMMEDIATE, PROSPECTIVE, QUASHED, SUSPENDED, SUSP_PROSP, SUSPEN_EXT, SUSPEN_RED, AWARD_RED, REDAPP,
}

data class OffenderOicSanctionRequest(
  val oicSanctionCode: OicSanctionCode,
  val status: Status,
  val effectiveDate: LocalDate,
  val compensationAmount: Double? = null,
  val sanctionDays: Long,
  val commentText: String? = null,
  val consecutiveReportNumber: Long? = null,
) {
  companion object {

    private fun PunishmentType.oicSanctionCodeMapper(): OicSanctionCode =
      when (this) {
        PunishmentType.PRIVILEGE -> OicSanctionCode.FORFEIT
        PunishmentType.EARNINGS -> OicSanctionCode.STOP_PCT
        PunishmentType.CONFINEMENT -> OicSanctionCode.CC
        PunishmentType.REMOVAL_ACTIVITY -> OicSanctionCode.REMACT
        PunishmentType.EXCLUSION_WORK -> OicSanctionCode.EXTRA_WORK
        PunishmentType.EXTRA_WORK -> OicSanctionCode.EXTW
        PunishmentType.REMOVAL_WING -> OicSanctionCode.REMWIN
        PunishmentType.ADDITIONAL_DAYS, PunishmentType.PROSPECTIVE_DAYS -> OicSanctionCode.ADA
        PunishmentType.CAUTION -> OicSanctionCode.CAUTION
        PunishmentType.DAMAGES_OWED -> OicSanctionCode.OTHER
      }

    private fun PunishmentSchedule.statusMapper(prospectiveDays: Boolean): Status =
      when (this.startDate) {
        null -> when (this.suspendedUntil) {
          null -> if (prospectiveDays) Status.PROSPECTIVE else Status.IMMEDIATE
          else -> if (prospectiveDays) Status.SUSP_PROSP else Status.SUSPENDED
        }
        else -> Status.IMMEDIATE
      }

    private fun Punishment.commentMapper(): String? =
      when (this.type) {
        PunishmentType.PRIVILEGE ->
          when (this.privilegeType) {
            PrivilegeType.OTHER -> "Loss of ${this.otherPrivilege}"
            else -> "Loss of ${this.privilegeType}"
          }
        PunishmentType.DAMAGES_OWED -> "OTHER - Damages owed £${String.format("%.2f", this.amount!!)}"
        else -> null
      }

    fun Punishment.mapPunishmentToSanction(): OffenderOicSanctionRequest {
      val latestSchedule = this.schedule.maxBy { it.createDateTime ?: LocalDateTime.now() }

      return OffenderOicSanctionRequest(
        oicSanctionCode = this.type.oicSanctionCodeMapper(),
        status = latestSchedule.statusMapper(this.type == PunishmentType.PROSPECTIVE_DAYS),
        effectiveDate = latestSchedule.suspendedUntil ?: latestSchedule.startDate ?: LocalDate.now(),
        sanctionDays = latestSchedule.days.toLong(),
        compensationAmount = this.amount ?: this.stoppagePercentage?.toDouble(),
        commentText = this.commentMapper(),
        consecutiveReportNumber = this.consecutiveChargeNumber?.toLong(),
      )
    }
  }
}
