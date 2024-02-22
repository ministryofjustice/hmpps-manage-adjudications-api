package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "dis5 data model")
data class Dis5PrintSupportDto(
  @Schema(description = "charge number")
  val chargeNumber: String,
  @Schema(description = "Date the incident occurred")
  val dateOfIncident: LocalDate,
  @Schema(description = "Date of discovery if date different to incident date")
  val dateOfDiscovery: LocalDate,
  @Schema(description = "total number of charge proved on current sentence")
  val previousCount: Int,
  @Schema(description = "total number of charge proved on current sentence at current establishment")
  val previousAtCurrentEstablishmentCount: Int,
  @Schema(description = "total number of charge proved for same offence")
  val sameOffenceCount: Int,
  @Schema(description = "optional last reported same offence")
  val lastReportedOffence: LastReportedOffence? = null,
  @Schema(description = "charges with suspended punishments that are active")
  val chargesWithSuspendedPunishments: List<ChargeWithSuspendedPunishments>,
  @Schema(description = "existing punishments")
  val existingPunishments: List<PunishmentDto>,
)

@Schema(description = "suspended punishments on charge")
data class ChargeWithSuspendedPunishments(
  @Schema(description = "Date the incident occurred")
  val dateOfIncident: LocalDate,
  @Schema(description = "Date of discovery if date different to incident date")
  val dateOfDiscovery: LocalDate,
  @Schema(description = "charge number")
  val chargeNumber: String,
  @Schema(description = "offence details of charge")
  val offenceDetails: OffenceDto,
  @Schema(description = "list of suspended punishments")
  val suspendedPunishments: List<PunishmentDto>,
)

@Schema(description = "optional last reported same offence charge")
data class LastReportedOffence(
  @Schema(description = "Date the incident occurred")
  val dateOfIncident: LocalDate,
  @Schema(description = "Date of discovery if date different to incident date")
  val dateOfDiscovery: LocalDate,
  @Schema(description = "last reported same offence charge number")
  val chargeNumber: String,
  @Schema(description = "The statement regarding the last reported incident")
  val statement: String,
  @Schema(description = "punishments awarded on the last reported same offence")
  val punishments: List<PunishmentDto>,
)
