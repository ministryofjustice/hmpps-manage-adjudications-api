package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported

import io.swagger.v3.oas.annotations.Hidden
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.NomisHearingOutcomeService

@RestController
@Hidden
@Tag(name = "50. Schedule Tasks")
@RequestMapping("/scheduled-tasks")
class ScheduleTaskController(
  private val nomisHearingOutcomeService: NomisHearingOutcomeService,
  private val draftAdjudicationService: DraftAdjudicationService,
) {

  @PutMapping("/check-nomis-created-hearing-outcomes-for-locking")
  @Operation(summary = "Checking in NOMIS if any hearing outcomes have been added and locks the adjuication record in this service, to prevent data corruption", hidden = true)
  @ResponseStatus(HttpStatus.OK)
  fun lockIfNomisCreatedHearingOutcomes(): String {
    nomisHearingOutcomeService.checkForNomisHearingOutcomesAndUpdate()
    return "OK"
  }

  @DeleteMapping(value = ["/delete-orphaned-draft-adjudications"])
  fun deleteOrphanedDraftAdjudications(): Unit =
    draftAdjudicationService.deleteOrphanedDraftAdjudications()
}
