package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OffenceRuleDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService

@Schema(description = "Request to update the list of offence details for a draft adjudication")
data class OffenceDetailsRequest(
  @Schema(description = "The details of the offence the prisoner is accused of")
  val offenceDetails: OffenceDetailsRequestItem,
)

@Schema(description = "Details of an offence")
data class OffenceDetailsRequestItem(
  @Schema(
    description = "The offence code",
    title = "This is the unique number relating to the type of offence they have been alleged to have committed",
    example = "3",
  )
  val offenceCode: Int,
  @Schema(description = "The prison number of the victim involved in the incident, if relevant", example = "G2996UX")
  val victimPrisonersNumber: String? = null,
  @Schema(
    description = "The username of the member of staff who is a victim of the incident, if relevant",
    example = "ABC12D",
  )
  val victimStaffUsername: String? = null,
  @Schema(
    description = "The name of the victim (who is not a member of staff or a prisoner) involved in the incident, if relevant",
    example = "Bob Hope",
  )
  val victimOtherPersonsName: String? = null,
  @Schema(description = "optional list of protected characteristics involved in the offence")
  val protectedCharacteristics: List<Characteristic>? = null,
)

@RestController
@Tag(name = "15. Draft Offence")
class DraftOffenceController(
  private val incidentOffenceService: DraftOffenceService,
) : DraftAdjudicationBaseController() {

  @PutMapping(value = ["/{id}/offence-details"])
  @Operation(
    summary = "Set the offence details for the draft adjudication.",
    description = "At least one set of offence details must be supplied",
  )
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS') and hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun setOffenceDetails(
    @PathVariable(name = "id") id: Long,
    @RequestBody @Valid
    offenceDetailsRequest: OffenceDetailsRequest,
  ): DraftAdjudicationResponse {
    val draftAdjudication = incidentOffenceService.setOffenceDetails(
      id,
      offenceDetailsRequest.offenceDetails,
    )

    return DraftAdjudicationResponse(
      draftAdjudication,
    )
  }

  @GetMapping("/offence-rule/{offenceCode}")
  @Operation(summary = "Returns details of the offence rule relating to this offence code.")
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  fun getOffenceRule(
    @PathVariable(name = "offenceCode") offenceCode: Int,
    @RequestParam(value = "youthOffender") isYouthOffender: Boolean,
    @RequestParam(value = "gender", required = false) gender: Gender?,
  ): OffenceRuleDetailsDto = incidentOffenceService.getRule(offenceCode, isYouthOffender, gender ?: Gender.MALE)

  @GetMapping("/offence-rules")
  @Operation(summary = "Returns all the offence rules")
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  fun getOffenceRules(
    @RequestParam(value = "youthOffender") isYouthOffender: Boolean,
    @RequestParam(value = "gender", required = false) gender: Gender?,
    @RequestParam(value = "version") version: Int,
  ): List<OffenceRuleDetailsDto> = incidentOffenceService.getRules(
    isYouthOffender = isYouthOffender,
    gender = gender ?: Gender.MALE,
    version = version,
  )
}
