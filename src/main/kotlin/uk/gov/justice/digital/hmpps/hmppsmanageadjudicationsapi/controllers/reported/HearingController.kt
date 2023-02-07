package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeFinding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
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
  @Schema(description = "oic hearing type")
  val oicHearingType: OicHearingType,
)

@Schema(description = "Request to create a hearing outcome")
data class HearingOutcomeRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "the outcome code")
  val code: HearingOutcomeCode,
  @Schema(description = "reason")
  val reason: HearingOutcomeAdjournReason? = null,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "finding")
  val finding: HearingOutcomeFinding? = null,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea? = null,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class HearingController(
  private val hearingService: HearingService,
  private val hearingOutcomeService: HearingOutcomeService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "Create a new hearing")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.createHearing(
      adjudicationNumber, hearingRequest.locationId, hearingRequest.dateTimeOfHearing, hearingRequest.oicHearingType,
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
      adjudicationNumber, hearingId, hearingRequest.locationId, hearingRequest.dateTimeOfHearing, hearingRequest.oicHearingType,
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
  @GetMapping(value = ["/hearings/agency/{agencyId}"])
  fun getAllHearingsByAgencyAndDate(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "hearingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hearingDate: LocalDate,
  ): HearingSummaryResponse {
    val hearings = hearingService.getAllHearingsByAgencyIdAndDate(agencyId, hearingDate)

    return HearingSummaryResponse(
      hearings
    )
  }

  @Operation(summary = "create a hearing outcome")
  @PostMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}/outcome"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearingOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
    @RequestBody hearingOutcomeRequest: HearingOutcomeRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingOutcomeService.createHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      hearingId = hearingId,
      adjudicator = hearingOutcomeRequest.adjudicator,
      code = hearingOutcomeRequest.code,
      reason = hearingOutcomeRequest.reason,
      details = hearingOutcomeRequest.details,
      finding = hearingOutcomeRequest.finding,
      plea = hearingOutcomeRequest.plea,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "update a hearing outcome")
  @PutMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun updateHearingOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
    @RequestBody hearingOutcomeRequest: HearingOutcomeRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingOutcomeService.updateHearingOutcome(
      adjudicationNumber = adjudicationNumber,
      hearingId = hearingId,
      code = hearingOutcomeRequest.code,
      adjudicator = hearingOutcomeRequest.adjudicator,
      reason = hearingOutcomeRequest.reason,
      details = hearingOutcomeRequest.details,
      finding = hearingOutcomeRequest.finding,
      plea = hearingOutcomeRequest.plea,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
