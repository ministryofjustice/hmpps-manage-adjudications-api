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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftDamagesService

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

@RestController
@Tag(name = "12. Draft Damages")
class DraftDamagesController(
  private val damagesService: DraftDamagesService,
) : DraftAdjudicationBaseController() {

  @PutMapping(value = ["/{id}/damages"])
  @Operation(summary = "Set the damages for the draft adjudication.", description = "0 or more damages to be supplied")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setDamages(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    damagesRequest: DamagesRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = damagesService.setDamages(
      id,
      damagesRequest.damages,
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }
}
