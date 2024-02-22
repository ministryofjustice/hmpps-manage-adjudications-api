package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Dis5PrintSupportDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PrintSupportService

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
  ): Dis5PrintSupportDto = printSupportService.getDis5Data(chargeNumber = chargeNumber)
}
