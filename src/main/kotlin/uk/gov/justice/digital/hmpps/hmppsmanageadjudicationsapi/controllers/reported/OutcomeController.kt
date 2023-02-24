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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@Schema(description = "Request to add an outcome")
data class OutcomeRequest(
  @Schema(description = "outcome code")
  val code: OutcomeCode,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "not proceeded with reason")
  val reason: NotProceedReason? = null
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class OutcomeController(
  private val outcomeService: OutcomeService,
  private val referralService: ReferralService,
) : ReportedAdjudicationBaseController() {

  @Operation(summary = "create a not proceed, refer police or prosecution outcome")
  @PostMapping(value = ["/{adjudicationNumber}/outcome"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createOutcome(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody outcomeRequest: OutcomeRequest
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.createOutcome(
      adjudicationNumber = adjudicationNumber,
      code = outcomeRequest.code,
      details = outcomeRequest.details,
      reason = outcomeRequest.reason,
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
  @DeleteMapping(value = ["/{adjudicationNumber}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun removeNotProceedWithoutReferral(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = outcomeService.deleteOutcome(
      adjudicationNumber = adjudicationNumber,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
