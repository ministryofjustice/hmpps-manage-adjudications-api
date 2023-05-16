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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentCommentService

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
class PunishmentCommentController(
  private val punishmentCommentService: PunishmentCommentService,
) : ReportedAdjudicationBaseController() {

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
            schema = io.swagger.v3.oas.annotations.media.Schema(implementation = ErrorResponse::class),
          ),
        ],
      ),
    ],
  )
  @PostMapping(value = ["/{adjudicationNumber}/punishments/comment"])
  @ResponseStatus(HttpStatus.CREATED)
  fun create(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody punishmentCommentRequest: PunishmentCommentRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentCommentService.create(
      adjudicationNumber = adjudicationNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "update punishment comment")
  @PutMapping(value = ["/{adjudicationNumber}/punishments/comment"])
  @ResponseStatus(HttpStatus.OK)
  fun update(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody punishmentCommentRequest: PunishmentCommentRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentCommentService.update(
      adjudicationNumber = adjudicationNumber,
      punishmentComment = punishmentCommentRequest,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "delete punishment comment")
  @DeleteMapping(value = ["/{adjudicationNumber}/punishments/comment{punishmentCommentId}"])
  @ResponseStatus(HttpStatus.OK)
  fun delete(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @PathVariable(name = "punishmentCommentId") punishmentCommentId: Long,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentCommentService.delete(
      adjudicationNumber = adjudicationNumber,
      punishmentCommentId = punishmentCommentId,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }
}
