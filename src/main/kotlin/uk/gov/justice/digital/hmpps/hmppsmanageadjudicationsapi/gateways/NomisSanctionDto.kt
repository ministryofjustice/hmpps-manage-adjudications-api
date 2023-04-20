package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate
import java.time.LocalDateTime

enum class OicSanctionCode {
  ADA, CAUTION, CC, EXTRA_WORK, EXTW, FORFEIT, OTHER, REMACT, REMWIN, STOP_PCT
}

enum class Status {
  IMMEDIATE, PROSPECTIVE, QUASHED, SUSPENDED
}

data class OffenderOicSanctionRequest(
  val oicSanctionCode: OicSanctionCode,
  val status: Status,
  val effectiveDate: LocalDate,
  val compensationAmount: Double? = null,
  val sanctionDays: Long,
  val comment: String? = null,
) {
  companion object {

    private fun oicSanctionCodeMapper(punishmentType: PunishmentType): OicSanctionCode =
      when (punishmentType) {
        PunishmentType.PRIVILEGE -> OicSanctionCode.FORFEIT
        PunishmentType.EARNINGS -> OicSanctionCode.STOP_PCT
        PunishmentType.CONFINEMENT -> OicSanctionCode.CC
        PunishmentType.REMOVAL_ACTIVITY -> OicSanctionCode.REMACT
        PunishmentType.EXCLUSION_WORK -> OicSanctionCode.EXTRA_WORK
        PunishmentType.EXTRA_WORK -> OicSanctionCode.EXTW
        PunishmentType.REMOVAL_WING -> OicSanctionCode.REMWIN
        PunishmentType.ADDITIONAL_DAYS, PunishmentType.PROSPECTIVE_DAYS -> OicSanctionCode.ADA
      }

    private fun PunishmentSchedule.statusMapper(prospectiveDays: Boolean): Status =
      when (this.startDate) {
        null -> when (this.suspendedUntil) {
          null -> if (prospectiveDays) Status.PROSPECTIVE else Status.IMMEDIATE
          else -> Status.SUSPENDED
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
        else -> null
      }

    fun Punishment.mapPunishmentToSanction(): OffenderOicSanctionRequest {
      val latestSchedule = this.schedule.maxBy { it.createDateTime ?: LocalDateTime.now() }

      return OffenderOicSanctionRequest(
        oicSanctionCode = oicSanctionCodeMapper(punishmentType = this.type),
        status = latestSchedule.statusMapper(this.type == PunishmentType.PROSPECTIVE_DAYS),
        effectiveDate = latestSchedule.suspendedUntil ?: latestSchedule.startDate ?: LocalDate.now(),
        sanctionDays = latestSchedule.days.toLong(),
        compensationAmount = this.stoppagePercentage?.toDouble(),
        comment = this.commentMapper(),
      )
    }
  }
}
