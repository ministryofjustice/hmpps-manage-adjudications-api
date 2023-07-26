package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
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
  @Schema(description = "optional victims", required = false)
  val victims: List<MigrateVictim> = emptyList(),
  @Schema(description = "optional associates", required = false)
  val associates: List<MigrateAssociate> = emptyList(),
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
)

@Schema(description = "victim")
data class MigrateVictim(
  @Schema(description = "victims staff username or prisoner number")
  val victimIdentifier: String,
  @Schema(description = "flag to indicate if staff, false means prisoner")
  val isStaff: Boolean,
)

@Schema(description = "incident associate")
data class MigrateAssociate(
  @Schema(description = "associate prisoner number")
  val associatedPrisoner: String?,
)

@Schema(description = "reporting officer")
data class ReportingOfficer(
  @Schema(description = "reporting officer username")
  val username: String,
)

enum class NomisGender {
  M, F, NK, NS, REF
}
