package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportedAdjudicationBaseController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto

@Schema(description = "adjudication migrate response")
data class MigrateResponse(
  @Schema(description = "charge number created")
  val chargeNumber: String,
)

@PreAuthorize("hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "99. Migrate")
class MigrateController() : ReportedAdjudicationBaseController() {

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
  fun migrate(@RequestBody adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse = MigrateResponse(chargeNumber = "")
}
