package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Request to create a new draft adjudication")
data class NewAdjudicationRequest(
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "Gender applied for adjuducation rules", example = "MALE")
  val gender: Gender = Gender.MALE, // default when nothing set
  @Schema(description = "The agency id (or caseload) associated with this adjudication", example = "MDI")
  val agencyId: String,
  @Schema(description = "The optional agencyId where the prisoner now resides", example = "MDI")
  val overrideAgencyId: String? = null,
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime,
  @Schema(description = "Optional Date time if discovery date different to incident date", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfDiscovery: LocalDateTime? = null,
)

@Schema(description = "Request to update the incident role")
data class IncidentRoleRequest(
  @Schema(description = "The incident role code", title = "If not set then it is assumed they committed the offence on their own", example = "25a")
  val roleCode: String?,
)

@Schema(description = "Request to set the associated prisoner")
data class IncidentRoleAssociatedPrisonerRequest(
  @Schema(required = true, description = "The prison number of the other prisoner involved in the incident", example = "G2996UX")
  val associatedPrisonersNumber: String,
  @Schema(description = "The name of the other prisoner involved in the incident", title = "This only applies if the associated prisoner is from outside the establishment")
  val associatedPrisonersName: String?,
)

@Schema(description = "Request to add or edit the incident statement for a draft adjudication")
data class IncidentStatementRequest(
  @Schema(description = "The statement regarding the incident")
  @get:Size(
    max = 4000,
    message = "The incident statement exceeds the maximum character limit of {max}",
  )
  val statement: String? = null,
  val completed: Boolean? = false,
)

@Schema(description = "Request to edit the incident details")
data class EditIncidentDetailsRequest(
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime,
  @Schema(description = "Optional Date time if discovery date different to incident date", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfDiscovery: LocalDateTime? = null,
)

@Schema(description = "Request to edit incident role")
data class EditIncidentRoleRequest(
  @Schema(description = "Information about the role of this prisoner in the incident")
  val incidentRole: IncidentRoleRequest,
  @Schema(description = "Whether to remove all existing offences")
  val removeExistingOffences: Boolean = false,
)

@Schema(description = "Request to set applicable rules")
data class ApplicableRulesRequest(
  @Schema(description = "Indicates whether the applicable rules are for a young offender")
  val isYouthOffenderRule: Boolean,
  @Schema(description = "Whether to remove all existing offences")
  val removeExistingOffences: Boolean = false,
)

@Schema(description = "Request to update the gender")
data class GenderRequest(
  @Schema(description = "The gender", title = "Gender of prisoner", example = "MALE")
  val gender: Gender,
)

@RestController
@Validated
@Tag(name = "10. Draft Adjudication Management")
class DraftAdjudicationController(
  private val draftAdjudicationService: DraftAdjudicationService,
) : DraftAdjudicationBaseController() {

  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
      example = "0",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20",
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery,DESC",
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today",
    ),
  )
  @GetMapping("/my-reports")
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @Operation(summary = "Returns all the in progress draft adjudications created by the current user")
  fun getCurrentUsersInProgressDraftAdjudications(
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate?,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate?,
    @PageableDefault(sort = ["IncidentDetailsDateTimeOfDiscovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable,
  ): Page<DraftAdjudicationDto> =
    draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(
      startDate = startDate ?: LocalDate.now().minusWeeks(1),
      endDate = endDate ?: LocalDate.now(),
      pageable = pageable,
    )

  @PostMapping
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Starts a new draft adjudication.",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "New Adjudication Started",
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
  @ResponseStatus(HttpStatus.CREATED)
  fun startNewAdjudication(@RequestBody newAdjudicationRequest: NewAdjudicationRequest): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService
      .startNewAdjudication(
        newAdjudicationRequest.prisonerNumber,
        newAdjudicationRequest.gender,
        newAdjudicationRequest.agencyId,
        newAdjudicationRequest.overrideAgencyId,
        newAdjudicationRequest.locationId,
        newAdjudicationRequest.dateTimeOfIncident,
        newAdjudicationRequest.dateTimeOfDiscovery,
      )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @GetMapping(value = ["/{id}"])
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @Operation(summary = "Returns the draft adjudication details.")
  fun getDraftAdjudicationDetails(@PathVariable(name = "id") id: Long): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.getDraftAdjudicationDetails(id)

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PostMapping(value = ["/{id}/incident-statement"])
  @Operation(
    summary = "Add the incident statement to the draft adjudication.",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Incident Statement Added",
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
  @ResponseStatus(HttpStatus.CREATED)
  fun addIncidentStatement(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    incidentStatementRequest: IncidentStatementRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.addIncidentStatement(
      id,
      incidentStatementRequest.statement,
      incidentStatementRequest.completed,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/incident-details"])
  @Operation(summary = "Edit the incident details for a draft adjudication.")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun editIncidentDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    editIncidentDetailsRequest: EditIncidentDetailsRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentDetails(
      id,
      editIncidentDetailsRequest.locationId,
      editIncidentDetailsRequest.dateTimeOfIncident,
      editIncidentDetailsRequest.dateTimeOfDiscovery,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/incident-role"])
  @Operation(summary = "Edit the incident role for a draft adjudication.")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun editIncidentRole(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    editIncidentRoleRequest: EditIncidentRoleRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentRole(
      id,
      editIncidentRoleRequest.incidentRole,
      editIncidentRoleRequest.removeExistingOffences,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/associated-prisoner"])
  @Operation(summary = "Set the associated prisoner for a draft adjudication.")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun setIncidentRoleAssociatedPrisoner(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    associatedPrisonerRequest: IncidentRoleAssociatedPrisonerRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
      id,
      associatedPrisonerRequest,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/incident-statement"])
  @Operation(summary = "Edit the incident statement for a draft adjudication.")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun editIncidentStatement(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    editIncidentStatementRequest: IncidentStatementRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentStatement(
      id,
      statement = editIncidentStatementRequest.statement,
      completed = editIncidentStatementRequest.completed,
    )
    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/applicable-rules"])
  @Operation(summary = "Set applicable rules for incident")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun setApplicableRules(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    applicableRulesRequest: ApplicableRulesRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setIncidentApplicableRule(
      id,
      applicableRulesRequest.isYouthOffenderRule,
      applicableRulesRequest.removeExistingOffences,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @PutMapping(value = ["/{id}/gender"])
  @Operation(summary = "Set gender")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun setGender(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    genderRequest: GenderRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setGender(
      id,
      genderRequest.gender,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @DeleteMapping(value = ["/{id}"])
  @Operation(summary = "Delete by Id. Only owner can delete.")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  fun deleteDraftAdjudication(
    @PathVariable(name = "id") id: Long,
  ) {
    draftAdjudicationService.deleteDraftAdjudications(id)
  }
}
