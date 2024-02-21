package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PrintSupportService
import java.time.LocalDate

@Schema(description = "dis5 data model")
data class Dis5DataModel(
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
  @Schema(description = "list of current suspended punishments")
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

@RestController
@Tag(name = "02. Print Support for DIS forms")
@PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
class PrintSupportController(
  private val printSupportService: PrintSupportService,
) : ReportedAdjudicationBaseController() {

  @Operation(
    summary = "Gets the data required to populate the dis5 form",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "DIS5 data model for pdf",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "404",
        description = "Report not found",
      ),

    ],
  )
  @GetMapping(value = ["/{chargeNumber}/print-support/dis5"])
  fun getDis5Data(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): Dis5DataModel = printSupportService.getDis5Data(chargeNumber = chargeNumber)
}
