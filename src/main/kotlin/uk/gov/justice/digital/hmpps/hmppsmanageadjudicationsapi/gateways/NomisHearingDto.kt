package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

enum class OicHearingType {
  GOV_ADULT,
  GOV_YOI,
  INAD_YOI,
  GOV,
  INAD_ADULT,
  ;

  fun isValidState(isYoungOffender: Boolean) {
    when (isYoungOffender) {
      true -> if (listOf(GOV_ADULT, INAD_ADULT).contains(this)) throw IllegalStateException("oic hearing type is not applicable for rule set")
      false -> if (listOf(GOV_YOI, INAD_YOI).contains(this)) throw IllegalStateException("oic hearing type is not applicable for rule set")
    }
  }
}

data class OicHearingRequest(
  val oicHearingId: String,
  val dateTimeOfHearing: LocalDateTime,
  val oicHearingType: OicHearingType,
  val hearingLocationId: Long,
  val adjudicator: String? = null,
  val commentText: String? = null,
)

data class OicHearingResponse(val oicHearingId: Long, val dateTimeOfHearing: LocalDateTime, val hearingLocationId: Long)
