package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange

object PunishmentCommentTransformer {
  fun displayName(status: ReasonForChange): String? {
    return when (status) {
      ReasonForChange.APPEAL -> "The punishments have been changed after an appeal"
      ReasonForChange.CORRECTION -> "To make a correction"
      ReasonForChange.OTHER -> "Other"
      ReasonForChange.GOV_OR_DIRECTOR -> "A Governor or Director has decided to terminate or change punishments for another reason"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = ReasonForChange.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
