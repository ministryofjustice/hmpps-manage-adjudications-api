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

    nomisLocationIds.forEach { nomisLocationId ->
      // get the dps location id
      val dpsId = locationService.getNomisLocationDetail(nomisLocationId.toString())!!.dpsLocationId
      locationFixRepository.updateIncidentDetailsLocationIdDetails(locationId = nomisLocationId, locationUuid = UUID.fromString(dpsId))
    }
  }

  fun fixReportedAdjudicationLocations() {
    // look up location ids for adjudications
    val reportedAdjudicationsIds = locationFixRepository.findNomisReportedAdjudicationsLocationsIds()

    // loop through
    reportedAdjudicationsIds.forEach { reportedAdjudicationsId ->
      val dpsId = locationService.getNomisLocationDetail(reportedAdjudicationsId.toString())!!.dpsLocationId
      locationFixRepository.updateReportedAdjudicationsLocationsIdDetails(locationId = reportedAdjudicationsId, locationUuid = UUID.fromString(dpsId))
    }
  }

  fun fixHearingLocations() {
    // look up location ids for hearings
    val hearingIds = locationFixRepository.findNomisHearingsLocationsIds()
    // loop through
    hearingIds.forEach { hearingId ->
      val dpsId = locationService.getNomisLocationDetail(hearingId.toString())!!.dpsLocationId
      locationFixRepository.updateHearingsLocationIdDetails(locationId = hearingId, locationUuid = UUID.fromString(dpsId))
    }
  }
}
