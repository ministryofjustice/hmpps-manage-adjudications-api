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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.AmendHearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.CompletedHearingService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingOutcomeService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReferralService

@Schema(description = "Request to create a referral for latest hearing")
data class ReferralRequest(
  @Schema(description = "the name of the adjudicator")
  val adjudicator: String,
  @Schema(description = "the outcome code")
  val code: HearingOutcomeCode,
  @Schema(description = "optional refer back to gov reason")
  val referGovReason: ReferGovReason? = null,
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
  val adjournReason: HearingOutcomeAdjournReason? = null,
  @Schema(description = "not proceed reason")
  val notProceedReason: NotProceedReason? = null,
  @Schema(description = "refer back to gov reason")
  val referGovReason: ReferGovReason? = null,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "plea")
  val plea: HearingOutcomePlea? = null,
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

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "26. Hearing outcomes")
class HearingOutcomeController(
  private val hearingOutcomeService: HearingOutcomeService,
  private val referralService: ReferralService,
  private val amendHearingOutcomeService: AmendHearingOutcomeService,
  private val completedHearingService: CompletedHearingService,
) : ReportedAdjudicationBaseController() {

  @Operation(
    summary = "create a referral for latest hearing",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Referral Created",
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
  @PostMapping(value = ["/{chargeNumber}/hearing/outcome/referral"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createReferral(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody referralRequest: ReferralRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_REFERRAL_CREATED },
        ),
      ),
      controllerAction = {
        referralService.createReferral(
          chargeNumber = chargeNumber,
          code = referralRequest.code.validateReferral(),
          adjudicator = referralRequest.adjudicator,
          details = referralRequest.details,
          referGovReason = referralRequest.referGovReason,
        )
      },
    )

  @Operation(summary = "remove a referral")
  @DeleteMapping(value = ["/{chargeNumber}/remove-referral"])
  @ResponseStatus(HttpStatus.OK)
  fun removeReferral(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      controllerAction = {
        referralService.removeReferral(
          chargeNumber = chargeNumber,
        )
      },
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = {
            when (it.status) {
              ReportedAdjudicationStatus.REFER_POLICE, ReportedAdjudicationStatus.REFER_GOV, ReportedAdjudicationStatus.REFER_INAD -> AdjudicationDomainEventType.REFERRAL_OUTCOME_DELETED
              else -> if (it.hearingIdActioned != null) AdjudicationDomainEventType.HEARING_REFERRAL_DELETED else AdjudicationDomainEventType.REFERRAL_DELETED
            }
          },
        ),
      ),
    )

  @Operation(
    summary = "create a adjourn for latest hearing",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Adjourn Created",
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
  @PostMapping(value = ["/{chargeNumber}/hearing/outcome/adjourn"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createAdjourn(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody adjournRequest: AdjournRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_ADJOURN_CREATED },
        ),
      ),
      controllerAction = {
        hearingOutcomeService.createAdjourn(
          chargeNumber = chargeNumber,
          adjudicator = adjournRequest.adjudicator,
          details = adjournRequest.details,
          adjournReason = adjournRequest.reason,
          plea = adjournRequest.plea,
        )
      },
    )

  @DeleteMapping(value = ["/{chargeNumber}/hearing/outcome/adjourn"])
  @Operation(summary = "removes the adjourn outcome")
  @ResponseStatus(HttpStatus.OK)
  fun removeAdjourn(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_ADJOURN_DELETED },
        ),
      ),
      controllerAction = {
        hearingOutcomeService.removeAdjourn(chargeNumber = chargeNumber)
      },
    )

  @Operation(summary = "amends a hearing outcome and associated outcome")
  @PutMapping(value = ["/{chargeNumber}/hearing/outcome/{status}/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun amendHearingOutcomeV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @PathVariable(name = "status") status: ReportedAdjudicationStatus,
    @RequestBody amendHearingOutcomeRequest: AmendHearingOutcomeRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_OUTCOME_UPDATED },
        ),
        EventRuleAndSupplier(
          eventRule = { it.punishmentsRemoved },
          eventSupplier = { AdjudicationDomainEventType.PUNISHMENTS_DELETED },
        ),
      ),
      controllerAction = {
        amendHearingOutcomeService.amendHearingOutcome(
          chargeNumber = chargeNumber,
          status = status,
          amendHearingOutcomeRequest = amendHearingOutcomeRequest,
        )
      },
    )

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
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_COMPLETED_CREATED },
        ),
      ),
      controllerAction = {
        completedHearingService.createDismissed(
          chargeNumber = chargeNumber,
          adjudicator = completedDismissedRequest.adjudicator,
          plea = completedDismissedRequest.plea,
          details = completedDismissedRequest.details,
        )
      },
    )

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
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_COMPLETED_CREATED },
        ),
      ),
      controllerAction = {
        completedHearingService.createNotProceed(
          chargeNumber = chargeNumber,
          adjudicator = completedNotProceedRequest.adjudicator,
          plea = completedNotProceedRequest.plea,
          notProceedReason = completedNotProceedRequest.reason,
          details = completedNotProceedRequest.details,
        )
      },
    )

  @PostMapping(value = ["/{chargeNumber}/complete-hearing/charge-proved/v2"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createChargeProvedFromHearingV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody chargeProvedRequest: HearingCompletedChargeProvedRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.HEARING_COMPLETED_CREATED },
        ),
      ),
      controllerAction = {
        completedHearingService.createChargeProved(
          chargeNumber = chargeNumber,
          adjudicator = chargeProvedRequest.adjudicator,
          plea = chargeProvedRequest.plea,
        )
      },
    )

  @Operation(summary = "remove a completed hearing outcome")
  @DeleteMapping(value = ["/{chargeNumber}/remove-completed-hearing"])
  @ResponseStatus(HttpStatus.OK)
  fun removeCompletedHearingOutcome(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.HEARING_COMPLETED_DELETED },
      ),
      EventRuleAndSupplier(
        eventRule = { it.punishmentsRemoved },
        eventSupplier = { AdjudicationDomainEventType.PUNISHMENTS_DELETED },
      ),
    ),
    controllerAction = {
      completedHearingService.removeOutcome(
        chargeNumber = chargeNumber,
      )
    },
  )
}
