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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftWitnessesService

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
@Tag(name = "14. Draft Witnesses")
class DraftWitnessesController(
  private val witnessesService: DraftWitnessesService,
) : DraftAdjudicationBaseController() {

  @PutMapping(value = ["/{id}/witnesses"])
  @Operation(summary = "Set the witnesses for the draft adjudication.", description = "0 or more witnesses to be supplied")
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setWitnesses(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    witnessesRequest: WitnessesRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = witnessesService.setWitnesses(
      id,
      witnessesRequest.witnesses,
    )

    return DraftAdjudicationResponse(draftAdjudication)
  }
}
