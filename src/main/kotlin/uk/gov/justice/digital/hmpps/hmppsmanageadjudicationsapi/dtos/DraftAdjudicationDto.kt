package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "Draft adjudication details")
data class DraftAdjudicationDto(
  @Schema(description = "Draft adjudication id")
  val id: Long,
  @Schema(description = "The number for the reported adjudication, Will only be present if this adjudication has been submitted to Prison-API", example = "4567123")
  val adjudicationNumber: Long?,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @Schema(description = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto? = null,
  @Schema(description = "Details about all the offences the prisoner is accused of")
  val offenceDetails: List<OffenceDetailsDto>? = null,
  @Schema(description = "Incident statement")
  val incidentStatement: IncidentStatementDto? = null,
  @Schema(description = "The id of the user who started the adjudication")
  val startedByUserId: String? = null,
  @Schema(description = "Is classified as a youth offender")
  val isYouthOffender: Boolean? = null,
)

@Schema(description = "Incident details")
data class IncidentDetailsDto(
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  val dateTimeOfIncident: LocalDateTime,
  @Schema(description = "When this report must be handed to the prisoner", example = "2010-10-14T10:00:00")
  val handoverDeadline: LocalDateTime,
)

@Schema(description = "Incident Role")
data class IncidentRoleDto(
  @Schema(description = "The incident role code, If not set then it is assumed they committed the offence on their own", example = "25a")
  val roleCode: String?,
  @Schema(description = "The offence rules related to the given incident role, Will not be set of there is no role code")
  val offenceRule: OffenceRuleDetailsDto?,
  @Schema(description = "The prison number of the other prisoner involved in the incident, This only applies to role codes 25b and 25c", example = "G2996UX")
  val associatedPrisonersNumber: String?,
)

@Schema(description = "Details of an offence")
data class OffenceDetailsDto(
  @Schema(description = "The offence code, This is the unique number relating to the type of offence they have been alleged to have committed", example = "3")
  val offenceCode: Int,
  @Schema(description = "The offence rules they have broken")
  val offenceRule: OffenceRuleDetailsDto,
  @Schema(description = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @Schema(description = "The username of the member of staff who is a victim of the incident, if relevant", example = "ABC12D")
  val victimStaffUsername: String? = null,
  @Schema(description = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant", example = "Bob Hope")
  val victimOtherPersonsName: String? = null,
)

@Schema(description = "Details of a rule they have broken")
data class OffenceRuleDetailsDto(
  @Schema(description = "The paragraph number relating to the offence rule they have been alleged to have broken", example = "25(a)")
  val paragraphNumber: String,
  @Schema(description = "The name relating to the paragraph description", example = "Committed an assault")
  val paragraphDescription: String,
)

@Schema(description = "Incident statement")
data class IncidentStatementDto(
  @Schema(description = "The statement regarding the incident")
  val statement: String,
  @Schema(description = "Indicates when the statement is complete")
  val completed: Boolean? = false,
)
