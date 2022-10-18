package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService

@Schema(description = "Reported adjudication response")
data class ReportedAdjudicationResponse(
  @Schema(description = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto
)

@RestController
@RequestMapping("/reported-adjudications")
class ReportedAdjudicationBaseController {

  @Autowired
  lateinit var reportedAdjudicationService: ReportedAdjudicationService
}
