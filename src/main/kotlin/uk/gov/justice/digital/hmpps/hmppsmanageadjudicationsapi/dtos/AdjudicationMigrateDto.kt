package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Adjudication to migrate into service")
data class AdjudicationMigrateDto(
  @Schema(description = "The agency incident id")
  val agencyIncidentId: Long,
  @Schema(description = "The oic incident id")
  val oicIncidentId: Long,
  @Schema(description = "the offence sequence")
  val offenceSequence: Long,
  @Schema(description = "booking id on adjudication")
  val bookingId: Long,
  @Schema(description = "agency id on adjudication")
  val agencyId: String,
  @Schema(description = "the date and time of the incident")
  val incidentDateTime: LocalDateTime,
  @Schema(description = "the reported dated time")
  val reportedDateTime: LocalDateTime,
  @Schema(description = "the internal location id of the incident")
  val locationId: Long,
  @Schema(description = "the incident statement")
  val statement: String,
  @Schema(description = "reporting officer")
  val reportingOfficer: ReportingOfficer,
  @Schema(description = "created by username from nomis audit - to be mapped to reviewedBy")
  val createdByUsername: String,
  @Schema(description = "prisoner details")
  val prisoner: MigratePrisoner,
  @Schema(description = "offence details")
  val offence: MigrateOffence,
  @Schema(description = "witnesses", required = false)
  val witnesses: List<MigrateWitness> = emptyList(),
  @Schema(description = "damages / repairs", required = false)
  val damages: List<MigrateDamage> = emptyList(),
  @Schema(description = "evidence", required = false)
  val evidence: List<MigrateEvidence> = emptyList(),
  @Schema(description = "punishments / sanctions", required = false)
  val punishments: List<MigratePunishment> = emptyList(),
  @Schema(description = "hearings", required = false)
  val hearings: List<MigrateHearing> = emptyList(),
  @Schema(description = "dis issued")
  val disIssued: List<DisIssued> = emptyList(),
)

@Schema(description = "dis issued")
data class DisIssued(
  var issuingOfficer: String,
  var dateTimeOfIssue: LocalDateTime,
)

@Schema(description = "prisoner details")
data class MigratePrisoner(
  @Schema(description = "prisoner number on adjudication")
  val prisonerNumber: String,
  @Schema(description = "agency the prisoner is currently residing, or null if no longer in prison")
  val currentAgencyId: String?,
  @Schema(description = "gender of the prisoner")
  val gender: String,
)

@Schema(description = "offence details")
data class MigrateOffence(
  @Schema(description = "the nomis offence code")
  val offenceCode: String,
  @Schema(description = "the nomis offence description, ie rule, paragraph as displayed in nomis")
  val offenceDescription: String,
)

@Schema(description = "reporting officer")
data class ReportingOfficer(
  @Schema(description = "reporting officer username")
  val username: String,
)

@Schema(description = "witnesses -these can include victims and associates")
data class MigrateWitness(
  @Schema(description = "first name")
  val firstName: String,
  @Schema(description = "last name")
  val lastName: String,
  @Schema(description = "created by username")
  val createdBy: String,
  @Schema(description = "type of witness")
  val witnessType: WitnessCode,
  @Schema(description = "date added")
  val dateAdded: LocalDate,
  @Schema(description = "comment")
  val comment: String?,
  @Schema(description = "username")
  val username: String?,
)

@Schema(description = "damages recorded on adjudication - repair in nomis")
data class MigrateDamage(
  @Schema(description = "damage / repair type")
  val damageType: DamageCode,
  @Schema(description = "detail of damage - can be null as not enforced on nomis")
  val details: String?,
  @Schema(description = "created by username")
  val createdBy: String,
  @Schema(description = "repair cost")
  val repairCost: BigDecimal?,
)

@Schema(description = "evidence related to adjudication")
data class MigrateEvidence(
  @Schema(description = "evidence code")
  val evidenceCode: EvidenceCode,
  @Schema(description = "evidence details / statement")
  val details: String,
  @Schema(description = "this is the reporter, we could use the investigator in this field, or createdBy user")
  val reporter: String,
  @Schema(description = "date added")
  val dateAdded: LocalDate,
)

@Schema(description = "punishment / sanction")
data class MigratePunishment(
  @Schema(description = "sanction code")
  val sanctionCode: String,
  @Schema(description = "sanction status")
  val sanctionStatus: String,
  @Schema(description = "sanction sequence")
  val sanctionSeq: Long,
  @Schema(description = "effective date")
  val effectiveDate: LocalDate,
  @Schema(description = "comment text")
  val comment: String?,
  @Schema(description = "compensation amount")
  val compensationAmount: BigDecimal?,
  @Schema(description = "consecutive charge number - for nomis this needs to be the oic_incident_id for the consecutiveOffenderBookId/consecutiveSanctionSeq. Note,  this will also need to include the chargeSequence, ie oic_incident_id-charge_seq")
  val consecutiveChargeNumber: String?,
  @Schema(description = "days applied - note we do not support months, should be converted to days if present TBC")
  val days: Int?,
  @Schema(description = "created by username")
  val createdBy: String,
  @Schema(description = "created date time")
  val createdDateTime: LocalDateTime,
  @Schema(description = "status date")
  val statusDate: LocalDate? = null,
)

@Schema(description = "hearing and optional result")
data class MigrateHearing(
  @Schema(description = "oic hearing id")
  val oicHearingId: Long,
  @Schema(description = "oic hearing type")
  val oicHearingType: OicHearingType,
  @Schema(description = "date time of hearing")
  val hearingDateTime: LocalDateTime,
  @Schema(description = "hearing location id")
  val locationId: Long,
  @Schema(description = "adjudicator username - nomis has this on the hearing, so will accept at this level but for our service it goes on outcome")
  val adjudicator: String?,
  @Schema(description = "comment text")
  val commentText: String?,
  @Schema(description = "hearing result", required = false)
  val hearingResult: MigrateHearingResult? = null,
  @Schema(description = "representative")
  val representative: String?,
)

@Schema(description = "hearing result")
data class MigrateHearingResult(
  @Schema(description = "plea")
  val plea: String,
  @Schema(description = "finding")
  val finding: String,
  @Schema(description = "date time created")
  val createdDateTime: LocalDateTime,
  @Schema(description = "created by")
  val createdBy: String,
)

enum class NomisGender {
  M, F, NK, NS, REF
}
