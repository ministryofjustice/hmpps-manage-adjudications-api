package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import java.util.UUID

interface LocationFixRepository : Repository<IncidentDetails, Long> {

  @Query(
    value = "select distinct id.location_id from incident_details id where id.location_id is not null and id.location_uuid is null limit 200",
    nativeQuery = true,
  )
  fun findNomisIncidentDetailsLocationsIds(): List<Long>

  @Query(
    value = "update incident_details set location_uuid = :locationUuid where location_id = :locationId",
    nativeQuery = true,
  )
  @Modifying
  fun updateIncidentDetailsLocationIdDetails(@Param("locationId") locationId: Long, @Param("locationUuid") locationUuid: UUID)

  @Query(
    value = "select distinct ra.location_id from reported_adjudications ra where ra.location_id is not null and ra.location_uuid is null limit 200",
    nativeQuery = true,
  )
  fun findNomisReportedAdjudicationsLocationsIds(): List<Long>

  @Query(
    value = "update reported_adjudications set location_uuid = :locationUuid where location_id = :locationId",
    nativeQuery = true,
  )
  @Modifying
  fun updateReportedAdjudicationsLocationsIdDetails(@Param("locationId") locationId: Long, @Param("locationUuid") locationUuid: UUID)

  @Query(
    value = "select distinct h.location_id from hearing h where h.location_id is not null and h.location_uuid is null limit 200",
    nativeQuery = true,
  )
  fun findNomisHearingsLocationsIds(): List<Long>

  @Query(
    value = "update hearing set location_uuid = :locationUuid where location_id = :locationId",
    nativeQuery = true,
  )
  @Modifying
  fun updateHearingsLocationIdDetails(@Param("locationId") locationId: Long, @Param("locationUuid") locationUuid: UUID)
}
