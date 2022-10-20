package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService

@RestController
class ReportedAdjudicationWorkflowController(
  val adjudicationWorkflowService: AdjudicationWorkflowService
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{adjudicationNumber}/create-draft-adjudication"])
  @Operation(summary = "Creates a draft adjudication from the reported adjudication with the given number.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createDraftAdjudication(@PathVariable(name = "adjudicationNumber") adjudicationNumber: Long): DraftAdjudicationResponse {
    val draftAdjudication = adjudicationWorkflowService.createDraftFromReportedAdjudication(adjudicationNumber)
    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }
}
