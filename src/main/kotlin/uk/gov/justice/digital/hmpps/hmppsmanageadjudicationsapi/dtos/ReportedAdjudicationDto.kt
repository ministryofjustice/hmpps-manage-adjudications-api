package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotCompletedOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@Schema(description = "Reported adjudication details")
data class ReportedAdjudicationDto(
  @Schema(description = "The charge number for the reported adjudication")
  val chargeNumber: String,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "Name of a prisoner", example = "SAM GOMEZ")
  var prisonerName: String? = null,
  @Schema(description = "Gender applied for adjudication rules", example = "MALE")
  val gender: Gender,
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
  @Schema(description = "The description of the status in the reported adjudication")
  var statusDescription: String? = null,
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
  @Schema(description = "The last issuing officer")
  val issuingOfficer: String? = null,
  @Schema(description = "The last date time of form issued")
  val dateTimeOfIssue: LocalDateTime? = null,
  @Schema(description = "Previous DIS1/2 issues")
  val disIssueHistory: List<DisIssueHistoryDto>,
  @Schema(description = "date time of first hearing")
  val dateTimeOfFirstHearing: LocalDateTime? = null,
  @Schema(description = "Hearings, hearing outcomes, referrals and outcomes in chronological order")
  val outcomes: List<OutcomeHistoryDto>,
  @Schema(description = "punishments")
  val punishments: MutableList<PunishmentDto>,
  @Schema(description = "punishments")
  val punishmentComments: List<PunishmentCommentDto>,
  @Schema(description = "flag to indicate a hearing outcome was entered in NOMIS")
  val outcomeEnteredInNomis: Boolean = false,
  @Schema(description = "optional override agency id")
  val overrideAgencyId: String?,
  @Schema(description = "agency id where report was created")
  val originatingAgencyId: String,
  @Schema(description = "optional actions flag to indicate if an ALO can carry out actions against a transferable adjudication, null if not transferable")
  val transferableActionsAllowed: Boolean? = null,
  @Schema(description = "hearing id action carried out on")
  @JsonIgnore
  var hearingIdActioned: Long? = null,
  @Schema(description = "Name the officer the report was created on behalf of")
  var createdOnBehalfOfOfficer: String? = null,
  @Schema(description = "Reason why the report was created on behalf of another officer")
  var createdOnBehalfOfReason: String? = null,
  @Schema(description = "punishments have been removed due to outcome delete or edit")
  @JsonIgnore
  var punishmentsRemoved: Boolean = false,
  @Schema(description = "list of linked nomis charges, where multiple offences recorded on a single charge")
  val linkedChargeNumbers: List<String>,
  @Schema(description = "flag to indicate if the user can action this item via the history page")
  val canActionFromHistory: Boolean = false,
  @Schema(description = "array of additional events to send for suspended punishments")
  @JsonIgnore
  var suspendedPunishmentEvents: Set<SuspendedPunishmentEvent>? = null,
)

@Schema(description = "suspended punishment event")
data class SuspendedPunishmentEvent(
  val agencyId: String,
  val chargeNumber: String,
  val status: ReportedAdjudicationStatus,
  val prisonerNumber: String? = null,
)

@Schema(description = "Details of an offence")
data class OffenceDto(
  @Schema(description = "The offence code", example = "3")
  val offenceCode: Int,
  @Schema(description = "The offence code description", example = "Disobeys any lawful order")
  var offenceCodeDescription: String? = null,
  @Schema(description = "The offence rules they have broken")
  val offenceRule: OffenceRuleDto,
  @Schema(description = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @Schema(
    description = "The username of the member of staff who is a victim of the incident, if relevant",
    example = "ABC12D",
  )
  val victimStaffUsername: String? = null,
  @Schema(
    description = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant",
    example = "Bob Hope",
  )
  val victimOtherPersonsName: String? = null,
  @Schema(description = "list of protected characteristics for offence, empty if non involved in offence")
  val protectedCharacteristics: List<Characteristic>,
  @Schema(description = "list of protected characteristics for offence descriptions, empty if non involved in offence")
  var protectedCharacteristicsDescriptions: List<String>? = null,
)

@Schema(description = "Details of a rule they have broken")
data class OffenceRuleDto(
  @Schema(
    description = "The paragraph number relating to the offence rule they have been alleged to have broken",
    example = "25(a)",
  )
  val paragraphNumber: String,
  @Schema(description = "The name relating to the paragraph description", example = "Committed an assault")
  val paragraphDescription: String,
  @Schema(description = "nomis code - not set if migrated data")
  val nomisCode: String? = null,
  @Schema(description = "with others nomis code, not set if migrated data")
  val withOthersNomisCode: String? = null,
)

@Schema(description = "Reported damages")
data class ReportedDamageDto(
  @Schema(description = "The damage code based on an enum for defined damages", example = "CLEANING")
  val code: DamageCode,
  @Schema(description = "The damage code description based on an enum for defined damages", example = "Cleaning")
  var codeDescription: String? = null,
  @Schema(description = "The details of the damage", example = "the kettle was broken")
  val details: String,
  @Schema(description = "The username of the person who added this record", example = "ABC12D")
  val reporter: String,
)

@Schema(description = "Reported evidence")
data class ReportedEvidenceDto(
  @Schema(description = "The evidence code based on an enum for defined evidence", example = "PHOTO")
  val code: EvidenceCode,
  @Schema(description = "The evidence code description based on an enum for defined evidence", example = "Photo")
  var codeDescription: String? = null,
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
  @Schema(
    description = "The witness code description based on an enum for defined witness",
    example = "A prison officer",
  )
  var codeDescription: String? = null,
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
  @Schema(description = "The id of the location of the hearing", deprecated = true)
  val locationId: Long? = null,
  @Schema(description = "The name of the location of the hearing", deprecated = true)
  var locationName: String? = null,
  @Schema(description = "The location uuid of the location the hearing")
  val locationUuid: UUID,
  @Schema(description = "Date and time the hearing will take place", example = "2010-10-12T10:00:00")
  val dateTimeOfHearing: LocalDateTime,
  @Schema(description = "oic hearing type")
  val oicHearingType: OicHearingType,
  @Schema(description = "oic hearing type description")
  var oicHearingTypeDescription: String? = null,
  @Schema(description = "hearing outcome")
  val outcome: HearingOutcomeDto? = null,
  @Schema(description = "agency id of hearing")
  val agencyId: String,
)

@Schema(description = "hearing outcome")
data class HearingOutcomeDto(
  @Schema(description = "The id of the hearing outcome")
  val id: Long? = null,
  @Schema(description = "adjudicator of hearing")
  val adjudicator: String,
  @Schema(description = "the hearing outcome code")
  val code: HearingOutcomeCode,
  @Schema(description = "the hearing outcome code description")
  var codeDescription: String? = null,
  @Schema(description = "reason for outcome")
  val reason: HearingOutcomeAdjournReason? = null,
  @Schema(description = "details of outcome")
  val details: String? = null,
  @Schema(description = "hearing outcome plea")
  val plea: HearingOutcomePlea? = null,
  @Schema(description = "hearing outcome plea description")
  var pleaDescription: String? = null,
)

@Schema(description = "Hearing Summary")
data class HearingSummaryDto(
  @Schema(description = "The id of the hearing")
  val id: Long? = null,
  @Schema(description = "Date and time the hearing will take place", example = "2010-10-12T10:00:00")
  val dateTimeOfHearing: LocalDateTime,
  @Schema(description = "Date and time the incident was discovered", example = "2010-10-12T10:00:00")
  val dateTimeOfDiscovery: LocalDateTime,
  @Schema(description = "The charge number for the reported adjudication")
  val chargeNumber: String,
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "type of hearing")
  val oicHearingType: OicHearingType,
  @Schema(description = "reported adjudication status")
  val status: ReportedAdjudicationStatus,
  @Schema(description = "internal location id", deprecated = true)
  val locationId: Long? = null,
  @Schema(description = "Location UUID of the hearing")
  val locationUuid: UUID,
)

data class OutcomeHistoryDto(
  @Schema(description = "hearing including hearing outcome")
  val hearing: HearingDto? = null,
  @Schema(description = "combined outcome")
  val outcome: CombinedOutcomeDto? = null,
)

data class OutcomeDto(
  @Schema(description = "The id of the outcome")
  val id: Long? = null,
  @Schema(description = "outcome code")
  val code: OutcomeCode,
  @Schema(description = "optional details")
  val details: String? = null,
  @Schema(description = "optional not proceeded with reason")
  val reason: NotProceedReason? = null,
  @Schema(description = "optional quashed reason")
  val quashedReason: QuashedReason? = null,
  @Schema(description = "optional refer to gov reason")
  val referGovReason: ReferGovReason? = null,
  @Schema(description = "flag to indicate if the outcome can be removed")
  val canRemove: Boolean = true,
)

@Schema(description = "Combined Outcome - currently to support referral but maybe expanded once awards are added")
data class CombinedOutcomeDto(
  @Schema(description = "the outcome")
  val outcome: OutcomeDto,
  @Schema(description = "the optional referral outcome")
  val referralOutcome: OutcomeDto? = null,
)

@Schema(description = "Previous DIS1/2 issues")
data class DisIssueHistoryDto(
  @Schema(description = "Previous issuing officer")
  val issuingOfficer: String,
  @Schema(description = "Previous date time of form issued")
  val dateTimeOfIssue: LocalDateTime,
)

@Schema(description = "punishment")
data class PunishmentDto(
  @Schema(description = "punishment id used for edit and delete")
  val id: Long? = null,
  @Schema(description = "punishment type")
  val type: PunishmentType,
  @Schema(description = "punishment type description")
  var typeDescription: String? = null,
  @Schema(description = "optional privilege type")
  val privilegeType: PrivilegeType? = null,
  @Schema(description = "optional privilege type description")
  var privilegeTypeDescription: String? = null,
  @Schema(description = "optional other privilege type")
  val otherPrivilege: String? = null,
  @Schema(description = "optional stoppage of earnings percentage")
  val stoppagePercentage: Int? = null,
  @Schema(description = "optional activated by report number")
  val activatedBy: String? = null,
  @Schema(description = "optional activated from report number")
  val activatedFrom: String? = null,
  @Schema(description = "latest punishment schedule")
  val schedule: PunishmentScheduleDto,
  @Schema(description = "optional consecutive to charge number")
  val consecutiveChargeNumber: String? = null,
  @Schema(description = "optional consecutive report number is available to view in adjudications service")
  val consecutiveReportAvailable: Boolean? = null,
  @Schema(description = "optional amount - money being recovered for damages")
  val damagesOwedAmount: Double? = null,
  @Schema(description = "flag to indicate if the punishment can be removed")
  val canRemove: Boolean = true,
  @Schema(description = "flag to indicate if the punishment can be edited")
  val canEdit: Boolean = true,
  @Schema(description = "payback notes")
  val paybackNotes: String? = null,
  @Schema(description = "rehabilitative activities associated to suspended punishment")
  val rehabilitativeActivities: List<RehabilitativeActivityDto> = emptyList(),
  @Schema(description = "rehabilitative activity completed")
  val rehabilitativeActivitiesCompleted: Boolean? = null,
  @Schema(description = "rehabilitative activity not completed outcome")
  val rehabilitativeActivitiesNotCompletedOutcome: NotCompletedOutcome? = null,
  @Schema(description = "previous suspended until date if subsequently extended")
  val previousSuspendedUntilDate: LocalDate? = null,
)

@Schema(description = "rehabilitative activity dto")
data class RehabilitativeActivityDto(
  @Schema(description = "id")
  val id: Long? = null,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "who is monitoring it")
  val monitor: String? = null,
  @Schema(description = "end date")
  val endDate: LocalDate? = null,
  @Schema(description = "optional number of sessions")
  val totalSessions: Int? = null,
  @Schema(description = "completed")
  val completed: Boolean? = null,
)

@Schema(description = "punishment schedule")
data class PunishmentScheduleDto(
  @Schema(description = "days punishment will last - use duration for new integrations")
  @Deprecated("this is for live services such as sync, please use duration from now on")
  val days: Int,
  @Schema(description = "duration of punishment")
  val duration: Int? = null,
  @Schema(description = "measurement of duration")
  val measurement: Measurement,
  @Schema(description = "optional start date of punishment")
  val startDate: LocalDate? = null,
  @Schema(description = "optional end date of punishment")
  val endDate: LocalDate? = null,
  @Schema(description = "optional punishment suspended until date")
  val suspendedUntil: LocalDate? = null,
)

@Schema(description = "suspended punishment dto")
data class SuspendedPunishmentDto(
  @Schema(description = "charge number punishment from")
  val chargeNumber: String,
  @Schema(description = "indicates there is something wrong with this suspended punishment, and its within the last 6 months")
  val corrupted: Boolean,
  @Schema(description = "punishment dto")
  val punishment: PunishmentDto,
)

@Schema(description = "additional days to activate dto")
data class AdditionalDaysDto(
  @Schema(description = "charge number punishment from")
  val chargeNumber: String,
  @Schema(description = "date charge proved")
  val chargeProvedDate: LocalDate,
  @Schema(description = "punishment dto")
  val punishment: PunishmentDto,
)

@Schema(description = "punishment comment")
data class PunishmentCommentDto(
  @Schema(description = "punishment comment id used for edit and delete")
  val id: Long? = null,
  @Schema(description = "comment on punishment")
  val comment: String,
  @Schema(description = "punishment reason for change")
  val reasonForChange: ReasonForChange? = null,
  @Schema(description = "description of the punishment reason for change")
  var reasonForChangeDescription: String? = null,
  @Schema(description = "username of the person created or updated the comment")
  val createdByUserId: String? = null,
  @Schema(description = "date and time comment was created or updated")
  val dateTime: LocalDateTime? = null,
)
