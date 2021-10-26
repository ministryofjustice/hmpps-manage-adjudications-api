package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import java.time.LocalDateTime

data class DraftAdjudicationDto(
  val id: Long,
  val prisonerNumber: String,
  val adjudicationSent: Boolean? = false,
  val incidentDetails: IncidentDetailsDto? = null,
  val incidentStatement: IncidentStatementDto? = null
)

data class IncidentDetailsDto(
  val id: Long? = null,
  val locationId: Long,
  val dateTimeOfIncident: LocalDateTime,
)

data class IncidentStatementDto(
  val statement: String,
)
