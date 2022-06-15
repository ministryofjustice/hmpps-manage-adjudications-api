package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

@Schema(description = "Reported adjudication details")
data class ReportedAdjudicationDto(
  @Schema(description = "The number for the reported adjudication")
  val adjudicationNumber: Long,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "The current booking id for a prisoner", example = "1234")
  val bookingId: Long,
  @Schema(description = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @Schema(description = "Is classified as a youth offender")
  val isYouthOffender: Boolean,
  @Schema(description = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto,
  @Schema(description = "Details about all the offences the prisoner is accused of")
  val offenceDetails: List<OffenceDto>,
  @Schema(description = "Incident statement")
  val incidentStatement: IncidentStatementDto,
  @Schema(description = "Created by user id")
  val createdByUserId: String,
  @Schema(description = "When the report was created")
  val createdDateTime: LocalDateTime,
  @Schema(description = "The status of the reported adjudication")
  val status: ReportedAdjudicationStatus,
  @Schema(description = "The reason for the status of the reported adjudication")
  val statusReason: String?,
  @Schema(description = "The name for the status of the reported adjudication")
  val statusDetails: String?,
)

@Schema(description = "Details of an offence")
data class OffenceDto(
  @Schema(description = "The offence code", example = "3")
  val offenceCode: Int,
  @Schema(description = "The offence rules they have broken")
  val offenceRule: OffenceRuleDto,
  @Schema(description = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @Schema(description = "The username of the member of staff who is a victim of the incident, if relevant", example = "ABC12D")
  val victimStaffUsername: String? = null,
  @Schema(description = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant", example = "Bob Hope")
  val victimOtherPersonsName: String? = null,
)

@Schema(description = "Details of a rule they have broken")
data class OffenceRuleDto(
  @Schema(description = "The paragraph number relating to the offence rule they have been alleged to have broken", example = "25(a)")
  val paragraphNumber: String,
  @Schema(description = "The name relating to the paragraph description", example = "Committed an assault")
  val paragraphDescription: String,
)
