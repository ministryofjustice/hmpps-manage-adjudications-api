package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService

@RestController
@Validated
class DraftAdjudicationWorkflowController(
  private val adjudicationWorkflowService: AdjudicationWorkflowService,
) : DraftAdjudicationBaseController() {

  @PostMapping(value = ["/{id}/complete-draft-adjudication"])
  @Operation(summary = "Submits the draft adjudication to Prison-API, creates a submitted adjudication record and removes the draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun completeDraftAdjudication(@PathVariable(name = "id") id: Long): ReportedAdjudicationDto =
    adjudicationWorkflowService.completeDraftAdjudication(id)
}
