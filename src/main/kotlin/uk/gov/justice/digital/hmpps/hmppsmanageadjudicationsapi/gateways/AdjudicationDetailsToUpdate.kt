package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

data class AdjudicationDetailsToUpdate(
  val incidentLocationId: Long,
  val incidentTime: LocalDateTime,
  val statement: String,
  val offenceCodes: List<String>,
  val victimStaffUsernames: List<String>,
  val victimOffenderIds: List<String>,
  val connectedOffenderIds: List<String>,
)
