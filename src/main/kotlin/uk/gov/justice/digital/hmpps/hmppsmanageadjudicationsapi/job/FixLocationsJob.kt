package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.FixLocationsService
import kotlin.system.measureTimeMillis

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
    val elapsedId = measureTimeMillis {
      fixLocationsService.fixIncidentDetailsLocations()
    }
    log.info("fixIncidentDetailsLocations took ${elapsedId}ms")

    val elapsedRa = measureTimeMillis {
      fixLocationsService.fixReportedAdjudicationLocations()
    }
    log.info("fixReportedAdjudicationLocations took ${elapsedRa}ms")

    val elapsedH = measureTimeMillis {
      fixLocationsService.fixHearingLocations()
    }
    log.info("fixHearingLocations took ${elapsedH}ms")
  }
}
