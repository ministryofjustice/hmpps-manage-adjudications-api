package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
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
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.DraftAdjudicationService
import java.time.LocalDateTime
import javax.validation.Valid
import javax.validation.constraints.Size

@Schema(description = "Request to create a new draft adjudication")
data class NewAdjudicationRequest(
  @Schema(description = "Prison number assigned to a prisoner", example = "G2996UX")
  val prisonerNumber: String,
  @Schema(description = "The agency id (or caseload) associated with this adjudication", example = "MDI")
  val agencyId: String,
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime,
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

@Schema(description = "Request to update the list of offence details for a draft adjudication")
data class OffenceDetailsRequest(
  @Schema(description = "The details of all offences the prisoner is accused of")
  val offenceDetails: List<OffenceDetailsRequestItem>,
)

@Schema(description = "Details of an offence")
data class OffenceDetailsRequestItem(
  @Schema(description = "The offence code", title = "This is the unique number relating to the type of offence they have been alleged to have committed", example = "3")
  val offenceCode: Int,
  @Schema(description = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @Schema(description = "The username of the member of staff who is a victim of the incident, if relevant", example = "ABC12D")
  val victimStaffUsername: String? = null,
  @Schema(description = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant", example = "Bob Hope")
  val victimOtherPersonsName: String? = null,
)

@Schema(description = "Request to add or edit the incident statement for a draft adjudication")
data class IncidentStatementRequest(
  @Schema(description = "The statement regarding the incident")
  @get:Size(
    max = 4000,
    message = "The incident statement exceeds the maximum character limit of {max}"
  )
  val statement: String? = null,
  val completed: Boolean? = false
)

@Schema(description = "Request to edit the incident details")
data class EditIncidentDetailsRequest(
  @Schema(description = "The id of the location the incident took place")
  val locationId: Long? = null,
  @Schema(description = "Date and time the incident occurred", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIncident: LocalDateTime? = null,
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

@Schema(description = "Draft adjudication response")
data class DraftAdjudicationResponse(
  @Schema(description = "The draft adjudication")
  val draftAdjudication: DraftAdjudicationDto
)

@Schema(description = "In progress draft adjudication response")
data class InProgressAdjudicationResponse(
  @Schema(description = "All in progress adjudications")
  val draftAdjudications: List<DraftAdjudicationDto>
)

@Schema(description = "Request to update the list of damages for a draft adjudication")
data class DamagesRequest(
  @Schema(description = "The details of all damages the prisoner is accused of")
  val damages: List<DamageRequestItem>,
)

@Schema(description = "Details of Damage")
data class DamageRequestItem(
  @Schema(description = "The damage code", example = "CLEANING")
  val code: DamageCode,
  @Schema(description = "details of the damage", example = "the kettle was broken")
  val details: String,
  @Schema(description = "optional reporter as per token, used when editing", example = "A_USER")
  val reporter: String? = null,
)

@Schema(description = "Request to update the list of evidence for a draft adjudication")
data class EvidenceRequest(
  @Schema(description = "The details of all evidence")
  val evidence: List<EvidenceRequestItem>,
)

@Schema(description = "Details of Evidence")
data class EvidenceRequestItem(
  @Schema(description = "The evidence code", example = "PHOTO")
  val code: EvidenceCode,
  @Schema(description = "Evidence identifier", example = "Tag number or Camera number")
  val identifier: String? = null,
  @Schema(description = "details of the evidence", example = "ie description of photo")
  val details: String,
  @Schema(description = "optional reporter as per token, used when editing", example = "A_USER")
  val reporter: String? = null,
)

@Schema(description = "Request to update the list of witnesses for a draft adjudication")
data class WitnessesRequest(
  @Schema(description = "The details of all evidence")
  val witnesses: List<WitnessRequestItem>,
)

@Schema(description = "Details of Witness")
data class WitnessRequestItem(
  @Schema(description = "The witness code", example = "PRISON_OFFICER")
  val code: WitnessCode,
  @Schema(description = "Witness first name", example = "Fred")
  val firstName: String,
  @Schema(description = "Witness last name", example = "Kruger")
  val lastName: String,
  @Schema(description = "optional reporter as per token, used when editing", example = "A_USER")
  val reporter: String? = null,
)

@RestController
@RequestMapping("/draft-adjudications")
@Validated
class DraftAdjudicationController {
  @Autowired
  lateinit var draftAdjudicationService: DraftAdjudicationService

  @GetMapping("/my/agency/{agencyId}")
  @Operation(summary = "Returns all the in progress draft adjudications created by the current user. Default sort is by earliest incident date and time.")
  fun getCurrentUsersInProgressDraftAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
  ): InProgressAdjudicationResponse = InProgressAdjudicationResponse(
    draftAdjudications = draftAdjudicationService.getCurrentUsersInProgressDraftAdjudications(agencyId)
  )

  @GetMapping("/offence-rule/{offenceCode}")
  @Operation(summary = "Returns details of the offence rule relating to this offence code.")
  fun getOffenceRule(
    @PathVariable(name = "offenceCode") offenceCode: Int,
    @RequestParam(value = "youthOffender") isYouthOffender: Boolean,
  ): OffenceRuleDetailsDto = draftAdjudicationService.lookupRuleDetails(offenceCode, isYouthOffender)

  @PostMapping
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @Operation(summary = "Starts a new draft adjudication.")
  @ResponseStatus(HttpStatus.CREATED)
  fun startNewAdjudication(@RequestBody newAdjudicationRequest: NewAdjudicationRequest): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService
      .startNewAdjudication(
        newAdjudicationRequest.prisonerNumber,
        newAdjudicationRequest.agencyId,
        newAdjudicationRequest.locationId,
        newAdjudicationRequest.dateTimeOfIncident,
      )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @GetMapping(value = ["/{id}"])
  @Operation(summary = "Returns the draft adjudication details.")
  fun getDraftAdjudicationDetails(@PathVariable(name = "id") id: Long): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.getDraftAdjudicationDetails(id)

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/offence-details"])
  @Operation(summary = "Set the offence details for the draft adjudication.", description = "At least one set of offence details must be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setOffenceDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid offenceDetailsRequest: OffenceDetailsRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setOffenceDetails(
      id,
      offenceDetailsRequest.offenceDetails
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PostMapping(value = ["/{id}/incident-statement"])
  @Operation(summary = "Add the incident statement to the draft adjudication.")
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
  @Operation(summary = "Edit the incident details for a draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun editIncidentDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid editIncidentDetailsRequest: EditIncidentDetailsRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentDetails(
      id,
      editIncidentDetailsRequest.locationId,
      editIncidentDetailsRequest.dateTimeOfIncident,
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/incident-role"])
  @Operation(summary = "Edit the incident role for a draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun editIncidentRole(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid editIncidentRoleRequest: EditIncidentRoleRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.editIncidentRole(
      id,
      editIncidentRoleRequest.incidentRole,
      editIncidentRoleRequest.removeExistingOffences
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/associated-prisoner"])
  @Operation(summary = "Set the associated prisoner for a draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun setIncidentRoleAssociatedPrisoner(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid associatedPrisonerRequest: IncidentRoleAssociatedPrisonerRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setIncidentRoleAssociatedPrisoner(
      id,
      associatedPrisonerRequest,
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/incident-statement"])
  @Operation(summary = "Edit the incident statement for a draft adjudication.")
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

  @PutMapping(value = ["/{id}/applicable-rules"])
  @Operation(summary = "Set applicable rules for incident")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun setApplicableRules(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid applicableRulesRequest: ApplicableRulesRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setIncidentApplicableRule(
      id,
      applicableRulesRequest.isYouthOffenderRule,
      applicableRulesRequest.removeExistingOffences,
    )

    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{id}/damages"])
  @Operation(summary = "Set the damages for the draft adjudication.", description = "0 or more damages to be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setDamages(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid damagesRequest: DamagesRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setDamages(
      id,
      damagesRequest.damages
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PutMapping(value = ["/{id}/damages/edit"])
  @Operation(summary = "Updates the damages for the draft adjudication.", description = "0 or more damages to be supplied, only updates records owned by current user")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateDamages(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid damagesRequest: DamagesRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.updateDamages(
      id,
      damagesRequest.damages
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PutMapping(value = ["/{id}/evidence"])
  @Operation(summary = "Set the evidence for the draft adjudication.", description = "0 or more evidence to be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setEvidence(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid evidenceRequest: EvidenceRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setEvidence(
      id,
      evidenceRequest.evidence
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PutMapping(value = ["/{id}/evidence/edit"])
  @Operation(summary = "Updates the evidence for the draft adjudication.", description = "0 or more evidence to be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateEvidence(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid evidenceRequest: EvidenceRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.updateEvidence(
      id,
      evidenceRequest.evidence
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PutMapping(value = ["/{id}/witnesses"])
  @Operation(summary = "Set the witnesses for the draft adjudication.", description = "0 or more witnesses to be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setWitnesses(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid witnessesRequest: WitnessesRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.setWitnesses(
      id,
      witnessesRequest.witnesses
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PutMapping(value = ["/{id}/witnesses/edit"])
  @Operation(summary = "Update the witnesses for the draft adjudication.", description = "0 or more witnesses to be supplied")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateWitnesses(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid witnessesRequest: WitnessesRequest
  ): DraftAdjudicationResponse {
    val draftAdjudication = draftAdjudicationService.updateWitnesses(
      id,
      witnessesRequest.witnesses
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }

  @PostMapping(value = ["/{id}/complete-draft-adjudication"])
  @Operation(summary = "Submits the draft adjudication to Prison-API, creates a submitted adjudication record and removes the draft adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun completeDraftAdjudication(@PathVariable(name = "id") id: Long): ReportedAdjudicationDto =
    draftAdjudicationService.completeDraftAdjudication(id)

  @DeleteMapping(value = ["/orphaned"])
  fun deleteOrphanedDraftAdjudications(): Unit =
    draftAdjudicationService.deleteOrphanedDraftAdjudications()
}
