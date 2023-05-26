package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

data class NomisAdjudication(
  val adjudicationNumber: String,
  val reporterStaffId: Long,
  val offenderNo: String,
  val bookingId: Long,
  val agencyId: String,
  val incidentTime: LocalDateTime,
  val incidentLocationId: Long,
  val statement: String,
  val createdByUserId: String,
)
