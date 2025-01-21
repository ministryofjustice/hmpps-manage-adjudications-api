package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode

object EvidenceCodeTransformer {
  fun displayName(status: EvidenceCode): String? {
    return when (status) {
      EvidenceCode.CCTV -> "CCTV"
      EvidenceCode.PHOTO -> "Photo"
      EvidenceCode.OTHER -> "Other"
      EvidenceCode.BODY_WORN_CAMERA -> "Body-worn camera"
      EvidenceCode.BAGGED_AND_TAGGED -> "Bagged and tagged evidence"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = EvidenceCode.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
