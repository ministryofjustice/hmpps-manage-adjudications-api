package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotCompletedOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
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
  @Schema(description = "duration of punishment")
  val duration: Int? = null,
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
  @Schema(description = "payback punishment notes")
  val paybackNotes: String? = null,
  @Schema(description = "optional rehabilitative activities associated to suspended punishment")
  val rehabilitativeActivities: List<RehabilitativeActivityRequest> = emptyList(),
)

@Schema(description = "rehabilitative activity")
data class RehabilitativeActivityRequest(
  @Schema(description = "id")
  val id: Long? = null,
  @Schema(description = "details")
  val details: String? = null,
  @Schema(description = "who is monitoring it")
  val monitor: String? = null,
  @Schema(description = "end date")
  val endDate: LocalDate? = null,
  @Schema(description = "optional number of sessions")
  val totalSessions: Int? = null,
)

@Schema(description = "complete a rehabilitative activity")
data class CompleteRehabilitativeActivityRequest(
  @Schema(description = "did they complete it")
  val completed: Boolean,
  @Schema(description = "outcome if they did not complete it")
  val outcome: NotCompletedOutcome? = null,
  @Schema(description = "optional days to activate it for")
  val daysToActivate: Int? = null,
  @Schema(description = "optional new suspended until date")
  val suspendedUntil: LocalDate? = null,
)

@RestController
@Tag(name = "30. Punishments")
class PunishmentsController(
  private val punishmentsService: PunishmentsService,
) : ReportedAdjudicationBaseController() {

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @PostMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.CREATED)
  fun createV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.PUNISHMENTS_CREATED },
      ),
    ),
    controllerAction = {
      punishmentsService.create(
        chargeNumber = chargeNumber,
        punishments = punishmentsRequest.punishments,
      )
    },
  )

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @Operation(summary = "updates a set of punishments")
  @PutMapping(value = ["/{chargeNumber}/punishments/v2"])
  @ResponseStatus(HttpStatus.OK)
  fun updateV2(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @RequestBody punishmentsRequest: PunishmentsRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.PUNISHMENTS_UPDATED },
      ),
    ),
    controllerAction = {
      punishmentsService.update(
        chargeNumber = chargeNumber,
        punishments = punishmentsRequest.punishments,
      )
    },
  )

  @PreAuthorize("hasRole('ADJUDICATIONS_REVIEWER') and hasAuthority('SCOPE_write')")
  @Operation(summary = "completes a rehabilitative punishment's activities")
  @PostMapping(value = ["/{chargeNumber}/punishments/{punishmentId}/complete-rehabilitative-activity"])
  @ResponseStatus(HttpStatus.OK)
  fun completeRehabilitativeActivity(
    @PathVariable(name = "chargeNumber") chargeNumber: String,
    @PathVariable(name = "punishmentId") punishmentId: Long,
    @RequestBody completeRehabilitativeActivityRequest: CompleteRehabilitativeActivityRequest,
  ): ReportedAdjudicationResponse = eventPublishWrapper(
    events = listOf(
      EventRuleAndSupplier(
        eventSupplier = { AdjudicationDomainEventType.PUNISHMENTS_UPDATED },
      ),
    ),
    controllerAction = {
      punishmentsService.completeRehabilitativeActivity(
        chargeNumber = chargeNumber,
        punishmentId = punishmentId,
        completeRehabilitativeActivityRequest = completeRehabilitativeActivityRequest,
      )
    },
  )
}
