package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate

enum class OicSanctionCode {
  ADA, ASSO, ASS_DINING, ASS_NEWS, ASS_SNACKS, BEDDG_OWN, BEST_JOBS, BONUS_PNTS, CANTEEN, CAUTION, CC, CELL_ELEC, CELL_FURN, COOK_FAC, EXTRA_WORK,
  EXTW, FOOD_CHOIC, FORFEIT, GAMES_ELEC, INCOM_TEL, MAIL_ORDER, MEAL_TIMES, OIC, OTHER, PADA, PIC, POSSESSION, PUBL, REMACT, REMWIN, STOP_EARN,
  STOP_PCT, TOBA, USE_FRIDGE, USE_GYM, USE_LAUNDRY, USE_LIBRARY, VISIT_HRS,
}

enum class Status {
  AS_AWARDED, AWARD_RED, IMMEDIATE, PROSPECTIVE, QUASHED, REDAPP, SUSPENDED, SUSPEN_EXT, SUSPEN_RED, SUSP_PROSP,
}

data class OffenderOicSanctionRequest(
  val oicSanctionCode: OicSanctionCode,
  val status: Status,
  val effectiveDate: LocalDate,
  val compensationAmount: Double? = null,
  val sanctionDays: Long,
) {
  companion object {

    private fun oicSanctionCodeMapper(punishmentType: PunishmentType, privilegeType: PrivilegeType?): OicSanctionCode =
      when (punishmentType) {
        PunishmentType.PRIVILEGE -> when (privilegeType) {
          PrivilegeType.CANTEEN -> OicSanctionCode.CANTEEN
          PrivilegeType.FACILITIES -> TODO()
          PrivilegeType.MONEY -> TODO()
          PrivilegeType.TV -> TODO()
          PrivilegeType.ASSOCIATION -> OicSanctionCode.ASSO
          PrivilegeType.OTHER -> OicSanctionCode.OTHER
          null -> throw RuntimeException("fatal - no privilege type specified")
        }
        PunishmentType.EARNINGS -> OicSanctionCode.STOP_PCT
        PunishmentType.CONFINEMENT -> OicSanctionCode.CC
        PunishmentType.REMOVAL_ACTIVITY -> OicSanctionCode.REMACT
        PunishmentType.EXCLUSION_WORK -> TODO()
        PunishmentType.EXTRA_WORK -> TODO()
        PunishmentType.REMOVAL_WING -> OicSanctionCode.REMACT
        PunishmentType.ADDITIONAL_DAYS -> TODO()
        PunishmentType.PROSPECTIVE_DAYS -> TODO()
      }

    private fun PunishmentSchedule.statusMapper(): Status =
      when (this.startDate) {
        null -> TODO()
        else -> Status.AS_AWARDED
      }

    fun Punishment.mapPunishmentToSanction(damagesOwed: Double?): OffenderOicSanctionRequest {
      val latestSchedule = this.schedule.maxBy { it.createDateTime!! }

      return OffenderOicSanctionRequest(
        oicSanctionCode = oicSanctionCodeMapper(
          punishmentType = this.type,
          privilegeType = this.privilegeType,
        ),
        status = latestSchedule.statusMapper(),
        effectiveDate = latestSchedule.suspendedUntil ?: latestSchedule.startDate!!,
        sanctionDays = latestSchedule.days.toLong(),
        compensationAmount = damagesOwed,
      )
    }
  }
}
