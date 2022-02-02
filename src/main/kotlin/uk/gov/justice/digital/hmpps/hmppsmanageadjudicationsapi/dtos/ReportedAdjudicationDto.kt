package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel(value = "Reported adjudication details")
data class ReportedAdjudicationDto(
  @ApiModelProperty(value = "The number for the reported adjudication")
  val adjudicationNumber: Long,
  @ApiModelProperty(value = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @ApiModelProperty(value = "The current booking id for a prisoner", example = "1234")
  val bookingId: Long,
  @ApiModelProperty(value = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @ApiModelProperty(value = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto,
  @ApiModelProperty(value = "Details about all the offences the prisoner is accused of")
  val offences: List<OffenceDto>,
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto,
  @ApiModelProperty("Created by user id")
  val createdByUserId: String,
  @ApiModelProperty("When the report was created")
  val createdDateTime: LocalDateTime,
)

@ApiModel(value = "Details of an offence")
data class OffenceDto(
  @ApiModelProperty(value = "The offence code", notes = "This is the paragraph number relating to the offence", example = "3")
  val offenceCode: Int,
  @ApiModelProperty(value = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
)
