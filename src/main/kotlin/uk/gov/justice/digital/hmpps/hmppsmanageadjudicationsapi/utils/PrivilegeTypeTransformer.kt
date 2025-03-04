package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType

object PrivilegeTypeTransformer {
  fun displayName(status: PrivilegeType): String = when (status) {
    PrivilegeType.GYM -> "Gym"
    PrivilegeType.TV -> "TV"
    PrivilegeType.OTHER -> "Other"
    PrivilegeType.MONEY -> "Use of private cash"
    PrivilegeType.CANTEEN -> "Canteen"
    PrivilegeType.FACILITIES -> "Facilities to purchase"
    PrivilegeType.ASSOCIATION -> "Association"
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? = try {
    val enumValue = PrivilegeType.valueOf(status.uppercase())
    displayName(enumValue)
  } catch (ex: IllegalArgumentException) {
    // String didn't match any enum constant
    null
  }
}
