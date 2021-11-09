package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

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
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto
)
