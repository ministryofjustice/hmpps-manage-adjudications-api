package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@Schema(description = "Request to add a police referral, or refer gov request")
data class ReferralDetailsRequest(
  @Schema(description = "details")
  val details: String,
)

@Schema(description = "Request to add a not proceed - via referral or without hearing")
data class NotProceedRequest(
  @Schema(description = "details")
  val details: String,
  @Schema(description = "reason")
  val reason: NotProceedReason,
)

@Schema(description = "Request to add a not proceed - hearing completed")
data class HearingCompletedNotProceedRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea,
  @Schema(description = "reason")
  val reason: NotProceedReason,
  @Schema(description = "details")
  val details: String,
)

@Schema(description = "Request to add dismissed - hearing completed")
data class HearingCompletedDismissedRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea,
  @Schema(description = "details")
  val details: String,
)

@Schema(description = "Request to add charge proved - hearing completed")
data class HearingCompletedChargeProvedRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea,
)

@Schema(description = "Request to quash charge")
data class QuashedRequest(
  @Schema(description = "details")
  val details: String,
  @Schema(description = "reason")
  val reason: QuashedReason,
)

@Schema(description = "amend outcome without a hearing - NOT PROCEED, REFER POLICE, QUASHED")
data class AmendOutcomeRequest(
  @Schema(description = "details")
  val details: String,
  @Schema(description = "not proceed reason")
  val reason: NotProceedReason? = null,
  @Schema(description = "quashed reason")
  val quashedReason: QuashedReason? = null,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "31. Outcomes")
class OutcomeController(
  private val outcomeService: OutcomeService,
  private val referralService: ReferralService,
  private val completedHearingService: CompletedHearingService,
) : ReportedAdjudicationBaseController() {

  @Operation(
    summary = "create a not proceed outcome - via referral or without hearing",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Not Proceeded Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/outcome/not-proceed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createNotProceed(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody notProceedRequest: NotProceedRequest,
  ): ReportedAdjudicationResponse =
    outcomeService.createNotProceed(
      chargeNumber = chargeNumber,
      details = notProceedRequest.details,
      reason = notProceedRequest.reason,
    ).toResponse()

  @Operation(
    summary = "create a prosecution outcome",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Prosecution Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/outcome/prosecution"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createProsecution(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    outcomeService.createProsecution(
      chargeNumber = chargeNumber,
    ).toResponse()

  @Operation(
    summary = "create a referral outcome for refer gov",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Refer Gov Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/outcome/refer-gov"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createReferGov(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody referGovRequest: ReferralDetailsRequest,
  ): ReportedAdjudicationResponse =
    outcomeService.createReferGov(
      chargeNumber = chargeNumber,
      details = referGovRequest.details,
    ).toResponse()

  @Operation(
    summary = "quash an outcome",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Outcome Quashed",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/outcome/quashed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createQuashed(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody quashedRequest: QuashedRequest,
  ): ReportedAdjudicationResponse =
    outcomeService.createQuashed(
      chargeNumber = chargeNumber,
      reason = quashedRequest.reason,
      details = quashedRequest.details,
    ).toResponse()

  @Operation(
    summary = "create a police refer outcome",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Police Reference Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/outcome/refer-police"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createRefPolice(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody policeReferralRequest: ReferralDetailsRequest,
  ): ReportedAdjudicationResponse =
    outcomeService.createReferral(
      chargeNumber = chargeNumber,
      code = OutcomeCode.REFER_POLICE,
      details = policeReferralRequest.details,
    ).toResponse()

  @Operation(
    summary = "create a dismissed from hearing outcome",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Charge Dismissed Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/complete-hearing/dismissed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createDismissed(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody completedDismissedRequest: HearingCompletedDismissedRequest,
  ): ReportedAdjudicationResponse =
    completedHearingService.createDismissed(
      chargeNumber = chargeNumber,
      adjudicator = completedDismissedRequest.adjudicator,
      plea = completedDismissedRequest.plea,
      details = completedDismissedRequest.details,
    ).toResponse()

  @Operation(
    summary = "create a not proceed from hearing outcome",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Not Proceeded Created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/complete-hearing/not-proceed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createNotProceedFromHearing(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody completedNotProceedRequest: HearingCompletedNotProceedRequest,
  ): ReportedAdjudicationResponse =
    completedHearingService.createNotProceed(
      chargeNumber = chargeNumber,
      adjudicator = completedNotProceedRequest.adjudicator,
      plea = completedNotProceedRequest.plea,
      reason = completedNotProceedRequest.reason,
      details = completedNotProceedRequest.details,
    ).toResponse()

  @PostMapping(value = ["/{chargeNumber}/complete-hearing/charge-proved/v2"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createChargeProvedFromHearingV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody chargeProvedRequest: HearingCompletedChargeProvedRequest,
  ): ReportedAdjudicationResponse =
    completedHearingService.createChargeProved(
      chargeNumber = chargeNumber,
      adjudicator = chargeProvedRequest.adjudicator,
      plea = chargeProvedRequest.plea,
    ).toResponse()

  @Operation(summary = "remove a referral")
  @DeleteMapping(value = ["/{chargeNumber}/remove-referral"])
  @ResponseStatus(HttpStatus.OK)
  fun removeReferral(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    referralService.removeReferral(
      chargeNumber = chargeNumber,
    ).toResponse()

  @Operation(summary = "remove a not proceed without a referral outcome, or a quashed outcome")
  @DeleteMapping(value = ["/{chargeNumber}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun removeNotProceedWithoutReferralOrQuashed(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    outcomeService.deleteOutcome(
      chargeNumber = chargeNumber,
    ).toResponse()

  @Operation(summary = "remove a completed hearing outcome")
  @DeleteMapping(value = ["/{chargeNumber}/remove-completed-hearing"])
  @ResponseStatus(HttpStatus.OK)
  fun removeCompletedHearingOutcome(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    completedHearingService.removeOutcome(
      chargeNumber = chargeNumber,
    ).toResponse()

  @Operation(summary = "amend outcome without a hearing (refer police, not proceed or quashed), unless its a referral outcome - not proceed")
  @PutMapping(value = ["/{chargeNumber}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun amendOutcome(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody amendOutcomeRequest: AmendOutcomeRequest,
  ): ReportedAdjudicationResponse =
    outcomeService.amendOutcomeViaApi(
      chargeNumber = chargeNumber,
      details = amendOutcomeRequest.details,
      reason = amendOutcomeRequest.reason,
      quashedReason = amendOutcomeRequest.quashedReason,
    ).toResponse()
}
