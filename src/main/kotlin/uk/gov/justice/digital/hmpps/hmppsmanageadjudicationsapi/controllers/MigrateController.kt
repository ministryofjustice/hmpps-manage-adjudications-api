package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationBaseController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateService

@Schema(description = "adjudication migrate response")
data class MigrateResponse(
  @Schema(description = "charge number mapping")
  val chargeNumberMapping: ChargeNumberMapping,
  @Schema(description = "hearing mappings")
  val hearingMappings: List<HearingMapping>? = emptyList(),
  @Schema(description = "punishment mappings")
  val punishmentMappings: List<PunishmentMapping>? = emptyList(),
)

@Schema(description = "charge number mapping")
data class ChargeNumberMapping(
  @Schema(description = "charge number created")
  val chargeNumber: String,
  @Schema(description = "oic incident id")
  val oicIncidentId: Long,
  @Schema(description = "offence sequence")
  val offenceSequence: Long,
)

@Schema(description = "hearing mapping")
data class HearingMapping(
  @Schema(description = "hearing id")
  val hearingId: Long,
  @Schema(description = "oic hearing id")
  val oicHearingId: Long,
)

@Schema(description = "punishment mapping")
data class PunishmentMapping(
  @Schema(description = "punishment id")
  val punishmentId: Long,
  @Schema(description = "offender book id")
  val bookingId: Long,
  @Schema(description = "sanction sequence")
  val sanctionSeq: Long,
)

@PreAuthorize("hasRole('MIGRATE_ADJUDICATIONS') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "99. Migrate")
class MigrateController(
  private val migrateService: MigrateService,
) : ReportedAdjudicationBaseController() {

  @Operation(
    summary = "migrates a record into the adjudication service",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Adjudication successfully migrated",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "409",
        description = "Adjudication failed to update existing record",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "422",
        description = "unable to migrate record",
      ),
    ],
  )
  @PostMapping(value = ["/migrate"])
  @ResponseStatus(HttpStatus.CREATED)
  fun migrate(@RequestBody adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse = migrateService.accept(adjudicationMigrateDto)

  @Operation(summary = "resets the migration and removes all migrated records from database")
  @DeleteMapping(value = ["/migrate/reset"])
  @ResponseStatus(HttpStatus.OK)
  fun reset() = migrateService.reset()
}
