package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

@Schema(description = "Reported adjudication details")
data class ReportedAdjudicationDto(
  @Schema(description = "The number for the reported adjudication")
  val adjudicationNumber: Long,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "Gender applied for adjuducation rules", example = "MALE")
  val gender: Gender,
  @Schema(description = "The current booking id for a prisoner", example = "1234")
  val bookingId: Long,
  @Schema(description = "Incident details")
  val incidentDetails: IncidentDetailsDto,
  @Schema(description = "Is classified as a youth offender")
  val isYouthOffender: Boolean,
  @Schema(description = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto,
  @Schema(description = "Details about the offence the prisoner is accused of")
  val offenceDetails: OffenceDto,
  @Schema(description = "Incident statement")
  val incidentStatement: IncidentStatementDto,
  @Schema(description = "Created by user id")
  val createdByUserId: String,
  @Schema(description = "When the report was created")
  val createdDateTime: LocalDateTime,
  @Schema(description = "The status of the reported adjudication")
  val status: ReportedAdjudicationStatus,
  @Schema(description = "Reviewed by user id")
  val reviewedByUserId: String?,
  @Schema(description = "The reason for the status of the reported adjudication")
  val statusReason: String?,
  @Schema(description = "The name for the status of the reported adjudication")
  val statusDetails: String?,
  @Schema(description = "Damages related to incident")
  val damages: List<ReportedDamageDto>,
  @Schema(description = "Evidence related to incident")
  val evidence: List<ReportedEvidenceDto>,
  @Schema(description = "Witnesses related to incident")
  val witnesses: List<ReportedWitnessDto>,
  @Schema(description = "Hearings related to adjudication")
  val hearings: List<HearingDto>,
  @Schema(description = "issuing officer")
  val issuingOfficer: String? = null,
  @Schema(description = "date time of form issued")
  val dateTimeOfIssue: LocalDateTime? = null,
  @Schema(description = "date time of first hearing")
  val dateTimeOfFirstHearing: LocalDateTime? = null,
  @Schema(description = "Hearings, hearing outcomes, referrals and outcomes in chronological order")
  val history: List<OutcomeHistoryDto>,
  @Schema(description = "outcomes")
  val outcomes: List<CombinedOutcomeDto>,
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

@Schema(description = "Reported damages")
data class ReportedDamageDto(
  @Schema(description = "The damage code based on an enum for defined damages", example = "CLEANING")
  val code: DamageCode,
  @Schema(description = "The details of the damage", example = "the kettle was broken")
  val details: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "Reported evidence")
data class ReportedEvidenceDto(
  @Schema(description = "The evidence code based on an enum for defined evidence", example = "PHOTO")
  val code: EvidenceCode,
  @Schema(description = "Evidence identifier", example = "Tag number or Camera number")
  val identifier: String? = null,
  @Schema(description = "The details of the evidence", example = "ie what does the photo describe")
  val details: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "Reported witness")
data class ReportedWitnessDto(
  @Schema(description = "The witness code based on an enum for defined witness", example = "PRISON_OFFICER")
  val code: WitnessCode,
  @Schema(description = "Witness first name", example = "Fred")
  val firstName: String,
  @Schema(description = "Witness last name", example = "Kruger")
  val lastName: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "Hearing")
data class HearingDto(
  @Schema(description = "The id of the hearing")
  val id: Long? = null,
  @Schema(description = "The id of the location of the hearing")
  val locationId: Long,
  @Schema(description = "Date and time the hearing will take place", example = "2010-10-12T10:00:00")
  val dateTimeOfHearing: LocalDateTime,
  @Schema(description = "oic hearing type")
  val oicHearingType: OicHearingType,
  @Schema(description = "hearing outcome")
  val outcome: HearingOutcomeDto? = null,
)

@Schema(description = "hearing outcome")
data class HearingOutcomeDto(
  @Schema(description = "The id of the hearing outcome")
  val id: Long? = null,
  @Schema(description = "adjudicator of hearing")
  val adjudicator: String,
  @Schema(description = "the hearing outcome code")
  val code: HearingOutcomeCode,
  @Schema(description = "reason for outcome")
  val reason: HearingOutcomeAdjournReason? = null,
  @Schema(description = "details of outcome")
  val details: String? = null,
  @Schema(description = "hearing outcome finding")
  val finding: HearingOutcomeFinding? = null,
  @Schema(description = "hearing outcome plea")
  val plea: HearingOutcomePlea? = null,
)

@Schema(description = "Hearing Summary")
data class HearingSummaryDto(
  @Schema(description = "The id of the hearing")
  val id: Long? = null,
  @Schema(description = "Date and time the hearing will take place", example = "2010-10-12T10:00:00")
  val dateTimeOfHearing: LocalDateTime,
  @Schema(description = "Date and time the incident was discovered", example = "2010-10-12T10:00:00")
  val dateTimeOfDiscovery: LocalDateTime,
  @Schema(description = "The number for the reported adjudication")
  val adjudicationNumber: Long,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "type of hearing")
  val oicHearingType: OicHearingType,
)

@Schema(description = "item for hearings and referrals table")
data class OutcomeHistoryDto(
  @Schema(description = "hearing including hearing outcome")
  val hearing: HearingDto? = null,
  @Schema(description = "combined outcome")
  val outcome: CombinedOutcomeDto? = null,
)

@Schema(description = "Outcome")
data class OutcomeDto(
  @Schema(description = "The id of the outcome")
  val id: Long? = null,
  @Schema(description = "outcome code")
  val code: OutcomeCode,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "not proceeded with reason")
  val reason: NotProceedReason? = null,
)

@Schema(description = "Combined Outcome - currently to support referral but maybe expanded once awards are added")
data class CombinedOutcomeDto(
  @Schema(description = "the outcome")
  val outcome: OutcomeDto,
  @Schema(description = "the optional referral outcome")
  val referralOutcome: OutcomeDto? = null,
)
