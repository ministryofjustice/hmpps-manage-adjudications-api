package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationWorkflowService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService
@RestController
@Validated
@Tag(name = "11. Draft Adjudication Workflow")
class DraftAdjudicationWorkflowController(
  private val adjudicationWorkflowService: AdjudicationWorkflowService,
  private val eventPublishService: EventPublishService,
) : DraftAdjudicationBaseController() {

  @PostMapping(value = ["/{id}/complete-draft-adjudication"])
  @Operation(
    summary = "Submits the draft adjudication to Prison-API, creates a submitted adjudication record and removes the draft adjudication.",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Draft Adjudication Completed",
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
  fun completeDraftAdjudication(@PathVariable(name = "id") id: Long): ReportedAdjudicationDto {
    val adjudication = adjudicationWorkflowService.completeDraftAdjudication(id)
    eventPublishService.publishAdjudicationCreation(adjudication)
    return adjudication
  }

  @PostMapping(value = ["/{id}/alo-offence-details"])
  @Operation(
    summary = "Submits updated offence details and completes report.  Used by ALO when they need to amend offence details for submitted reports only",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Reported adjudication",
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
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun aloOffenceUpdate(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    offenceDetailsRequest: OffenceDetailsRequest,
  ): ReportedAdjudicationDto {
    val adjudication =
      adjudicationWorkflowService.setOffenceDetailsAndCompleteDraft(
        id = id,
        offenceDetails = offenceDetailsRequest.offenceDetails,
      )
    eventPublishService.publishAdjudicationUpdate(adjudication)
    return adjudication
  }
}
