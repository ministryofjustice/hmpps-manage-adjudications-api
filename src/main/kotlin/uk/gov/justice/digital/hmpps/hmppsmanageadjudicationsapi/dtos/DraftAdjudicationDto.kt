package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel(value = "Draft adjudication details")
data class DraftAdjudicationDto(
  @ApiModelProperty(value = "The id for the draft adjudication")
  val id: Long,
  @ApiModelProperty(value = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @ApiModelProperty(value = "Incident details")
  val incidentDetails: IncidentDetailsDto? = null,
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto? = null
)

@ApiModel(value = "Incident details")
data class IncidentDetailsDto(
  @ApiModelProperty(value = "The id of the location the incident took place")
  val locationId: Long,
  @ApiModelProperty(value = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  val dateTimeOfIncident: LocalDateTime,
)

@ApiModel(value = "Incident statement")
data class IncidentStatementDto(
  @ApiModelProperty(value = "The statement regarding the incident")
  val statement: String,
)
