package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.SummaryAdjudicationService
import java.time.LocalDate

@Schema(description = "Has Adjudications Response")
data class HasAdjudicationsResponse(
  @Schema(description = "flag to indicate the booking id has adjudications")
  val hasAdjudications: Boolean,
)

@PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
@RestController
@Tag(name = "01. Adjudication Summary")
@RequestMapping("/adjudications")
@Validated
class AdjudicationSummaryController(
  private val summaryAdjudicationService: SummaryAdjudicationService,
) {

  @Operation(
    summary = "Offender adjudications summary (awards and sanctions).",
    description = "Offender adjudications (awards and sanctions).",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjudication returned",
      ),
    ],
  )
  @GetMapping("/by-booking-id/{bookingId}")
  fun getAdjudicationSummary(
    @PathVariable("bookingId")
    @Parameter(
      description = "The prisoner booking id",
      required = true,
    )
    bookingId: Long,
    @RequestParam(
      value = "awardCutoffDate",
      required = false,
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Only awards ending on or after this date (in YYYY-MM-DD format) will be considered.")
    awardCutoffDate: LocalDate?,
    @RequestParam(
      value = "adjudicationCutoffDate",
      required = false,
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Only proved adjudications ending on or after this date (in YYYY-MM-DD format) will be counted.")
    adjudicationCutoffDate: LocalDate?,
  ): AdjudicationSummary = summaryAdjudicationService.getAdjudicationSummary(
    bookingId,
    awardCutoffDate,
    adjudicationCutoffDate,
  )

  @Operation(
    summary = "Does the booking id have any adjudications",
    description = "Does the booking id have any adjudications",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Has adjudications",
      ),
    ],
  )
  @GetMapping("/booking/{bookingId}/exists")
  fun hasAdjudications(
    @PathVariable("bookingId")
    @Parameter(
      description = "The prisoner booking id",
      required = true,
    )
    bookingId: Long,
  ): HasAdjudicationsResponse = summaryAdjudicationService.hasAdjudications(bookingId)
}
