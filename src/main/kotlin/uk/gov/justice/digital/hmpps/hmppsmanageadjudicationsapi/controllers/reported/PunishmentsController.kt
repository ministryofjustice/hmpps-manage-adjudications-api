package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService
import java.time.LocalDate

@Schema(description = "punishments request")
data class PunishmentsRequest(
  @Schema(description = "list of punishments")
  val punishments: List<PunishmentRequest>,
)

@Schema(description = "punishment request")
data class PunishmentRequest(
  @Schema(description = "id of punishment")
  val id: Long? = null,
  @Schema(description = "punishment type")
  val type: PunishmentType,
  @Schema(description = "privilege type - only use if punishment type is PRIVILEGE")
  val privilegeType: PrivilegeType? = null,
  @Schema(description = "other privilege type - only use if privilege type is OTHER")
  val otherPrivilege: String? = null,
  @Schema(description = "stoppage percentage - use if punishment type is EARNINGS")
  val stoppagePercentage: Int? = null,
  @Schema(description = "days punishment to last")
  val days: Int? = null,
  @Schema(description = "punishment start date, required if punishment is not suspended")
  val startDate: LocalDate? = null,
  @Schema(description = "punishment end date, required if punishment is not suspended")
  val endDate: LocalDate? = null,
  @Schema(description = "punishment suspended until date, required if punishment is suspended")
  val suspendedUntil: LocalDate? = null,
  @Schema(description = "optional activated from report number")
  val activatedFrom: String? = null,
  @Schema(description = "optional consecutive charge number")
  val consecutiveChargeNumber: String? = null,
  @Schema(description = "optional amount - money being recovered for damages - only use if type is DAMAGED_OWED")
  val damagesOwedAmount: Double? = null,
)

@Schema(description = "punishment comment request")
data class PunishmentCommentRequest(
  @Schema(description = "id of punishment comment")
  val id: Long? = null,
  @Schema(description = "punishment comment")
  val comment: String,
  @Schema(description = "punishment reason for change")
  val reasonForChange: ReasonForChange? = null,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "30. Punishments")
class PunishmentsController(
  private val punishmentsService: PunishmentsService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      event = AdjudicationDomainEventType.PUNISHMENTS_CREATED,
      controllerAction = {
        punishmentsService.create(
          chargeNumber = chargeNumber,
          punishments = punishmentsRequest.punishments,
        )
      },
    )

  @Operation(summary = "updates a set of punishments")
  @PutMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun updateV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(
      event = AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
      controllerAction = {
        punishmentsService.update(
          chargeNumber = chargeNumber,
          punishments = punishmentsRequest.punishments,
        )
      },
    )

  @Deprecated("to remove see v2")
  @Operation(summary = "get a list of suspended punishments by prisoner")
  @GetMapping(value = ["/punishments/{prisonerNumber}/suspended"])
  @ResponseStatus(HttpStatus.OK)
  fun getSuspendedPunishments(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "reportNumber") reportNumber: String,
  ): List<SuspendedPunishmentDto> =
    punishmentsService.getSuspendedPunishments(prisonerNumber = prisonerNumber, chargeNumber = reportNumber)

  @Operation(summary = "get a list of suspended punishments by prisoner")
  @GetMapping(value = ["/punishments/{prisonerNumber}/suspended/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun getSuspendedPunishmentsV2(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "chargeNumber") chargeNumber: String,
  ): List<SuspendedPunishmentDto> =
    punishmentsService.getSuspendedPunishments(prisonerNumber = prisonerNumber, chargeNumber = chargeNumber)

  @Operation(summary = "get a list of active additional days reports by prisoner for a consecutive punishment")
  @GetMapping(value = ["/punishments/{prisonerNumber}/for-consecutive"])
  @ResponseStatus(HttpStatus.OK)
  fun getActiveAdditionalDaysReports(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "type") punishmentType: PunishmentType,
    @RequestParam(name = "chargeNumber") chargeNumber: String,
  ): List<AdditionalDaysDto> =
    punishmentsService.getReportsWithAdditionalDays(
      chargeNumber = chargeNumber,
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    )

  @Operation(
    summary = "create punishment comment",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Punishment comment created",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{chargeNumber}/punishments/comment"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createPunishmentComment(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentCommentRequest: PunishmentCommentRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.createPunishmentComment(
      chargeNumber = chargeNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(
    summary = "update punishment comment",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Punishment comment updated",
      ),
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "415",
        description = "Not able to process the request because the payload is in a format not supported by this endpoint.",
        content = [
          io.swagger.v3.oas.annotations.media.Content(
            mediaType = "application/json",
            schema = Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PutMapping(value = ["/{chargeNumber}/punishments/comment"])
  @ResponseStatus(HttpStatus.OK)
  fun updatePunishmentComment(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentCommentRequest: PunishmentCommentRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.updatePunishmentComment(
      chargeNumber = chargeNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(
    summary = "delete punishment comment",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Punishment comment deleted",
      ),
    ],
  )
  @DeleteMapping(value = ["/{chargeNumber}/punishments/comment/{punishmentCommentId}"])
  @ResponseStatus(HttpStatus.OK)
  fun deletePunishmentComment(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @PathVariable(name = "punishmentCommentId") punishmentCommentId: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.deletePunishmentComment(
      chargeNumber = chargeNumber,
      punishmentCommentId = punishmentCommentId,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
