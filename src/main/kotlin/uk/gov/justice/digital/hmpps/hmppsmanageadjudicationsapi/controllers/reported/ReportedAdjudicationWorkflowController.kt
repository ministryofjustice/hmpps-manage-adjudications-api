package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DraftAdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService

@RestController
@Tag(name = "21. Adjudication Workflow")
class ReportedAdjudicationWorkflowController(
  private val adjudicationWorkflowService: AdjudicationWorkflowService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{chargeNumber}/create-draft-adjudication"])
  @Operation(
    summary = "Creates a draft adjudication from the reported adjudication with the given number.",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Draft Adjudication report created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createDraftAdjudication(@PathVariable(name = "chargeNumber") chargeNumber: String): DraftAdjudicationResponse {
    val draftAdjudication = adjudicationWorkflowService.createDraftFromReportedAdjudication(chargeNumber)
    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }
}
