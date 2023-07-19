package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDtoV2
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.AdjudicationDomainEventType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.EventPublishService

@Deprecated("to remove on completion of NN-5319")
@Schema(description = "Reported adjudication response")
data class ReportedAdjudicationResponse(
  @Schema(description = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDto,
)

@Schema(description = "Reported adjudication response")
data class ReportedAdjudicationResponseV2(
  @Schema(description = "The reported adjudication")
  val reportedAdjudication: ReportedAdjudicationDtoV2,
)

@RestController
@RequestMapping("/reported-adjudications")
class ReportedAdjudicationBaseController {

  @Autowired
  private lateinit var eventPublishService: EventPublishService

  fun eventPublishWrapper(
    event: AdjudicationDomainEventType,
    controllerAction: () -> ReportedAdjudicationDto,
    eventRule: (ReportedAdjudicationDto) -> Boolean = { _ -> true },
  ): ReportedAdjudicationResponse =
    controllerAction.invoke().also { if (eventRule.invoke(it)) eventPublishService.publishEvent(event, it) }.toResponse()

  companion object {
    @Deprecated("to remove on completion of NN-5319")
    fun ReportedAdjudicationDto.toResponse() = ReportedAdjudicationResponse(this)

    fun ReportedAdjudicationDtoV2.toResponse() = ReportedAdjudicationResponseV2(this)
  }
}
