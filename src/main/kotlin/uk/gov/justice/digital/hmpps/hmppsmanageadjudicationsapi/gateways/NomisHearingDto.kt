package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

enum class OicHearingType {
  GOV_ADULT,
  GOV_YOI,
  INAD_YOI,
  GOV,
  INAD_ADULT;
}

data class OicHearingRequest(val dateTimeOfHearing: LocalDateTime, val oicHearingType: OicHearingType, val hearingLocationId: Long)

data class OicHearingResponse(val oicHearingId: Long, val dateTimeOfHearing: LocalDateTime, val hearingLocationId: Long)
