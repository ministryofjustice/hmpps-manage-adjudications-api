package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftEvidenceService

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

@RestController
@Tag(name = "13. Draft Evidence")
class DraftEvidenceController(
  private val evidenceService: DraftEvidenceService,
) : DraftAdjudicationBaseController() {

  @PutMapping(value = ["/{id}/evidence"])
  @Operation(summary = "Set the evidence for the draft adjudication.", description = "0 or more evidence to be supplied")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setEvidence(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    evidenceRequest: EvidenceRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = evidenceService.setEvidence(
      id,
      evidenceRequest.evidence,
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }
}
