package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    val nomisLocationIds = locationFixRepository.findNomisIncidentDetailsLocationsIds()
    var updateCount: Int = 0

    nomisLocationIds.mapNotNull { nomisLocationId ->
      try {
        nomisLocationId to locationService.getNomisLocationDetail(nomisLocationId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Incident detail location ID: $nomisLocationId", e)
        null
      }
    }.forEach { (nomisLocationId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateIncidentDetailsLocationIdDetails(
          locationId = nomisLocationId,
          locationUuid = UUID.fromString(dpsId),
        )
        updateCount++
      }
    }
    log.info("Updated $updateCount of ${nomisLocationIds.size} distinct incident detail location Ids")
  }

  fun fixReportedAdjudicationLocations() {
    // look up location ids for reported adjudications
    val reportedAdjudicationsIds = locationFixRepository.findNomisReportedAdjudicationsLocationsIds()
    var updateCount: Int = 0
    reportedAdjudicationsIds.mapNotNull { reportedAdjudicationsId ->
      try {
        reportedAdjudicationsId to locationService.getNomisLocationDetail(reportedAdjudicationsId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Reported Adjudication location ID: $reportedAdjudicationsId", e)
        null
      }
    }.forEach { (reportedAdjudicationsId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateReportedAdjudicationsLocationsIdDetails(
          locationId = reportedAdjudicationsId,
          locationUuid = UUID.fromString(dpsId),
        )
        updateCount++
      }
    }
    log.info("Updated $updateCount of ${reportedAdjudicationsIds.size} distinct reported adjudication location Ids")
  }

  fun fixHearingLocations() {
    // look up location ids for hearings
    val hearingIds = locationFixRepository.findNomisHearingsLocationsIds()
    var updateCount: Int = 0
    hearingIds.mapNotNull { hearingId ->
      try {
        hearingId to locationService.getNomisLocationDetail(hearingId.toString())!!.dpsLocationId
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for hearing location ID: $hearingId", e)
        null
      }
    }.forEach { (hearingId, dpsId) ->
      transactionHandler.newSpringTransaction {
        locationFixRepository.updateHearingsLocationIdDetails(
          locationId = hearingId,
          locationUuid = UUID.fromString(dpsId)
        )
        updateCount++
      }
    }
    log.info("Updated $updateCount of ${hearingIds.size} distinct hearing location Ids")
  }
}
