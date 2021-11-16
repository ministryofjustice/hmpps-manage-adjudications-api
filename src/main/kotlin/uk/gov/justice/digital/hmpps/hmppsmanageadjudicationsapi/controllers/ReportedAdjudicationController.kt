package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.annotations.ApiImplicitParam
import io.swagger.annotations.ApiImplicitParams
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import io.swagger.annotations.ApiOperation
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import springfox.documentation.annotations.ApiIgnore
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
  @ApiOperation(value = "Get my reported adjudications for caseload")
  @ApiImplicitParams(ApiImplicitParam(name = "page", dataType = "java.lang.Integer", paramType = "query", value = "Results page you want to retrieve (0..N). Default 0, e.g. the first page", example = "0"), ApiImplicitParam(name = "size", dataType = "java.lang.Integer", paramType = "query", value = "Number of records per page. Default 20"), ApiImplicitParam(name = "sort", dataType = "java.lang.String", paramType = "query", value = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to incidentDate,incidentTime ASC"))
  @GetMapping("/my/agency/{agencyId}")
  fun getMyReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @ApiIgnore
    @PageableDefault(sort = ["incidentDate", "incidentTime"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    return reportedAdjudicationService.getMyReportedAdjudications(agencyId, pageable)
  }
}
