package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import java.time.LocalDateTime

enum class OicHearingType {
  GOV_ADULT,
  GOV_YOI;

  companion object {
    fun getOicHearingType(reportedAdjudication: ReportedAdjudication): OicHearingType =
      if (reportedAdjudication.isYouthOffender) GOV_YOI else GOV_ADULT
  }
}

data class OicHearingRequest(val dateTimeOfHearing: LocalDateTime, val oicHearingType: OicHearingType, val hearingLocationId: Long)

data class OicHearingResponse(val oicHearingId: Long, val dateTimeOfHearing: LocalDateTime, val hearingLocationId: Long)
