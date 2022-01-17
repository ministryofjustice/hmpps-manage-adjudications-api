package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentRoleDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.DraftAdjudicationService
import java.time.LocalDateTime
import javax.validation.Valid
import javax.validation.constraints.Size

@ApiModel("Request to create a new draft adjudication")
data class NewAdjudicationRequest(
  @ApiModelProperty(value = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @ApiModelProperty(value = "The agency id (or caseload) associated with this adjudication", example = "MDI")
  val agencyId: String,
  @ApiModelProperty(value = "The id of the location the incident took place")
  val locationId: Long,
  @ApiModelProperty(value = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime,
  @ApiModelProperty(value = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto? = null,
)

@ApiModel("Request to add or edit the incident statement for a draft adjudication")
data class IncidentStatementRequest(
  @ApiModelProperty(value = "The statement regarding the incident")
  @get:Size(
    max = 4000,
    message = "The incident statement exceeds the maximum character limit of {max}"
  )
  val statement: String? = null,
  val completed: Boolean? = false
)

@ApiModel("Request to edit the incident details")
data class EditIncidentDetailsRequest(
  @ApiModelProperty(value = "The id of the location the incident took place")
  val locationId: Long? = null,
  @ApiModelProperty(value = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime? = null,
  @ApiModelProperty(value = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleDto? = null,
)

@ApiModel("Draft adjudication response")
data class DraftAdjudicationResponse(
  @ApiModelProperty(value = "The draft adjudication")
  val draftAdjudication: DraftAdjudicationDto
)

@ApiModel("In progress draft adjudication response")
data class InProgressAdjudicationResponse(
  @ApiModelProperty(value = "All in progress adjudications")
  val draftAdjudications: List<DraftAdjudicationDto>
)

@RestController
@RequestMapping("/draft-adjudications")
@Validated
class DraftAdjudicationController {
  @Autowired
  lateinit var draftAdjudicationService: DraftAdjudicationService

  @GetMapping("/my/agency/{agencyId}")
  @ApiOperation(value = "Returns all the in progress draft adjudications created by the current user. Default sort is by earliest incident date and time.")
  fun getCurrentUsersInProgressDraftAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
  ): InProgressAdjudicationResponse = InProgressAdjudicationResponse(
    draftAdjudications = draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(agencyId)
  )

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ApiOperation(value = "Starts a new draft adjudication.")
  @ResponseStatus(HttpStatus.CREATED)
  fun startNewAdjudication(@RequestBody newAdjudicationRequest: NewAdjudicationRequest): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService
      .startNewAdjudication(
        newAdjudicationRequest.prisonerNumber,
        newAdjudicationRequest.agencyId,
        newAdjudicationRequest.locationId,
        newAdjudicationRequest.dateTimeOfIncident,
        newAdjudicationRequest.incidentRole,
      )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @GetMapping(value = ["/{id}"])
  @ApiOperation(value = "Returns the draft adjudication details.")
  fun getDraftAdjudicationDetails(@PathVariable(name = "id") id: Long): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.getDraftAdjudicationDetails(id)

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PostMapping(value = ["/{id}/incident-statement"])
  @ApiOperation(value = "Add the incident statement to the draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun addIncidentStatement(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid incidentStatementRequest: IncidentStatementRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.addIncidentStatement(
      id,
      incidentStatementRequest.statement,
      incidentStatementRequest.completed
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/incident-details"])
  @ApiOperation(value = "Edit the incident details for a draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun editIncidentDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid editIncidentDetailsRequest: EditIncidentDetailsRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentDetails(
      id,
      editIncidentDetailsRequest.locationId,
      editIncidentDetailsRequest.dateTimeOfIncident,
      editIncidentDetailsRequest.incidentRole,
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/incident-statement"])
  @ApiOperation(value = "Edit the incident statement for a draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun editIncidentStatement(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid editIncidentStatementRequest: IncidentStatementRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentStatement(
      id,
      statement = editIncidentStatementRequest.statement,
      completed = editIncidentStatementRequest.completed
    )
    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PostMapping(value = ["/{id}/complete-draft-adjudication"])
  @ApiOperation(value = "Submits the draft adjudication to Prison-API, creates a submitted adjudication record and removes the draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun completeDraftAdjudication(@PathVariable(name = "id") id: Long): ReportedAdjudicationDto =
    draftAdjudicationService.completeDraftAdjudication(id)
}
