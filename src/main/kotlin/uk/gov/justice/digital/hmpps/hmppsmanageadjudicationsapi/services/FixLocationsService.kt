package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.LocationFixRepository
import java.util.UUID
@Service
class FixLocationsService(
  private val locationService: LocationService,
  private val locationFixRepository: LocationFixRepository,
  private val transactionHandler: TransactionHandler,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun fixIncidentDetailsLocations() {
    // look up location ids
    val incidentDetailsLocationIds = locationFixRepository.findNomisIncidentDetailsLocationsIds()
    var updateCount: Int = 0

    incidentDetailsLocationIds.mapNotNull { incidentDetailsLocationId ->
      try {
        incidentDetailsLocationId to locationService.getNomisLocationDetail(incidentDetailsLocationId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Incident detail location ID: $incidentDetailsLocationId", e)
        null
      }
    }.forEach { (incidientDetailsLocationId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateIncidentDetailsLocationIdDetails(
          locationId = incidientDetailsLocationId,
          locationUuid = UUID.fromString(dpsId),
        )
        updateCount++
      }
      log.info("Updated location id $incidientDetailsLocationId")
    }
    log.info("Completed updating $updateCount of ${incidentDetailsLocationIds.size} distinct incident detail location Ids")
  }

  fun fixReportedAdjudicationLocations() {
    // look up location ids for reported adjudications
    val reportedAdjudicationsLocationIds = locationFixRepository.findNomisReportedAdjudicationsLocationsIds()
    var updateCount: Int = 0
    reportedAdjudicationsLocationIds.mapNotNull { reportedAdjudicationsLocationId ->
      try {
        reportedAdjudicationsLocationId to locationService.getNomisLocationDetail(reportedAdjudicationsLocationId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Reported Adjudication location ID: $reportedAdjudicationsLocationId", e)
        null
      }
    }.forEach { (reportedAdjudicationsLocationId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateReportedAdjudicationsLocationsIdDetails(
          locationId = reportedAdjudicationsLocationId,
          locationUuid = UUID.fromString(dpsId),
        )
        updateCount++
      }
      log.info("Updated location id $reportedAdjudicationsLocationId")
    }
    log.info("Completed updating $updateCount of ${reportedAdjudicationsLocationIds.size} distinct reported adjudication location Ids")
  }

  fun fixHearingLocations() {
    // look up location ids for hearings
    val hearingLocationIds = locationFixRepository.findNomisHearingsLocationsIds()
    var updateCount: Int = 0
    hearingLocationIds.mapNotNull { hearingLocationId ->
      try {
        hearingLocationId to locationService.getNomisLocationDetail(hearingLocationId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for hearing location ID: $hearingLocationId", e)
        null
      }
    }.forEach { (hearingId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateHearingsLocationIdDetails(
          locationId = hearingId,
          locationUuid = UUID.fromString(dpsId),
        )
        updateCount++
      }
      log.info("Updated location id of $hearingId")
    }
    log.info("Updated $updateCount of ${hearingLocationIds.size} distinct hearing location Ids")
  }
}
