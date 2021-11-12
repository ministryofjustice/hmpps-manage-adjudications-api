package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService



@ApiModel("Reported adjudication response")
data class ReportedAdjudicationResponse(
  @ApiModelProperty(value = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto
)

@ApiModel("My reported adjudication response")
data class MyReportedAdjudicationsResponse(
  @ApiModelProperty("My reported adjudications")
  val reportedAdjudications: List<ReportedAdjudicationDto>
)

@ApiModel("My paged reported adjudication response")
data class MyPagedReportedAdjudicationsResponse(
  @ApiModelProperty("My paged reported adjudications")
  val pagedReportedAdjudications: PageResponse<ReportedAdjudicationDto>
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

  @GetMapping("/my/location/{locationId}")
  fun getMyReportedAdjudications2(@PathVariable(name = "locationId") locationId: Long,
                                  pageRequest: PageRequest
                                  ): MyPagedReportedAdjudicationsResponse {
    val myReportedAdjudications = reportedAdjudicationService.getMyReportedAdjudications(locationId, pageRequest)

    return MyPagedReportedAdjudicationsResponse(
      pagedReportedAdjudications = myReportedAdjudications
    )
  }

  @GetMapping("/my")
  fun getMyReportedAdjudications(): MyReportedAdjudicationsResponse {
    val myReportedAdjudications = reportedAdjudicationService.getMyReportedAdjudications()

    return MyReportedAdjudicationsResponse(
      reportedAdjudications = myReportedAdjudications
    )
  }
}
