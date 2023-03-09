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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService
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

@Schema(description = "Request to create a referral for latest hearing")
data class ReferralRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "the outcome code")
  val code: HearingOutcomeCode,
  @Schema(description = "details")
  val details: String,
)

@Schema(description = "Request to create an adjourn for latest hearing")
data class AdjournRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "the adjourn resaon")
  val reason: HearingOutcomeAdjournReason,
  @Schema(description = "details")
  val details: String,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea,
)

@Schema(description = "amend hearing outcome request")
data class AmendHearingOutcomeRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String? = null,
  @Schema(description = "the adjourn reason")
  val reason: HearingOutcomeAdjournReason? = null,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea? = null,
  @Schema(description = "caution")
  val caution: Boolean? = null,
  @Schema(description = "amount of damages")
  val amount: Double? = null,
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
  fun createHearingV1(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.createHearingV1(
      adjudicationNumber, hearingRequest.locationId, hearingRequest.dateTimeOfHearing, hearingRequest.oicHearingType,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PutMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}"])
  @Operation(summary = "Amend an existing hearing")
  @ResponseStatus(HttpStatus.OK)
  fun amendHearingV1(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
    @RequestBody hearingRequest: HearingRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.amendHearingV1(
      adjudicationNumber, hearingId, hearingRequest.locationId, hearingRequest.dateTimeOfHearing, hearingRequest.oicHearingType,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @DeleteMapping(value = ["/{adjudicationNumber}/hearing/{hearingId}"])
  @Operation(summary = "deletes a hearing")
  @ResponseStatus(HttpStatus.OK)
  fun deleteHearingV1(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "hearingId") hearingId: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingService.deleteHearingV1(
      adjudicationNumber, hearingId
    )
    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PostMapping(value = ["/{adjudicationNumber}/hearing/v2"])
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

  @PutMapping(value = ["/{adjudicationNumber}/hearing/v2"])
  @Operation(summary = "Amends latest hearing")
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

  @DeleteMapping(value = ["/{adjudicationNumber}/hearing/v2"])
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

  @Operation(summary = "create a referral for latest hearing")
  @PostMapping(value = ["/{adjudicationNumber}/hearing/outcome/referral"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createReferral(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody referralRequest: ReferralRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication =
      referralService.createReferral(
        adjudicationNumber = adjudicationNumber,
        code = referralRequest.code.validateReferral(),
        adjudicator = referralRequest.adjudicator,
        details = referralRequest.details
      )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a adjourn for latest hearing")
  @PostMapping(value = ["/{adjudicationNumber}/hearing/outcome/adjourn"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createAdjourn(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody adjournRequest: AdjournRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication =
      hearingOutcomeService.createAdjourn(
        adjudicationNumber = adjudicationNumber,
        adjudicator = adjournRequest.adjudicator,
        details = adjournRequest.details,
        reason = adjournRequest.reason,
        plea = adjournRequest.plea,
      )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @DeleteMapping(value = ["/{adjudicationNumber}/hearing/outcome/adjourn"])
  @Operation(summary = "removes the adjourn outcome")
  @ResponseStatus(HttpStatus.OK)
  fun removeAdjourn(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = hearingOutcomeService.removeAdjourn(adjudicationNumber = adjudicationNumber)
    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
