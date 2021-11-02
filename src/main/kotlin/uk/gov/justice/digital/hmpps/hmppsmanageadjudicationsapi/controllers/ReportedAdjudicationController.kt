package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService

@ApiModel("Reported adjudication response")
data class ReportedAdjudicationResponse(
  @ApiModelProperty(value = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto
)

@RestController
@RequestMapping("/reported-adjudications")
class ReportedAdjudicationController {

  @Autowired
  lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @GetMapping(value = ["/{adjudicationNumber}"])
  fun getReportedAdjudicationDetails(@PathVariable(name = "adjudicationNumber") adjudicationNumber: Long): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.getReportedAdjudicationDetails(adjudicationNumber)

    return ReportedAdjudicationResponse(
      reportedAdjudication
    )
  }
}
