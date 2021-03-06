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
  val isYouthOffender: Boolean,
  val incidentRoleCode: String,
  val incidentRoleParagraphNumber: String,
  val incidentRoleParagraphDescription: String,
  val incidentRoleAssociatedPrisonersNumber: String,
  val offences: List<OffenceTestDataSet>,
  val statement: String,
  val createdByUserId: String
)

data class OffenceTestDataSet(
  val offenceCode: Int,
  val paragraphNumber: String,
  val paragraphDescription: String,
  val victimPrisonersNumber: String? = null,
  val victimStaffUsername: String? = null,
  val victimOtherPersonsName: String? = null,
)

data class NomisOffenceTestDataSet(
  val nomisCodes: List<String>,
  val victimStaffUsernames: List<String>,
  val victimPrisonersNumbers: List<String>,
)
