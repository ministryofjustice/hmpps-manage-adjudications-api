package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
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
  @Schema(description = "Damages related to incident")
  val damages: List<DamageDto>? = null,
  @Schema(description = "Evidence related to incident")
  val evidence: List<EvidenceDto>? = null,
  @Schema(description = "Witnesses related to incident")
  val witnesses: List<WitnessDto>? = null,
  @Schema(description = "has the damages end point been called")
  val damagesSaved: Boolean? = null,
  @Schema(description = "has the evidence end point been called")
  val evidenceSaved: Boolean? = null,
  @Schema(description = "has the witnesses end point been called")
  val witnessesSaved: Boolean? = null,

)

@Schema(description = "Incident details")
data class IncidentDetailsDto(
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  val dateTimeOfIncident: LocalDateTime,
  @Schema(description = "Date time if discovery date different to incident date", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfDiscovery: LocalDateTime,
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
  @Schema(description = "The name of the other prisoner involved in the incident, This only applies when the prisoner is from outside the establishment", example = "G2996UX")
  val associatedPrisonersName: String?,
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

@Schema(description = "damages")
data class DamageDto(
  @Schema(description = "The damage code based on an enum for defined damages", example = "CLEANING")
  val code: DamageCode,
  @Schema(description = "The details of the damage", example = "the kettle was broken")
  val details: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "evidence")
data class EvidenceDto(
  @Schema(description = "The evidence code based on an enum for defined evidence", example = "PHOTO")
  val code: EvidenceCode,
  @Schema(description = "Evidence identifier", example = "Tag number or Camera number")
  val identifier: String? = null,
  @Schema(description = "The details of the evidence", example = "ie what does the photo show")
  val details: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "witness")
data class WitnessDto(
  @Schema(description = "The witness code based on an enum for defined witness", example = "PRISON_OFFICER")
  val code: WitnessCode,
  @Schema(description = "Witness first name", example = "Fred")
  val firstName: String,
  @Schema(description = "Witness last name", example = "Kruger")
  val lastName: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)
