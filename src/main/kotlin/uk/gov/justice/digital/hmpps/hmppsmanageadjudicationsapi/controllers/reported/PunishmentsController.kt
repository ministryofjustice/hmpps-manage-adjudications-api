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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService
import java.time.LocalDate

@Deprecated("to remove on completion of NN-5319")
@Schema(description = "punishments request")
data class PunishmentsRequest(
  @Schema(description = "list of punishments")
  val punishments: List<PunishmentRequest>,
)

@Schema(description = "punishments request")
data class PunishmentsRequestV2(
  @Schema(description = "list of punishments")
  val punishments: List<PunishmentRequestV2>,
)

@Deprecated("to remove on completion of NN-5319")
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
  val days: Int,
  @Schema(description = "punishment start date, required if punishment is not suspended")
  val startDate: LocalDate? = null,
  @Schema(description = "punishment end date, required if punishment is not suspended")
  val endDate: LocalDate? = null,
  @Schema(description = "punishment suspended until date, required if punishment is suspended")
  val suspendedUntil: LocalDate? = null,
  @Schema(description = "optional activated from report number")
  val activatedFrom: String? = null,
  @Schema(description = "optional consecutive report number")
  val consecutiveReportNumber: String? = null,
)

@Schema(description = "punishment request")
data class PunishmentRequestV2(
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
  @Schema(description = "optional consecutive report number")
  val consecutiveReportNumber: String? = null,
  @Schema(description = "optional amount - money being recovered for damages - only use if type is DAMAGED_OWED")
  val damagesOwedAmount: Double? = null,
)

@Schema(description = "punishment comment request")
data class PunishmentCommentRequest(
  @Schema(description = "id of punishment comment")
  val id: Long? = null,
  @Schema(description = "punishment comment")
  val comment: String,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "30. Punishments")
class PunishmentsController(
  private val punishmentsService: PunishmentsService,
) : ReportedAdjudicationBaseController() {

  @Deprecated("to remove on completion of NN-5319")
  @Operation(
    summary = "create a set of punishments",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Punishment created",
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
  @PostMapping(value = ["/{chargeNumber}/punishments"])
  @ResponseStatus(HttpStatus.CREATED)
  fun create(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse =
    punishmentsService.create(
      chargeNumber = chargeNumber,
      punishments = punishmentsRequest.punishments,
    ).toResponse()

  @PostMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequestV2,
  ): ReportedAdjudicationResponse = punishmentsService.createV2(
    chargeNumber = chargeNumber,
    punishments = punishmentsRequest.punishments,
  ).toResponse()

  @Deprecated("to remove on completion of NN-5319")
  @Operation(summary = "updates a set of punishments")
  @PutMapping(value = ["/{chargeNumber}/punishments"])
  @ResponseStatus(HttpStatus.OK)
  fun update(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.update(
      chargeNumber = chargeNumber,
      punishments = punishmentsRequest.punishments,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "updates a set of punishments")
  @PutMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun updateV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequestV2,
  ): ReportedAdjudicationResponse = punishmentsService.updateV2(
    chargeNumber = chargeNumber,
    punishments = punishmentsRequest.punishments,
  ).toResponse()

  @Operation(summary = "get a list of suspended punishments by prisoner")
  @GetMapping(value = ["/punishments/{prisonerNumber}/suspended"])
  @ResponseStatus(HttpStatus.OK)
  fun getSuspendedPunishments(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "reportNumber") reportNumber: String,
  ): List<SuspendedPunishmentDto> =
    punishmentsService.getSuspendedPunishments(prisonerNumber = prisonerNumber, chargeNumber = reportNumber)

  @Operation(summary = "get a list of active additional days reports by prisoner for a consecutive punishment")
  @GetMapping(value = ["/punishments/{prisonerNumber}/for-consecutive"])
  @ResponseStatus(HttpStatus.OK)
  fun getActiveAdditionalDaysReports(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "type") punishmentType: PunishmentType,
  ): List<AdditionalDaysDto> =
    punishmentsService.getReportsWithAdditionalDays(prisonerNumber = prisonerNumber, punishmentType = punishmentType)

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
