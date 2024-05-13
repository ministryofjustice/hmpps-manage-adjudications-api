package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdditionalDaysDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportService
import java.time.LocalDate

@Schema(description = "active punishment dto")
data class ActivePunishmentDto(
  @Schema(description = "charge number")
  val chargeNumber: String,
  @Schema(description = "punishment type")
  val punishmentType: PunishmentType,
  @Schema(description = "privilege type")
  val privilegeType: PrivilegeType? = null,
  @Schema(description = "other privilege description")
  val otherPrivilege: String? = null,
  @Schema(description = "days applied")
  val days: Int? = null,
  @Schema(description = "duration of punishment")
  val duration: Int? = null,
  @Schema(description = "measurement of duration")
  val measurement: Measurement? = null,
  @Schema(description = "start date")
  val startDate: LocalDate? = null,
  @Schema(description = "last day")
  val lastDay: LocalDate? = null,
  @Schema(description = "amount")
  val amount: Double? = null,
  @Schema(description = "stoppage percentage")
  val stoppagePercentage: Int? = null,
  @Schema(description = "activated from report")
  val activatedFrom: String? = null,
)

@RestController
@Tag(name = "32. Punishments reports")
class PunishmentsReportController(
  private val punishmentsReportService: PunishmentsReportService,
) : ReportedAdjudicationBaseController() {

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @Operation(summary = "get a list of suspended punishments by prisoner")
  @GetMapping(value = ["/punishments/{prisonerNumber}/suspended/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun getSuspendedPunishments(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "chargeNumber") chargeNumber: String,
  ): List<SuspendedPunishmentDto> =
    punishmentsReportService.getSuspendedPunishments(prisonerNumber = prisonerNumber, chargeNumber = chargeNumber)

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @Operation(summary = "get a list of active additional days reports by prisoner for a consecutive punishment")
  @GetMapping(value = ["/punishments/{prisonerNumber}/for-consecutive"])
  @ResponseStatus(HttpStatus.OK)
  fun getActiveAdditionalDaysReports(
    @PathVariable(name = "prisonerNumber") prisonerNumber: String,
    @RequestParam(name = "type") punishmentType: PunishmentType,
    @RequestParam(name = "chargeNumber") chargeNumber: String,
  ): List<AdditionalDaysDto> =
    punishmentsReportService.getReportsWithAdditionalDays(
      chargeNumber = chargeNumber,
      prisonerNumber = prisonerNumber,
      punishmentType = punishmentType,
    )

  @PreAuthorize("hasRole('VIEW_ADJUDICATIONS')")
  @Operation(summary = "get a list of active punishments by offenderBookingId")
  @GetMapping(value = ["/punishments/{offenderBookingId}/active"])
  @ResponseStatus(HttpStatus.OK)
  fun getActivePunishments(
    @PathVariable(name = "offenderBookingId") offenderBookingId: Long,
  ): List<ActivePunishmentDto> =
    punishmentsReportService.getActivePunishments(offenderBookingId = offenderBookingId)
}
