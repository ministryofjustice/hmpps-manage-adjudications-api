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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamagesRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.DamagesService

@PreAuthorize("hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "22. Damages")
class DamagesController(
  private val damagesService: DamagesService,
) : ReportedAdjudicationBaseController() {

  @PutMapping(value = ["/{adjudicationNumber}/damages/edit"])
  @Operation(summary = "Updates the damages for the reported adjudication.", description = "0 or more damages to be supplied, only updates records owned by current user")
  @ResponseStatus(HttpStatus.OK)
  fun updateDamages(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid
    damagesRequest: DamagesRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = damagesService.updateDamages(
      adjudicationNumber,
      damagesRequest.damages,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
