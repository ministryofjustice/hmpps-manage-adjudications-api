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
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import springfox.documentation.annotations.ApiIgnore
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService
import java.time.LocalDate
import java.util.Optional

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

  @ApiOperation(value = "Get all reported adjudications for caseload")
  @ApiImplicitParams(ApiImplicitParam(name = "page", dataType = "java.lang.Integer", paramType = "query", value = "Results page you want to retrieve (0..N). Default 0, e.g. the first page", example = "0"), ApiImplicitParam(name = "size", dataType = "java.lang.Integer", paramType = "query", value = "Number of records per page. Default 20"), ApiImplicitParam(name = "sort", dataType = "java.lang.String", paramType = "query", value = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to incidentDate,incidentTime ASC"),
  ApiImplicitParam(name = "startDate", required = false, dataType = "java.time.LocalDate", paramType = "query", value = "optional inclusive start date for results, default is today"),ApiImplicitParam(name = "endDate", dataType = "java.time.LocalDate", required = false, paramType = "query", value = "optional inclusive end date for results, default is start date"), ApiImplicitParam(name = "status", dataType = "java.lang.String", required = false, paramType = "query", value = "optional status filter for reports"))
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER')")
  @GetMapping("/agency/{agencyId}")
  fun getReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Optional<LocalDate>,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  endDate: Optional<LocalDate>,
    @RequestParam(name = "status") status: Optional<ReportedAdjudicationStatus>,
    @ApiIgnore
    @PageableDefault(sort = ["dateTimeOfIncident"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    val reportDate = startDate.orElse(LocalDate.now())
    return reportedAdjudicationService.getAllReportedAdjudications(agencyId, reportDate, endDate.orElse(reportDate), status, pageable)
  }

  @ApiOperation(value = "Get my reported adjudications for caseload")
  @ApiImplicitParams(ApiImplicitParam(name = "page", dataType = "java.lang.Integer", paramType = "query", value = "Results page you want to retrieve (0..N). Default 0, e.g. the first page", example = "0"), ApiImplicitParam(name = "size", dataType = "java.lang.Integer", paramType = "query", value = "Number of records per page. Default 20"), ApiImplicitParam(name = "sort", dataType = "java.lang.String", paramType = "query", value = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to incidentDate,incidentTime ASC"),
    ApiImplicitParam(name = "startDate", dataType = "java.time.LocalDate", required = false, paramType = "query", value = "optional inclusive start date for results, default is today"),ApiImplicitParam(name = "endDate", dataType = "java.time.LocalDate", required = false, paramType = "query", value = "optional inclusive end date for results, default is start date"), ApiImplicitParam(name = "status", dataType = "java.lang.String", required = false, paramType = "query", value = "optional status filter for reports"))
  @GetMapping("/my/agency/{agencyId}")
  fun getMyReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Optional<LocalDate>,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)  endDate: Optional<LocalDate>,
    @RequestParam(name = "status") status: Optional<ReportedAdjudicationStatus>,
    @ApiIgnore
    @PageableDefault(sort = ["dateTimeOfIncident"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    val reportDate = startDate.orElse(LocalDate.now())
    return reportedAdjudicationService.getMyReportedAdjudications(agencyId, reportDate, endDate.orElse(reportDate), status, pageable)
  }

  @PostMapping(value = ["/{adjudicationNumber}/create-draft-adjudication"])
  @ApiOperation(value = "Creates a draft adjudication from the reported adjudication with the given number.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createDraftAdjudication(@PathVariable(name = "adjudicationNumber") adjudicationNumber: Long): DraftAdjudicationResponse {
    val draftAdjudication = reportedAdjudicationService.createDraftFromReportedAdjudication(adjudicationNumber)
    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }
}
