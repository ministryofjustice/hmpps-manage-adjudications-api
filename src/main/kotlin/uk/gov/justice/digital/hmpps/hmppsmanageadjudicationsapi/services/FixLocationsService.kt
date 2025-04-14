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
    val nomisLocationIds = locationFixRepository.findNomisLocationsIds()

    nomisLocationIds.forEach { nomisLocationId ->
      // get the dps location id
      val dpsId = locationService.getNomisLocationDetail(nomisLocationId.toString())!!.dpsLocationId
      locationFixRepository.updateLocationDetails(locationId = nomisLocationId, locationUuid = UUID.fromString(dpsId))
    }
  }

  fun fixReportedAdjudicationLocations() {
    // look up location ids for adjudications

    // loop through

    // get the dps location id
    // val dpsId = locationService.getNomisLocationDetail("1234")?.dpsLocationId

    // update entities
  }

  fun fixHearingLocations() {
    // look up location ids for hearings

    // loop through

    // get the dps location id
    // val dpsId = locationService.getNomisLocationDetail("1234")?.dpsLocationId

    // update entities
  }
}
