package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
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
  val days: Int,
  @Schema(description = "punishment start date, required if punishment is not suspended")
  val startDate: LocalDate? = null,
  @Schema(description = "punishment end date, required if punishment is not suspended")
  val endDate: LocalDate? = null,
  @Schema(description = "punishment suspended until date, required if punishment is suspended")
  val suspendedUntil: LocalDate? = null,
  @Schema(description = "optional activated from report number")
  val activatedFrom: Long? = null,
)

@PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
@RestController
class PunishmentsController(
  private val punishmentsService: PunishmentsService,
) : ReportedAdjudicationBaseController() {

  @Operation(summary = "create a set of punishments")
  @PostMapping(value = ["/{adjudicationNumber}/punishments"])
  @ResponseStatus(HttpStatus.CREATED)
  fun create(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.create(
      adjudicationNumber = adjudicationNumber,
      punishments = punishmentsRequest.punishments,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "updates a set of punishments")
  @PutMapping(value = ["/{adjudicationNumber}/punishments"])
  @ResponseStatus(HttpStatus.OK)
  fun update(
    @PathVariable(name = "adjudicationNumber") adjudicationNumber: Long,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse {
    val reportedAdjudication = punishmentsService.update(
      adjudicationNumber = adjudicationNumber,
      punishments = punishmentsRequest.punishments,
    )

    return ReportedAdjudicationResponse(reportedAdjudication)
  }

  @Operation(summary = "get a list of suspended punishments by prisoner")
  @GetMapping(value = ["/punishments/{prisonerNumber}/suspended"])
  @ResponseStatus(HttpStatus.OK)
  fun getSuspendedPunishments(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
  ): List<SuspendedPunishmentDto> = punishmentsService.getSuspendedPunishments(prisonerNumber = prisonerNumber)
}
