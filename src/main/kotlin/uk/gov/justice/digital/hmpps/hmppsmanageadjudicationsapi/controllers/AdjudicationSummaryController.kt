package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.NotNull
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ProvenAdjudicationsSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.SummaryAdjudicationService
import java.time.LocalDate

@PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
@RestController
@Tag(name = "01. Adjudication Summary")
@RequestMapping("/adjudications")
@Validated
class AdjudicationSummaryController(
  private val summaryAdjudicationService: SummaryAdjudicationService,
) {

  @Operation(
    summary = "Return a specific adjudication for a prisoner reference by charge ID",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Adjudication returned",
      ),
    ],
  )
  @GetMapping("/{prisonerNumber}/charge/{chargeId}")
  fun getAdjudication(
    @PathVariable("prisonerNumber")
    @Parameter(
      description = "prisonerNumber",
      required = true,
      example = "A1234AA",
    )
    prisonerNumber: @NotNull String,
    @PathVariable("chargeId")
    @Parameter(
      description = "chargeId",
      required = true,
    )
    chargeId: Long,
  ): AdjudicationDetail {
    return summaryAdjudicationService.getAdjudication(prisonerNumber, chargeId)
  }

  @Operation(
    summary = "Proven adjudications summary count.",
    description = "Need VIEW_ADJUDICATIONS role",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Proven adjudication summary count returned",
      ),
    ],
  )
  @PostMapping("/proven-adjudications")
  fun getProvenAdjudicationCounts(
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
    @NotNull @RequestBody
    bookingIds: List<Long>,
  ): List<ProvenAdjudicationsSummary> {
    return summaryAdjudicationService.getProvenAdjudicationsForBookings(
      bookingIds,
      awardCutoffDate,
      adjudicationCutoffDate,
    )
  }

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
  ): AdjudicationSummary {
    return summaryAdjudicationService.getAdjudicationSummary(
      bookingId,
      awardCutoffDate,
      adjudicationCutoffDate,
    )
  }

  @Operation(
    summary = "Return a list of adjudications for a given prisoner",
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Adjudications returned",
      ),
    ],
  )
  @GetMapping("/{prisonerNumber}/adjudications")
  fun getAdjudicationsByPrisonerNumber(
    @PathVariable("prisonerNumber")
    @Parameter(
      description = "prisonerNumber",
      required = true,
      example = "A1234AA",
    )
    prisonerNumber: String,
    @RequestParam(
      value = "offenceId",
      required = false,
    )
    @Parameter(description = "An offence id to allow optionally filtering by type of offence")
    offenceId: Long?,
    @RequestParam(
      value = "prisonId",
      required = false,
    )
    @Parameter(description = "An agency id to allow optionally filtering by the prison in which the offence occurred")
    prisonId: String?,
    @RequestParam(
      value = "finding",
      required = false,
    )
    @Parameter(
      description = "Finding code to allow optionally filtering by type of finding",
      example = "NOT_PROVEN",
    )
    finding: Finding?,
    @RequestParam(
      value = "fromDate",
      required = false,
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Adjudications must have been reported on or after this date (in YYYY-MM-DD format).")
    fromDate: LocalDate?,
    @RequestParam(
      value = "toDate",
      required = false,
    )
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Parameter(description = "Adjudications must have been reported on or before this date (in YYYY-MM-DD format).")
    toDate: LocalDate?,
    @ParameterObject
    @PageableDefault(size = 20)
    pageable: Pageable,
  ): AdjudicationSearchResponse {
    return summaryAdjudicationService.getAdjudications(
      prisonerNumber = prisonerNumber,
      offenceId = offenceId,
      prisonId = prisonId,
      finding = finding,
      fromDate = fromDate,
      toDate = toDate,
      pageable = pageable,
    )
  }
}
