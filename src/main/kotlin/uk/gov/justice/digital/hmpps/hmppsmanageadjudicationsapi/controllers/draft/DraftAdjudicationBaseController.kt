package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto

@Schema(description = "Draft adjudication response")
data class DraftAdjudicationResponse(
  @Schema(description = "The draft adjudication")
  val draftAdjudication: DraftAdjudicationDto
)

@RestController
@RequestMapping("/draft-adjudications")
@Validated
class DraftAdjudicationBaseController
