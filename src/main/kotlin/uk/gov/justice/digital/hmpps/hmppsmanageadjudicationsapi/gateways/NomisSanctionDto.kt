package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDate

enum class OicSanctionCode {
  ADA,
  ASSO,
  ASS_DINING,
  ASS_NEWS,
  ASS_SNACKS,
  BEDDG_OWN,
  BEST_JOBS,
  BONUS_PNTS,
  CANTEEN,
  CAUTION,
  CC,
  CELL_ELEC,
  CELL_FURN,
  COOK_FAC,
  EXTRA_WORK,
  EXTW,
  FOOD_CHOIC,
  FORFEIT,
  GAMES_ELEC,
  INCOM_TEL,
  MAIL_ORDER,
  MEAL_TIMES,
  OIC,
  OTHER,
  PADA,
  PIC,
  POSSESSION,
  PUBL,
  REMACT,
  REMWIN,
  STOP_EARN,
  STOP_PCT,
  TOBA,
  USE_FRIDGE,
  USE_GYM,
  USE_LAUNDRY,
  USE_LIBRARY,
  VISIT_HRS,
  ;
}

enum class Status {
  AS_AWARDED,
  AWARD_RED,
  IMMEDIATE,
  PROSPECTIVE,
  QUASHED,
  REDAPP,
  SUSPENDED,
  SUSPEN_EXT,
  SUSPEN_RED,
  SUSP_PROSP,
  ;
}

data class OffenderOicSanctionRequest(
  val oicSanctionCode: OicSanctionCode,
  val status: Status,
  val effectiveDate: LocalDate,
  val compensationAmount: Double,
  val sanctionDays: Long,
)
