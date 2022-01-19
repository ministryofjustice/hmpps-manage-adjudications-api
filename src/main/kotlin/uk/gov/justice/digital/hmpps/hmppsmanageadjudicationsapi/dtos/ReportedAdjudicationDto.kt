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
  @ApiModelProperty(value = "When this report will expire", example = "2010-10-12T10:00:00")
  val dateTimeReportExpires: LocalDateTime,
  @ApiModelProperty(value = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @ApiModelProperty(value = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto,
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto,
  @ApiModelProperty("Created by user id")
  val createdByUserId: String,
  @ApiModelProperty("When the report was created")
  val createdDateTime: LocalDateTime,
)
