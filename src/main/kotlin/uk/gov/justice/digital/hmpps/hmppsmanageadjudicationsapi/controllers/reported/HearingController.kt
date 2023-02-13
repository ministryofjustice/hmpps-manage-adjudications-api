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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService
import java.time.LocalDate
import java.time.LocalDateTime
import javax.validation.ValidationException

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
  @Schema(description = "the name of the adjudicator, optional when editing a referral")
  val adjudicator: String? = null,
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
  private val referralService: ReferralService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "Create a new hearing")
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.createHearing(
      adjudicationNumber = adjudicationNumber,
      locationId = hearingRequest.locationId,
      dateTimeOfHearing = hearingRequest.dateTimeOfHearing,
      oicHearingType = hearingRequest.oicHearingType,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PutMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "Amends leatest hearing")
  @ResponseStatus(HttpStatus.OK)
  fun amendHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.amendHearing(
      adjudicationNumber = adjudicationNumber,
      locationId = hearingRequest.locationId,
      dateTimeOfHearing = hearingRequest.dateTimeOfHearing,
      oicHearingType = hearingRequest.oicHearingType,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @DeleteMapping(value = ["/{adjudicationNumber}/hearing"])
  @Operation(summary = "deletes latest hearing")
  @ResponseStatus(HttpStatus.OK)
  fun deleteHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.deleteHearing(adjudicationNumber = adjudicationNumber)
    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "Get a list of hearings for a given date and agency")
  @GetMapping(value = ["/hearings/agency/{agencyId}"])
  fun getAllHearingsByAgencyAndDate(
    @PathVariable(name = "agencyId") agencyId: String,
    @RequestParam(name = "hearingDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) hearingDate: LocalDate,
  ): HearingSummaryResponse {
    val hearings = hearingService.getAllHearingsByAgencyIdAndDate(agencyId = agencyId, dateOfHearing = hearingDate)

    return HearingSummaryResponse(
      hearings
    )
  }

  @Operation(summary = "create a hearing outcome for latest hearing")
  @PostMapping(value = ["/{adjudicationNumber}/hearing/outcome"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearingOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingOutcomeRequest: HearingOutcomeRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication =
      when (hearingOutcomeRequest.code.outcomeCode) {
        null -> hearingOutcomeService.createHearingOutcome(
          adjudicationNumber = adjudicationNumber,
          adjudicator = validateAdjudicator(hearingOutcomeRequest.adjudicator),
          code = hearingOutcomeRequest.code,
          reason = hearingOutcomeRequest.reason,
          details = hearingOutcomeRequest.details,
          finding = hearingOutcomeRequest.finding,
          plea = hearingOutcomeRequest.plea,
        )
        else -> referralService.createReferral(
          adjudicationNumber = adjudicationNumber,
          code = hearingOutcomeRequest.code,
          adjudicator = validateAdjudicator(hearingOutcomeRequest.adjudicator),
          details = validateDetails(hearingOutcomeRequest.details)
        )
      }

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "update a hearing outcome for latest hearing")
  @PutMapping(value = ["/{adjudicationNumber}/hearing/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun updateHearingOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingOutcomeRequest: HearingOutcomeRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication =
      when (hearingOutcomeRequest.code.outcomeCode) {
        null -> hearingOutcomeService.updateHearingOutcome(
          adjudicationNumber = adjudicationNumber,
          code = hearingOutcomeRequest.code,
          adjudicator = validateAdjudicator(hearingOutcomeRequest.adjudicator),
          reason = hearingOutcomeRequest.reason,
          details = hearingOutcomeRequest.details,
          finding = hearingOutcomeRequest.finding,
          plea = hearingOutcomeRequest.plea,
        )
        else -> referralService.updateReferral(
          adjudicationNumber = adjudicationNumber,
          code = hearingOutcomeRequest.code,
          details = validateDetails(hearingOutcomeRequest.details)
        )
      }

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  companion object {
    fun validateAdjudicator(adjudicator: String?) =
      adjudicator ?: throw ValidationException("adjudicator is required")

    fun validateDetails(details: String?) =
      details ?: throw ValidationException("details is required")
  }
}
