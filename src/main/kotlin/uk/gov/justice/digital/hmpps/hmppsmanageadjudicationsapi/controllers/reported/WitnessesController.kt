package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessesRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.WitnessesService

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "25. Witnesses")
class WitnessesController(
  private val witnessesService: WitnessesService,
) : ReportedAdjudicationBaseController() {

  @PutMapping(value = ["/{chargeNumber}/witnesses/edit"])
  @Operation(summary = "Updates the witnesses for the reported adjudication.", description = "0 or more witnesses to be supplied, only updates records owned by current user")
  @ResponseStatus(HttpStatus.OK)
  fun updateWitnesses(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody @Valid
    witnessesRequest: WitnessesRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = witnessesService.updateWitnesses(
      chargeNumber,
      witnessesRequest.witnesses,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
