package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType

object OicHearingTypeTransformer {
  fun displayName(status: OicHearingType): String? {
    return when (status) {
      OicHearingType.GOV -> "Governor"
      OicHearingType.GOV_YOI -> "Governor"
      OicHearingType.GOV_ADULT -> "Governor"
      OicHearingType.INAD_YOI -> "Independent Adjudicator"
      OicHearingType.INAD_ADULT -> "Independent Adjudicator"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = OicHearingType.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
