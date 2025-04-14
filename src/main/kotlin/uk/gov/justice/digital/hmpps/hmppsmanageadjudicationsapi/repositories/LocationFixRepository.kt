package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import java.util.UUID

interface LocationFixRepository : Repository<IncidentDetails, Long> {

  @Query(
    value = "select distinct id.location_id from incident_details id where id.location_id is not null and id.location_uuid is null",
    nativeQuery = true,
  )
  fun findNomisLocationsIds(): List<Long>

  @Query(
    value = "update incident_details set location_uuid = :locationUuid where location_id = :locationId",
    nativeQuery = true,
  )
  @Modifying
  fun updateLocationDetails(@Param("locationId") locationId: Long, @Param("locationUuid") locationUuid: UUID)
}
