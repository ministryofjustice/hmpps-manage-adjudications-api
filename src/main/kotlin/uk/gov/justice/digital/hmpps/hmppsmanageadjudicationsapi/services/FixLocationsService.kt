package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.LocationFixRepository
import java.util.UUID

@Transactional
@Service
class FixLocationsService(
  private val locationService: LocationService,
  private val locationFixRepository: LocationFixRepository,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun fixIncidentDetailsLocations() {
    // look up location ids
    val nomisLocationIds = locationFixRepository.findNomisIncidentDetailsLocationsIds()
    var updateCount: Int = 0

    nomisLocationIds.forEach { nomisLocationId ->
      try {
        val dpsId = locationService.getNomisLocationDetail(nomisLocationId.toString())!!.dpsLocationId
        locationFixRepository.updateIncidentDetailsLocationIdDetails(
          locationId = nomisLocationId,
          locationUuid = UUID.fromString(dpsId)
        )
        updateCount++
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Incident detail location ID: $nomisLocationId", e)
      }
    }
    log.info("Updated $updateCount of ${nomisLocationIds.size} distinct incident detail location Ids")
  }

  fun fixReportedAdjudicationLocations() {
    // look up location ids for reported adjudications
    val reportedAdjudicationsIds = locationFixRepository.findNomisReportedAdjudicationsLocationsIds()
    var updateCount: Int = 0
    reportedAdjudicationsIds.forEach { reportedAdjudicationsId ->
      try {
        val dpsId = locationService.getNomisLocationDetail(reportedAdjudicationsId.toString())!!.dpsLocationId
        locationFixRepository.updateReportedAdjudicationsLocationsIdDetails(
          locationId = reportedAdjudicationsId,
          locationUuid = UUID.fromString(dpsId)
        )
        updateCount++
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Reported Adjudication location ID: $reportedAdjudicationsId", e)
      }
    }
    log.info("Updated $updateCount of ${reportedAdjudicationsIds.size} distinct reported adjudication location Ids")
  }

  fun fixHearingLocations() {
    // look up location ids for hearings
    val hearingIds = locationFixRepository.findNomisHearingsLocationsIds()
    var updateCount: Int = 0
    hearingIds.forEach { hearingId ->
      try {
        val dpsId = locationService.getNomisLocationDetail(hearingId.toString())!!.dpsLocationId
        locationFixRepository.updateHearingsLocationIdDetails(locationId = hearingId, locationUuid = UUID.fromString(dpsId))
        updateCount++
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for hearing location ID: $hearingId", e)
      }
    }
    log.info("Updated $updateCount of ${hearingIds.size} distinct hearing location Ids")
  }
}
