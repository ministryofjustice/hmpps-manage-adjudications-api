package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@Schema(description = "Request to add a police referral")
data class PoliceReferralRequest(
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
  @Schema(description = "optional amount - money being recovered for damages ", required = false)
  val amount: Double? = null,
  @Schema(description = "is this a caution")
  val caution: Boolean,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class OutcomeController(
  private val outcomeService: OutcomeService,
  private val referralService: ReferralService,
  private val completedHearingService: CompletedHearingService,
) : ReportedAdjudicationBaseController() {

  @Operation(summary = "create a not proceed outcome")
  @PostMapping(value = ["/{adjudicationNumber}/outcome/not-proceed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createNotProceed(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody notProceedRequest: NotProceedRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.createNotProceed(
      adjudicationNumber = adjudicationNumber,
      details = notProceedRequest.details,
      reason = notProceedRequest.reason,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a prosecution outcome")
  @PostMapping(value = ["/{adjudicationNumber}/outcome/prosecution"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createProsecution(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.createProsecution(
      adjudicationNumber = adjudicationNumber,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a police refer outcome")
  @PostMapping(value = ["/{adjudicationNumber}/outcome/refer-police"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createRefPolice(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody policeReferralRequest: PoliceReferralRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.createReferral(
      adjudicationNumber = adjudicationNumber,
      code = OutcomeCode.REFER_POLICE,
      details = policeReferralRequest.details,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a dismissed from hearing outcome")
  @PostMapping(value = ["/{adjudicationNumber}/complete-hearing/dismissed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createDismissed(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody completedDismissedRequest: HearingCompletedDismissedRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = completedHearingService.createDismissed(
      adjudicationNumber = adjudicationNumber,
      adjudicator = completedDismissedRequest.adjudicator,
      plea = completedDismissedRequest.plea,
      details = completedDismissedRequest.details,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a not proceed from hearing outcome")
  @PostMapping(value = ["/{adjudicationNumber}/complete-hearing/not-proceed"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createNotProceedFromHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody completedNotProceedRequest: HearingCompletedNotProceedRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = completedHearingService.createNotProceed(
      adjudicationNumber = adjudicationNumber,
      adjudicator = completedNotProceedRequest.adjudicator,
      plea = completedNotProceedRequest.plea,
      reason = completedNotProceedRequest.reason,
      details = completedNotProceedRequest.details,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "create a charge proved from hearing outcome")
  @PostMapping(value = ["/{adjudicationNumber}/complete-hearing/charge-proved"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createChargeProvedFromHearing(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody chargeProvedRequest: HearingCompletedChargeProvedRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = completedHearingService.createChargeProved(
      adjudicationNumber = adjudicationNumber,
      adjudicator = chargeProvedRequest.adjudicator,
      plea = chargeProvedRequest.plea,
      amount = chargeProvedRequest.amount,
      caution = chargeProvedRequest.caution,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "remove a referral")
  @DeleteMapping(value = ["/{adjudicationNumber}/remove-referral"])
  @ResponseStatus(HttpStatus.OK)
  fun removeReferral(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = referralService.removeReferral(
      adjudicationNumber = adjudicationNumber,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "remove a not proceed without a referral or hearing")
  @DeleteMapping(value = ["/{adjudicationNumber}/outcome/not-proceed"])
  @ResponseStatus(HttpStatus.OK)
  fun removeNotProceedWithoutReferral(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "remove a completed hearing outcome")
  @DeleteMapping(value = ["/{adjudicationNumber}/remove-completed-hearing"])
  @ResponseStatus(HttpStatus.OK)
  fun removeCompletedHearingOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = completedHearingService.removeOutcome(
      adjudicationNumber = adjudicationNumber,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
