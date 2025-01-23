package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode

object WitnessCodeTransformer {
  fun displayName(status: WitnessCode): String? {
    return when (status) {
      WitnessCode.STAFF -> "A member of staff who's not a prison officer"
      WitnessCode.VICTIM -> "VICTIM"
      WitnessCode.OFFICER -> "A prison officer"
      WitnessCode.PRISONER -> "PRISONER"
      WitnessCode.OTHER_PERSON -> "A person not listed above"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = WitnessCode.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
