package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.IssuedStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService
import java.time.LocalDate

@Schema(description = "All issuable adjudications response")
data class IssuableAdjudicationsResponse(
  @Schema(description = "Th reported adjudications response")
  val reportedAdjudications: List<ReportedAdjudicationDto>,
)

@Schema(description = "Agency Report counts DTO")
data class AgencyReportCountsDto(
  @Schema(description = "total reports to review for agency")
  val reviewTotal: Long,
  @Schema(description = "total transferable reports to review for agency")
  val transferReviewTotal: Long,
)

@RestController
@Tag(name = "40. Reports")
class ReportsController(
  private val reportsService: ReportsService,
) : ReportedAdjudicationBaseController() {

  @Operation(summary = "Get all reported adjudications for caseload")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20",
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery DESC",
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today",
    ),
    Parameter(
      name = "status",
      required = true,
      description = "list of status filter for reports",
    ),
    Parameter(
      name = "transfersOnly",
      required = false,
      description = "optional filter for transferred reports only",
    ),
  )
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER')")
  @GetMapping("/reports")
  fun getReportedAdjudications(
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate?,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate?,
    @RequestParam(name = "status", required = true) statuses: List<ReportedAdjudicationStatus>,
    @RequestParam(name = "transfersOnly") transfersOnly: Boolean = false,
    @PageableDefault(sort = ["date_time_of_discovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    reportsService.getAllReportedAdjudications(
      startDate = startDate ?: LocalDate.now().minusDays(3),
      endDate = endDate ?: LocalDate.now(),
      statuses = statuses,
      transfersOnly = transfersOnly,
      pageable = pageable,
    )

  @Operation(summary = "Get my reported adjudications for caseload")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
      example = "0",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20",
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to dateTimeOfDiscovery DESC",
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today",
    ),
    Parameter(
      name = "status",
      required = true,
      description = "list of status filter for reports",
    ),
  )
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @GetMapping("/my-reports")
  fun getMyReportedAdjudications(
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate?,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate?,
    @RequestParam(name = "status", required = true) statuses: List<ReportedAdjudicationStatus>,
    @PageableDefault(sort = ["dateTimeOfDiscovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    reportsService.getMyReportedAdjudications(
      startDate = startDate ?: LocalDate.now().minusDays(3),
      endDate = endDate ?: LocalDate.now(),
      statuses = statuses,
      pageable = pageable,
    )

  @Operation(summary = "Get all reported adjudications for issue")
  @Parameters(
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 2 days",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is today",
    ),
  )
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @GetMapping("/for-issue")
  fun getReportedAdjudicationsForIssue(
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate?,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate?,
  ): IssuableAdjudicationsResponse =
    IssuableAdjudicationsResponse(
      reportsService.getAdjudicationsForIssue(
        startDate = startDate ?: LocalDate.now().minusDays(2),
        endDate = endDate ?: LocalDate.now(),
      ),
    )

  @Operation(summary = "Get all reported adjudications for print")
  @Parameters(
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive hearing start date for results, default is today",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive hearing end date for results, default is today + 2",
    ),
    Parameter(
      name = "issueStatus",
      required = true,
      description = "list of issue status, as comma separated String",
    ),
  )
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @GetMapping("/for-print")
  fun getReportedAdjudicationsForPrint(
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate?,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate?,
    @RequestParam(name = "issueStatus") issueStatuses: List<IssuedStatus>,
  ): IssuableAdjudicationsResponse =
    IssuableAdjudicationsResponse(
      reportsService.getAdjudicationsForPrint(
        startDate = startDate ?: LocalDate.now(),
        endDate = endDate ?: LocalDate.now().plusDays(2),
        issueStatuses = issueStatuses,
      ),
    )

  @Operation(summary = "Get report counts by agency")
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @GetMapping("/report-counts")
  fun getReportCounts(): AgencyReportCountsDto = reportsService.getReportCounts()

  @Operation(summary = "Get adjudications for an offender booking")
  @Parameters(
    Parameter(
      name = "page",
      description = "Results page you want to retrieve (0..N). Default 0, e.g. the first page",
    ),
    Parameter(
      name = "size",
      description = "Number of records per page. Default 20",
    ),
    Parameter(
      name = "sort",
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to date_time_of_discovery DESC",
    ),
    Parameter(
      name = "bookingId",
      required = true,
      description = "offender booking id",
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results",
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results",
    ),
    Parameter(
      name = "status",
      required = true,
      description = "list of status filter for reports",
    ),
    Parameter(
      name = "agency",
      required = true,
      description = "list of agencies to filter for reports, based on current booking",
    ),
  )
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @GetMapping("/booking/{bookingId}")
  fun getAdjudicationsForBooking(
    @PathVariable(name = "bookingId") bookingId: Long,
    @RequestParam(name = "startDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate? = null,
    @RequestParam(name = "endDate", required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate? = null,
    @RequestParam(name = "status", required = true) statuses: List<ReportedAdjudicationStatus>,
    @RequestParam(name = "agency", required = true) agencies: List<String>,
    @PageableDefault(sort = ["date_time_of_discovery"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable,
  ): Page<ReportedAdjudicationDto> = reportsService.getAdjudicationsForBooking(
    bookingId = bookingId,
    startDate = startDate,
    endDate = endDate,
    statuses = statuses,
    agencies = agencies,
    pageable = pageable,
  )

  @Operation(
    summary = "Get all adjudications for a prisoner",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "success",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
          ),
        ],
      ),
    ],
  )
  @Parameters(
    Parameter(
      name = "prisonerNumber",
      required = true,
      description = "prisoner number",
    ),
  )
  @PreAuthorize("hasRole('ALL_ADJUDICATIONS')")
  @GetMapping("/prisoner/{prisonerNumber}")
  fun getAdjudicationsForPrisoner(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
  ): List<ReportedAdjudicationDto> = reportsService.getReportsForPrisoner(prisonerNumber = prisonerNumber)
}
