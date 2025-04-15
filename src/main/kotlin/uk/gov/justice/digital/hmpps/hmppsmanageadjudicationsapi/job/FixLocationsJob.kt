package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.FixLocationsService

@Component
class FixLocationsJob(
  private val fixLocationsService: FixLocationsService,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    log.info("executing fix location job")
    fixLocationsService.fixIncidentDetailsLocations()
    fixLocationsService.fixReportedAdjudicationLocations()
    fixLocationsService.fixHearingLocations()
    log.info("executing fix location job complete")
  }
}
