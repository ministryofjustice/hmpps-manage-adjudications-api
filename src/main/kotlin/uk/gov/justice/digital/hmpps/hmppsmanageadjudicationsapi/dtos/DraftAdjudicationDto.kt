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
  @ApiModelProperty(value = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto,
  @ApiModelProperty(value = "Details about all the offences the prisoner is accused of")
  val offenceDetails: List<OffenceDetailsDto>? = null,
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

@ApiModel(value = "Incident Role")
data class IncidentRoleDto(
  @ApiModelProperty(value = "The incident role code", notes = "If not set then it is assumed they committed the offence on their own", example = "25a")
  val roleCode: String?,
  @ApiModelProperty(value = "The prison number of the other prisoner involved in the incident", notes = "This only applies to role codes 25b and 25c", example = "G2996UX")
  val associatedPrisonersNumber: String?,
)

@ApiModel(value = "Details of an offence")
data class OffenceDetailsDto(
  @ApiModelProperty(value = "The offence code", notes = "This is the unique number relating to the type of offence they have been alleged to have committed", example = "3")
  val offenceCode: Int,
  @ApiModelProperty(value = "The offence rules they have broken")
  val offenceRule: OffenceRuleDetailsDto,
  @ApiModelProperty(value = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @ApiModelProperty(value = "The username of the member of staff who is a victim of the incident, if relevant", example = "ABC12D")
  val victimStaffUsername: String? = null,
  @ApiModelProperty(value = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant", example = "Bob Hope")
  val victimOtherPersonsName: String? = null,
)

@ApiModel(value = "Details of a rule they have broken")
data class OffenceRuleDetailsDto(
  @ApiModelProperty(value = "The paragraph number relating to the offence rule they have been alleged to have broken", example = "25(a)")
  val paragraphNumber: String,
  @ApiModelProperty(value = "The description relating to the paragraph number", example = "Committed an assault")
  val paragraphDescription: String,
)

@ApiModel(value = "Incident statement")
data class IncidentStatementDto(
  @ApiModelProperty(value = "The statement regarding the incident")
  val statement: String,
  @ApiModelProperty(value = "Indicates when the statement is complete")
  val completed: Boolean? = false,
)
