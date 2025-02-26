package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService

@Schema(description = "Reported adjudication response")
data class ReportedAdjudicationResponse(
  @Schema(description = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto,
)

class EventRuleAndSupplier(
  val eventRule: (ReportedAdjudicationDto) -> Boolean = { _ -> true },
  val eventSupplier: (ReportedAdjudicationDto) -> AdjudicationDomainEventType,
)

@RestController
@RequestMapping("/reported-adjudications")
class ReportedAdjudicationBaseController {

  @Autowired
  private lateinit var eventPublishService: EventPublishService

  fun eventPublishWrapper(
    controllerAction: () -> ReportedAdjudicationDto,
    events: List<EventRuleAndSupplier> = emptyList(),
  ): ReportedAdjudicationResponse = controllerAction.invoke().also {
    events.forEach { event ->
      if (event.eventRule.invoke(it)) eventPublishService.publishEvent(
        event.eventSupplier.invoke(
          it,
        ),
        it,
      )
    }
  }.toResponse()

  companion object {
    fun ReportedAdjudicationDto.toResponse() = ReportedAdjudicationResponse(this)
  }
}
