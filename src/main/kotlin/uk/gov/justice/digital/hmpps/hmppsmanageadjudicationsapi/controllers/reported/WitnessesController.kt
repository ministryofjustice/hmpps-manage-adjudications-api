package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessesRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.WitnessesService
import javax.validation.Valid

@PreAuthorize("hasAuthority('SCOPE_write')")
@RestController
class WitnessesController(
  private val witnessesService: WitnessesService,
) : ReportedAdjudicationBaseController() {

  @PutMapping(value = ["/{adjudicationNumber}/witnesses/edit"])
  @Operation(summary = "Updates the witnesses for the reported adjudication.", description = "0 or more witnesses to be supplied, only updates records owned by current user")
  @ResponseStatus(HttpStatus.OK)
  fun updateWitnesses(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid
    witnessesRequest: WitnessesRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = witnessesService.updateWitnesses(
      adjudicationNumber,
      witnessesRequest.witnesses,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
