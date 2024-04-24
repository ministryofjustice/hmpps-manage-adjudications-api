package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService

@Schema(description = "Request to add a police referral")
data class ReferralDetailsRequest(
  @Schema(description = "details")
  val details: String,
)

@Schema(description = "Request to add a gov referral")
data class ReferralGovRequest(
  @Schema(description = "refer back to gov reason")
  val referGovReason: ReferGovReason,
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
  @Schema(description = "refer back to gov reason")
  val referGovReason: ReferGovReason? = null,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "31. Outcomes")
class OutcomeController(
  @Value("\${service.punishments.version}") private val punishmentsVersion: Int,
  private val outcomeService: OutcomeService,
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
    eventPublishWrapper(
      controllerAction = {
        outcomeService.createNotProceed(
          chargeNumber = chargeNumber,
          details = notProceedRequest.details,
          notProceedReason = notProceedRequest.reason,
        )
      },
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = {
            when (it.outcomes.lastOrNull()?.outcome?.referralOutcome) {
              null -> AdjudicationDomainEventType.NOT_PROCEED_OUTCOME
              else -> AdjudicationDomainEventType.NOT_PROCEED_REFERRAL_OUTCOME
            }
          },
        ),
      ),

    )

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
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.PROSECUTION_REFERRAL_OUTCOME },
        ),
      ),
      controllerAction = {
        outcomeService.createProsecution(
          chargeNumber = chargeNumber,
        )
      },
    )

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
    @RequestBody referGovRequest: ReferralGovRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.REFERRAL_OUTCOME_REFER_GOV },
        ),
      ),
      controllerAction = {
        outcomeService.createReferGov(
          chargeNumber = chargeNumber,
          details = referGovRequest.details,
          referGovReason = referGovRequest.referGovReason,
        )
      },
    )

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
  ): ReportedAdjudicationResponse = when (punishmentsVersion) {
    1 -> eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.QUASHED },
        ),
      ),
      controllerAction = {
        outcomeService.createQuashed(
          chargeNumber = chargeNumber,
          reason = quashedRequest.reason,
          details = quashedRequest.details,
        )
      },
    )

    else -> TODO("implement me")
  }

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
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.REF_POLICE_OUTCOME },
        ),
      ),
      controllerAction = {
        outcomeService.createReferral(
          chargeNumber = chargeNumber,
          code = OutcomeCode.REFER_POLICE,
          details = policeReferralRequest.details,
        )
      },
    )

  @Operation(summary = "remove a not proceed without a referral outcome, or a quashed outcome")
  @DeleteMapping(value = ["/{chargeNumber}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun removeNotProceedWithoutReferralOrQuashed(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse = when (punishmentsVersion) {
    1 -> eventPublishWrapper(
      controllerAction = {
        outcomeService.deleteOutcome(
          chargeNumber = chargeNumber,
        )
      },
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = {
            when (it.status) {
              ReportedAdjudicationStatus.CHARGE_PROVED -> AdjudicationDomainEventType.UNQUASHED
              else -> AdjudicationDomainEventType.NOT_PROCEED_OUTCOME_DELETED
            }
          },
        ),
      ),
    )

    else -> TODO("implement me")
  }

  @Operation(summary = "amend outcome without a hearing (refer police, not proceed or quashed), unless its a referral outcome from next steps")
  @PutMapping(value = ["/{chargeNumber}/outcome"])
  @ResponseStatus(HttpStatus.OK)
  fun amendOutcome(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody amendOutcomeRequest: AmendOutcomeRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      events = listOf(
        EventRuleAndSupplier(
          eventSupplier = { AdjudicationDomainEventType.OUTCOME_UPDATED },
        ),
      ),
      controllerAction = {
        outcomeService.amendOutcomeViaApi(
          chargeNumber = chargeNumber,
          details = amendOutcomeRequest.details,
          reason = amendOutcomeRequest.reason,
          quashedReason = amendOutcomeRequest.quashedReason,
          referGovReason = amendOutcomeRequest.referGovReason,
        )
      },
    )
}
