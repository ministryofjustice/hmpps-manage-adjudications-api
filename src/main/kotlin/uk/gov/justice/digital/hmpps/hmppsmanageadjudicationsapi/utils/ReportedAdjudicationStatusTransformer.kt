package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus

object ReportedAdjudicationStatusTransformer {
  fun displayName(status: ReportedAdjudicationStatus): String? {
    return when (status) {
      ReportedAdjudicationStatus.AWAITING_REVIEW -> "Awaiting review"
      ReportedAdjudicationStatus.RETURNED -> "Returned"
      ReportedAdjudicationStatus.REJECTED -> "Rejected"
      ReportedAdjudicationStatus.ACCEPTED -> "Accepted"
      ReportedAdjudicationStatus.UNSCHEDULED -> "Unscheduled"
      ReportedAdjudicationStatus.SCHEDULED -> "Scheduled"
      ReportedAdjudicationStatus.ADJOURNED -> "Adjourned"
      ReportedAdjudicationStatus.NOT_PROCEED -> "Not proceeded with"
      ReportedAdjudicationStatus.DISMISSED -> "Dismissed"
      ReportedAdjudicationStatus.REFER_POLICE -> "Referred to police"
      ReportedAdjudicationStatus.REFER_INAD -> "Referred to IA"
      ReportedAdjudicationStatus.CHARGE_PROVED -> "Charge proved"
      ReportedAdjudicationStatus.QUASHED -> "Quashed"
      ReportedAdjudicationStatus.PROSECUTION -> "Police prosecution"
      ReportedAdjudicationStatus.REFER_GOV -> "Referred to Gov"
      ReportedAdjudicationStatus.INVALID_OUTCOME -> "Invalid outcome"
      ReportedAdjudicationStatus.INVALID_SUSPENDED -> "Invalid suspended"
      ReportedAdjudicationStatus.INVALID_ADA -> "Invalid added days"
    }
  }

  /**
   * If status is a string (maybe from an HTTP request), try to parse it.
   * If invalid, return null or any fallback behavior you prefer.
   */
  fun displayName(status: String): String? {
    return try {
      val enumValue = ReportedAdjudicationStatus.valueOf(status.uppercase())
      displayName(enumValue)
    } catch (ex: IllegalArgumentException) {
      // String didn't match any enum constant
      null
    }
  }
}
