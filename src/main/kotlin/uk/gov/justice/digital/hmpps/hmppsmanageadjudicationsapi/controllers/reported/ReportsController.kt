package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService
import java.time.LocalDate

@Schema(description = "All reported adjudications to issue response")
data class AdjudicationsToIssueResponse(
  @Schema(description = "The reported adjudication to issue response")
  val reportedAdjudications: List<ReportedAdjudicationDto>
)

@RestController
class ReportsController(
  private val reportsService: ReportsService
) : ReportedAdjudicationBaseController() {

  @Operation(summary = "Get all reported adjudications for caseload")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20"
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery ASC"
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days"
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today"
    ),
    Parameter(
      name = "status",
      required = true,
      description = "list of status filter for reports"
    )
  )
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER')")
  @GetMapping("/agency/{agencyId}")
  fun getReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    @RequestParam(name = "status", required = true) statuses: List<ReportedAdjudicationStatus>,
    @PageableDefault(sort = ["dateTimeOfDiscovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    return reportsService.getAllReportedAdjudications(
      agencyId,
      startDate ?: LocalDate.now().minusDays(3),
      endDate ?: LocalDate.now(),
      statuses,
      pageable
    )
  }

  @Operation(summary = "Get my reported adjudications for caseload")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
      example = "0"
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20"
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery ASC"
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days"
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today"
    ),
    Parameter(
      name = "status",
      required = true,
      description = "list of status filter for reports"
    )
  )
  @GetMapping("/my/agency/{agencyId}")
  fun getMyReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    @RequestParam(name = "status", required = true) statuses: List<ReportedAdjudicationStatus>,
    @PageableDefault(sort = ["dateTimeOfDiscovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    return reportsService.getMyReportedAdjudications(
      agencyId,
      startDate ?: LocalDate.now().minusDays(3),
      endDate ?: LocalDate.now(),
      statuses,
      pageable
    )
  }

  @Operation(summary = "Get all reported adjudications for caseload for issue")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
      example = "0"
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20"
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery ASC"
    ),

    Parameter(
      name = "locationId",
      required = false,
      description = "Location id, optional if all",
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 2 days"
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today"
    ),
  )
  @GetMapping("/agency/{agencyId}/issue")
  fun getReportedAdjudicationsForIssue(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "locationId") locationId: Long?,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
    @PageableDefault(sort = ["dateTimeOfDiscovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {

    return reportsService.getAdjudicationsForIssue(
      agencyId = agencyId,
      locationId = locationId,
      startDate = startDate ?: LocalDate.now().minusDays(2),
      endDate = endDate ?: LocalDate.now(),
      pageable = pageable,
    )
  }
}
