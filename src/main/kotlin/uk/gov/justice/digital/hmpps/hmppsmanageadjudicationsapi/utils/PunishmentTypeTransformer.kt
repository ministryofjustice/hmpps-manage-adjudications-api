package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType

object PunishmentTypeTransformer {
  fun displayName(status: PunishmentType): String? {
    return when (status) {
      PunishmentType.ADDITIONAL_DAYS -> "Additional days"
      PunishmentType.PAYBACK -> "Payback punishment"
      PunishmentType.CAUTION -> "Caution"
      PunishmentType.EARNINGS -> "Stoppage of earnings"
      PunishmentType.PRIVILEGE -> "Loss of privileges"
      PunishmentType.EXTRA_WORK -> "Extra work"
      PunishmentType.CONFINEMENT -> "Cellular confinement"
      PunishmentType.DAMAGES_OWED -> "Recovery of money for damages"
      PunishmentType.REMOVAL_WING -> "Removal from wing or unit"
      PunishmentType.EXCLUSION_WORK -> "Exclusion from associated work"
      PunishmentType.PROSPECTIVE_DAYS -> "Prospective additional days"
      PunishmentType.REMOVAL_ACTIVITY -> "Removal from activity"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = PunishmentType.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}