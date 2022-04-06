package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
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
  val offenceDetails: List<OffenceDto>,
  @ApiModelProperty(value = "Incident statement")
  val incidentStatement: IncidentStatementDto,
  @ApiModelProperty("Created by user id")
  val createdByUserId: String,
  @ApiModelProperty("When the report was created")
  val createdDateTime: LocalDateTime,
  @ApiModelProperty("The status of the reported adjudication")
  val status: ReportedAdjudicationStatus,
  @ApiModelProperty("The reason for the status of the reported adjudication")
  val statusReason: String?,
  @ApiModelProperty("The description for the status of the reported adjudication")
  val statusDetails: String?,
)

@ApiModel(value = "Details of an offence")
data class OffenceDto(
  @ApiModelProperty(value = "The offence code", notes = "This is the paragraph number relating to the offence", example = "3")
  val offenceCode: Int,
  @ApiModelProperty(value = "The offence rules they have broken")
  val offenceRule: OffenceRuleDto,
  @ApiModelProperty(value = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @ApiModelProperty(value = "The username of the member of staff who is a victim of the incident, if relevant", example = "ABC12D")
  val victimStaffUsername: String? = null,
  @ApiModelProperty(value = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant", example = "Bob Hope")
  val victimOtherPersonsName: String? = null,
)

@ApiModel(value = "Details of a rule they have broken")
data class OffenceRuleDto(
  @ApiModelProperty(value = "The paragraph number relating to the offence rule they have been alleged to have broken", example = "25(a)")
  val paragraphNumber: String,
  @ApiModelProperty(value = "The description relating to the paragraph number", example = "Committed an assault")
  val paragraphDescription: String,
)
