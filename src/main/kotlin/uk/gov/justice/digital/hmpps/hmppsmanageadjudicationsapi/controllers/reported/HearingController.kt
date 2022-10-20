package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.Parameters
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "All hearings response")
data class HearingSummaryResponse(
  @Schema(description = "The hearing summaries response")
  val hearings: List<HearingSummaryDto>
)

@Schema(description = "Request to add a hearing")
data class HearingRequest(
  @Schema(description = "The id of the location of the hearing")
  val locationId: Long,
  @Schema(description = "Date and time of the hearing", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfHearing: LocalDateTime,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class HearingController(
  val hearingService: HearingService,
  val reportedAdjudicationService: ReportedAdjudicationService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "Create a new hearing")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.createHearing(
      adjudicationNumber, hearingRequest.locationId, hearingRequest.dateTimeOfHearing
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PutMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}"])
  @Operation(summary = "Amend an existing hearing")
  @ResponseStatus(HttpStatus.OK)
  fun amendHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.amendHearing(
      adjudicationNumber, hearingId, hearingRequest.locationId, hearingRequest.dateTimeOfHearing
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @DeleteMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}"])
  @Operation(summary = "deletes a hearing")
  @ResponseStatus(HttpStatus.OK)
  fun deleteHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.deleteHearing(
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
