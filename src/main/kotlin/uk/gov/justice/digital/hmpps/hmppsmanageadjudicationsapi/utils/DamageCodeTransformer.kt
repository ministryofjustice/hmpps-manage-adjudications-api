package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode

object DamageCodeTransformer {
  fun displayName(status: DamageCode): String? {
    return when (status) {
      DamageCode.ELECTRICAL_REPAIR -> "Electrical repair"
      DamageCode.CLEANING -> "Cleaning"
      DamageCode.LOCK_REPAIR -> "Lock repair"
      DamageCode.REDECORATION -> "Redecoration"
      DamageCode.PLUMBING_REPAIR -> "Plumbing repair"
      DamageCode.REPLACE_AN_ITEM -> "Replacing an item"
      DamageCode.FURNITURE_OR_FABRIC_REPAIR -> "Furniture or fabric repair"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = DamageCode.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
