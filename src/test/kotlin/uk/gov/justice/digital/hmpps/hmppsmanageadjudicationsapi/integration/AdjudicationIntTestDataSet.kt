package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import java.time.LocalDateTime

data class AdjudicationIntTestDataSet(
  val adjudicationNumber: Long,
  val prisonerNumber: String,
  val bookingId: Long,
  val agencyId: String,
  val locationId: Long,
  val dateTimeOfIncident: LocalDateTime,
  val dateTimeOfIncidentISOString: String,
  val handoverDeadlineISOString: String,
  val incidentRoleCode: String,
  val incidentRoleAssociatedPrisonersNumber: String,
  val offences: List<OffenceTestDataSet>,
  val statement: String,
  val createdByUserId: String
)

data class OffenceTestDataSet(
  val offenceCode: Int,
  val victimPrisonersNumber: String? = null
)
