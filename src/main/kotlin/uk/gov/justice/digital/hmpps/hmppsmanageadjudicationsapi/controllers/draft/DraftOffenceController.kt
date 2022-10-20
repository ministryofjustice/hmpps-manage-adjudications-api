package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService
import javax.validation.Valid

@RestController
class DraftOffenceController(
  val incidentOffenceService: DraftOffenceService
) : DraftAdjudicationBaseController() {

  @PutMapping(value = ["/{id}/offence-details"])
  @Operation(summary = "Set the offence details for the draft adjudication.", description = "At least one set of offence details must be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setOffenceDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid offenceDetailsRequest: OffenceDetailsRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = incidentOffenceService.setOffenceDetails(
      id,
      offenceDetailsRequest.offenceDetails
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @GetMapping("/offence-rule/{offenceCode}")
  @Operation(summary = "Returns details of the offence rule relating to this offence code.")
  fun getOffenceRule(
    @PathVariable(name = "offenceCode") offenceCode: Int,
    @RequestParam(value = "youthOffender") isYouthOffender: Boolean,
  ): OffenceRuleDetailsDto = incidentOffenceService.lookupRuleDetails(offenceCode, isYouthOffender)
}
