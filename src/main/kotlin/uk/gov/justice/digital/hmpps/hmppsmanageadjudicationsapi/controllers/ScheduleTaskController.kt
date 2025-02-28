package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService

@RestController
@Hidden
@Tag(name = "50. Schedule Tasks")
@RequestMapping("/scheduled-tasks")
class ScheduleTaskController(
  private val draftAdjudicationService: DraftAdjudicationService,
) {

  @DeleteMapping(value = ["/delete-orphaned-draft-adjudications"])
  fun deleteOrphanedDraftAdjudications(): Unit =
    draftAdjudicationService.deleteOrphanedDraftAdjudications()
}
