package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.ReportedAdjudicationService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import javax.validation.Valid
import javax.validation.constraints.Size

@Schema(description = "All hearings response")
data class HearingSummaryResponse(
  @Schema(description = "The hearing summaries response")
  val hearings: List<HearingSummaryDto>
)

@Schema(description = "Reported adjudication response")
data class ReportedAdjudicationResponse(
  @Schema(description = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto
)

@Schema(description = "Request to add a hearing")
data class HearingRequest(
  @Schema(description = "The id of the location of the hearing")
  val locationId: Long,
  @Schema(description = "Date and time of the hearing", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfHearing: LocalDateTime,

)

@Schema(description = "Request to set the state for an a reported adjudication")
data class ReportedAdjudicationStatusRequest(
  @Schema(description = "The status to set the reported adjudication to")
  val status: ReportedAdjudicationStatus,
  @Schema(description = "The reason the status has been set")
  @get:Size(
    max = 128,
    message = "The reason the status has been set exceeds the maximum character limit of {max}"
  )
  val statusReason: String? = null,
  @Schema(description = "Details of why the status has been set")
  @get:Size(
    max = 4000,
    message = "The details of why the status has been set exceeds the maximum character limit of {max}"
  )
  val statusDetails: String? = null,
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
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to incidentDate,incidentTime ASC"
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days"
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is start date"
    ),
    Parameter(
      name = "status",
      required = false,
      description = "optional status filter for reports"
    )
  )
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER')")
  @GetMapping("/agency/{agencyId}")
  fun getReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Optional<LocalDate>,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: Optional<LocalDate>,
    @RequestParam(name = "status") status: Optional<ReportedAdjudicationStatus>,
    @PageableDefault(sort = ["dateTimeOfIncident"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    return reportedAdjudicationService.getAllReportedAdjudications(
      agencyId,
      startDate.orElse(LocalDate.now().minusDays(3)),
      endDate.orElse(startDate.orElse(LocalDate.now())),
      status,
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
      description = "Sort as combined comma separated property and uppercase direction. Multiple sort params allowed to sort by multiple properties. Default to incidentDate,incidentTime ASC"
    ),
    Parameter(
      name = "startDate",
      required = false,
      description = "optional inclusive start date for results, default is today - 3 days"
    ),
    Parameter(
      name = "endDate",
      required = false,
      description = "optional inclusive end date for results, default is start date"
    ),
    Parameter(
      name = "status",
      required = false,
      description = "optional status filter for reports"
    )
  )
  @GetMapping("/my/agency/{agencyId}")
  fun getMyReportedAdjudications(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: Optional<LocalDate>,
    @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: Optional<LocalDate>,
    @RequestParam(name = "status") status: Optional<ReportedAdjudicationStatus>,
    @PageableDefault(sort = ["dateTimeOfIncident"], direction = Sort.Direction.DESC, size = 20) pageable: Pageable
  ): Page<ReportedAdjudicationDto> {
    return reportedAdjudicationService.getMyReportedAdjudications(
      agencyId,
      startDate.orElse(LocalDate.now().minusDays(3)),
      endDate.orElse(startDate.orElse(LocalDate.now())),
      status,
      pageable
    )
  }

  @PostMapping(value = ["/{adjudicationNumber}/create-draft-adjudication"])
  @Operation(summary = "Creates a draft adjudication from the reported adjudication with the given number.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.CREATED)
  fun createDraftAdjudication(@PathVariable(name = "adjudicationNumber") adjudicationNumber: Long): DraftAdjudicationResponse {
    val draftAdjudication = reportedAdjudicationService.createDraftFromReportedAdjudication(adjudicationNumber)
    return DraftAdjudicationResponse(
      draftAdjudication
    )
  }

  @PutMapping(value = ["/{adjudicationNumber}/status"])
  @Operation(summary = "Set the status for the reported adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun setStatus(
    @PathVariable(name = "adjudicationNumber") id: Long,
    @RequestBody @Valid reportedAdjudicationStatusRequest: ReportedAdjudicationStatusRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.setStatus(
      id,
      reportedAdjudicationStatusRequest.status,
      reportedAdjudicationStatusRequest.statusReason,
      reportedAdjudicationStatusRequest.statusDetails
    )

    return ReportedAdjudicationResponse(
      reportedAdjudication
    )
  }

  @PutMapping(value = ["/{adjudicationNumber}/damages/edit"])
  @Operation(summary = "Updates the damages for the reported adjudication.", description = "0 or more damages to be supplied, only updates records owned by current user")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateDamages(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid damagesRequest: DamagesRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.updateDamages(
      adjudicationNumber,
      damagesRequest.damages
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PutMapping(value = ["/{adjudicationNumber}/evidence/edit"])
  @Operation(summary = "Updates the evidence for the reported adjudication.", description = "0 or more evidence to be supplied, only updates records owned by current user")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateEvidence(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid evidenceRequest: EvidenceRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.updateEvidence(
      adjudicationNumber,
      evidenceRequest.evidence
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PutMapping(value = ["/{adjudicationNumber}/witnesses/edit"])
  @Operation(summary = "Updates the witnesses for the reported adjudication.", description = "0 or more witnesses to be supplied, only updates records owned by current user")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun updateWitnesses(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid witnessesRequest: WitnessesRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.updateWitnesses(
      adjudicationNumber,
      witnessesRequest.witnesses
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @PostMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "Create a new hearing")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.createHearing(
      adjudicationNumber, hearingRequest.locationId, hearingRequest.dateTimeOfHearing
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @DeleteMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}"])
  @Operation(summary = "deletes a hearing")
  @ResponseStatus(HttpStatus.OK)
  fun deleteHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = reportedAdjudicationService.deleteHearing(
      adjudicationNumber, hearingId
    )
    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "Get a list of hearings for a given date and agency")
  @Parameters(
    Parameter(
      name = "hearingDate",
      description = "date of hearings"
    ),
  )
  @GetMapping(value = ["/hearings/agency/{agencyId}"])
  fun getAllHearingsByAgencyAndDate(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "hearingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hearingDate: LocalDate,
  ): HearingSummaryResponse {
    val hearings = reportedAdjudicationService.getAllHearingsByAgencyIdAndDate(agencyId, hearingDate)

    return HearingSummaryResponse(
      hearings
    )
  }
}
