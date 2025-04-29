package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingSummaryDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Schema(description = "All hearings response")
data class HearingSummaryResponse(
  @Schema(description = "The hearing summaries response")
  val hearings: List<HearingSummaryDto>,
)

@Schema(description = "Hearing and prisoner")
data class HearingAndPrisoner(
  @Schema(description = "prisoner number")
  val prisonerNumber: String,
  @Schema(description = "hearing")
  val hearing: HearingDto,
)

@Schema(description = "Request to add a hearing")
data class HearingRequest(
  @Schema(description = "The id of the location of the hearing", deprecated = true)
  val locationId: Long? = null,
  @Schema(description = "The uuid of the location of the hearing")
  val locationUuid: UUID,
  @Schema(description = "Date and time of the hearing", example = "2010-10-12T10:00:00")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  val dateTimeOfHearing: LocalDateTime,
  @Schema(description = "oic hearing type")
  val oicHearingType: OicHearingType,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
@Tag(name = "24. Hearings")
class HearingController(
  private val hearingService: HearingService,
) : ReportedAdjudicationBaseController() {

  @PostMapping(value = ["/{chargeNumber}/hearing/v2"])
  @Operation(
    summary = "Create a new hearing",
    responses = [
      io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "201",
        description = "Hearing created",
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
  @ResponseStatus(HttpStatus.CREATED)
  fun createHearing(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody hearingRequest: HearingRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.HEARING_CREATED },
      ),
    ),
    controllerAction = {
      hearingService.createHearing(
        chargeNumber = chargeNumber,
        locationId = hearingRequest.locationId,
        locationUuid = hearingRequest.locationUuid,
        dateTimeOfHearing = hearingRequest.dateTimeOfHearing,
        oicHearingType = hearingRequest.oicHearingType,
      )
    },
  )

  @PutMapping(value = ["/{chargeNumber}/hearing/v2"])
  @Operation(summary = "Amends latest hearing")
  @ResponseStatus(HttpStatus.OK)
  fun amendHearing(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody hearingRequest: HearingRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.HEARING_UPDATED },
      ),
    ),
    controllerAction = {
      hearingService.amendHearing(
        chargeNumber = chargeNumber,
        locationId = hearingRequest.locationId,
        locationUuid = hearingRequest.locationUuid,
        dateTimeOfHearing = hearingRequest.dateTimeOfHearing,
        oicHearingType = hearingRequest.oicHearingType,
      )
    },
  )

  @DeleteMapping(value = ["/{chargeNumber}/hearing/v2"])
  @Operation(summary = "deletes latest hearing")
  @ResponseStatus(HttpStatus.OK)
  fun deleteHearing(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.HEARING_DELETED },
      ),
    ),
    controllerAction = {
      hearingService.deleteHearing(chargeNumber = chargeNumber)
    },
  )

  @Operation(summary = "Get a list of hearings for a given date and agency")
  @GetMapping(value = ["/hearings"])
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  fun getAllHearingsByAgencyAndDate(
    @RequestParam(name = "hearingDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    hearingDate: LocalDate,
  ): HearingSummaryResponse {
    val hearings = hearingService.getAllHearingsByAgencyIdAndDate(dateOfHearing = hearingDate)

    return HearingSummaryResponse(
      hearings,
    )
  }

  @Operation(summary = "Get a list of hearings by prisoner for an agency")
  @PostMapping(value = ["/hearings/{agencyId}"])
  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  fun getHearingsByPrisoner(
    @PathVariable("agencyId")
    agencyId: String,
    @RequestParam(name = "startDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    startDate: LocalDate,
    @RequestParam(name = "endDate")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    endDate: LocalDate,
    @RequestBody prisoners: List<String>,
  ): List<HearingAndPrisoner> = hearingService.getHearingsByPrisoner(
    agencyId = agencyId,
    startDate = startDate,
    endDate = endDate,
    prisoners = prisoners,
  )
}
