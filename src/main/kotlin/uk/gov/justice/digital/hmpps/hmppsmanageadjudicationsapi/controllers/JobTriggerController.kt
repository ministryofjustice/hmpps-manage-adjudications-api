package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.job.FixLocationsJob
import java.time.Clock

@RestController
// @ProtectedByIngress
@RequestMapping("/job", produces = [MediaType.TEXT_PLAIN_VALUE])
class JobTriggerController(
  private val fixLocationsJob: FixLocationsJob,
  private val clock: Clock,
) {

  @PostMapping(value = ["/adjudications-fix-locations"])
  @Operation(
    summary = "Trigger the job to fix locations uuids",
    description = "",
  )
  @ResponseBody
  @ResponseStatus(HttpStatus.CREATED)
  fun triggerFixLocationsJob(): String {
    fixLocationsJob.execute()
    return "Fix locations data"
  }
}
