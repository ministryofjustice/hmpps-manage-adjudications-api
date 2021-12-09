package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel(value = "Draft adjudication details")
data class DraftAdjudicationDto(
  @ApiModelProperty(value = "Draft adjudication id")
  val id: Long,
  @ApiModelProperty(value = "The number for the reported adjudication", notes = "Will only be present if this adjudication has been submitted to Prison-API", example = "4567123")
  val adjudicationNumber: Long?,
  @ApiModelProperty(value = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @ApiModelProperty(value = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto? = null,
  @ApiModelProperty("The id of the user who started the adjudication")
  val startedByUserId: String? = null,
)

@ApiModel(value = "Incident details")
data class IncidentDetailsDto(
  @ApiModelProperty(value = "The id of the location the incident took place")
  val locationId: Long,
  @ApiModelProperty(value = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  val dateTimeOfIncident: LocalDateTime,
  @ApiModelProperty(value = "When this report must be handed to the prisoner", example = "2010-10-14T10:00:00")
  val handoverDeadline: LocalDateTime,
)

@ApiModel(value = "Incident statement")
data class IncidentStatementDto(
  @ApiModelProperty(value = "The statement regarding the incident")
  val statement: String,
  @ApiModelProperty(value = "Indicates when the statement is complete")
  val completed: Boolean? = false,
)
