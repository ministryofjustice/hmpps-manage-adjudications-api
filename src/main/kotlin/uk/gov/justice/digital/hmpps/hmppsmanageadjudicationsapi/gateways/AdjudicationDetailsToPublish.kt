package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

data class AdjudicationDetailsToPublish(
  val offenderNo: String,
  val incidentLocationId: Long,
  val incidentTime: LocalDateTime,
  val statement: String
)
