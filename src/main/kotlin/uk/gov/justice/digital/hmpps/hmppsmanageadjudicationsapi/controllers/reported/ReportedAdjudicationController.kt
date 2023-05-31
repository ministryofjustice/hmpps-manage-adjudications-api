package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService
import java.time.LocalDateTime

@Schema(description = "Request to set the state for an a reported adjudication")
data class ReportedAdjudicationStatusRequest(
  @Schema(description = "The status to set the reported adjudication to")
  val status: ReportedAdjudicationStatus,
  @Schema(description = "The reason the status has been set")
  @get:Size(
    max = 128,
    message = "The reason the status has been set exceeds the maximum character limit of {max}",
  )
  val statusReason: String? = null,
  @Schema(description = "Details of why the status has been set")
  @get:Size(
    max = 4000,
    message = "The details of why the status has been set exceeds the maximum character limit of {max}",
  )
  val statusDetails: String? = null,
)

@Schema(description = "Request to issue a DIS")
data class IssueRequest(
  @Schema(description = "The date time of issue", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfIssue: LocalDateTime,
)

@RestController
@Tag(name = "20. Adjudication Management")
class ReportedAdjudicationController(
  private val reportedAdjudicationService: ReportedAdjudicationService,
) : ReportedAdjudicationBaseController() {

  @GetMapping(value = ["/{adjudicationNumber}"])
  fun getReportedAdjudicationDetails(@PathVariable(name = "adjudicationNumber") adjudicationNumber: Long): ReportedAdjudicationResponse =
    reportedAdjudicationService.getReportedAdjudicationDetails(adjudicationNumber).toResponse()

  @PutMapping(value = ["/{adjudicationNumber}/status"])
  @Operation(summary = "Set the status for the reported adjudication.")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  @ResponseStatus(HttpStatus.OK)
  fun setStatus(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid
    reportedAdjudicationStatusRequest: ReportedAdjudicationStatusRequest,
  ): ReportedAdjudicationResponse =
    eventPublishWrapper(AdjudicationDomainEventType.ADJUDICATION_CREATED) {
      reportedAdjudicationService.setStatus(
        adjudicationNumber,
        reportedAdjudicationStatusRequest.status,
        reportedAdjudicationStatusRequest.statusReason,
        reportedAdjudicationStatusRequest.statusDetails,
      )
    }

  @PutMapping(value = ["/{adjudicationNumber}/issue"])
  @Operation(summary = "Issue DIS Form")
  @PreAuthorize("hasAuthority('SCOPE_write')")
  fun setIssued(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody @Valid
    issueRequest: IssueRequest,
  ): ReportedAdjudicationResponse =
    reportedAdjudicationService.setIssued(
      adjudicationNumber,
      issueRequest.dateTimeOfIssue,
    ).toResponse()
}
