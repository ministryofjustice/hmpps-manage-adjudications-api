package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import java.time.LocalDateTime

interface HearingRepository : CrudRepository<Hearing, Long> {
  fun findByAgencyIdAndDateTimeOfHearingBetween(
    agencyId: String,
    start: LocalDateTime,
    end: LocalDateTime,
  ): List<Hearing>

  @Query(
    value = """
      SELECT ra.prisoner_number,
             h.date_time_of_hearing,
             h.location_id,
             h.oic_hearing_type,
             h.id AS hearing_id
       FROM 
      reported_adjudications ra
      JOIN hearing h ON h.reported_adjudication_fk_id = ra.id
      WHERE ra.prisoner_number in :prisoners
        AND h.agency_id = :agencyId
        AND h.date_time_of_hearing BETWEEN :startDate AND :endDate
    """,
    nativeQuery = true,
  )
  fun getHearingsByPrisoner(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("prisoners") prisoners: List<String>,
  ): List<HearingsByPrisoner>
}

interface HearingsByPrisoner {
  fun getPrisonerNumber(): String
  fun getDateTimeOfHearing(): LocalDateTime
  fun getOicHearingType(): String
  fun getLocationId(): Long
  fun getHearingId(): Long
}
