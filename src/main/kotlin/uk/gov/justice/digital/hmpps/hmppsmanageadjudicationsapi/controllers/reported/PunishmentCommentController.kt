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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentCommentService

@Schema(description = "punishment comment request")
data class PunishmentCommentRequest(
  @Schema(description = "id of punishment comment")
  val id: Long? = null,
  @Schema(description = "punishment comment")
  val comment: String,
  @Schema(description = "punishment reason for change")
  val reasonForChange: ReasonForChange? = null,
)

@RestController
@Tag(name = "31. Punishments comments")
class PunishmentCommentController(
  private val punishmentCommentService: PunishmentCommentService,
) : ReportedAdjudicationBaseController() {
  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
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
    val reportedAdjudication = punishmentCommentService.createPunishmentComment(
      chargeNumber = chargeNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
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
    val reportedAdjudication = punishmentCommentService.updatePunishmentComment(
      chargeNumber = chargeNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
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
    val reportedAdjudication = punishmentCommentService.deletePunishmentComment(
      chargeNumber = chargeNumber,
      punishmentCommentId = punishmentCommentId,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
