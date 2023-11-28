package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import java.time.LocalDateTime

data class NomisAdjudication(
  val adjudicationNumber: Long,
  val reporterStaffId: Long,
  val offenderNo: String,
  val agencyId: String,
  val incidentTime: LocalDateTime,
  val incidentLocationId: Long,
  val statement: String,
  val createdByUserId: String? = null,
)
