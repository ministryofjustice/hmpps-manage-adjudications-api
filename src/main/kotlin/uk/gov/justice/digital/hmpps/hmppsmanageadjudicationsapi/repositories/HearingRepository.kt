package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import java.time.LocalDateTime

interface HearingRepository : CrudRepository<Hearing, Long> {
  fun findByAgencyIdAndDateTimeOfHearingBetween(
    agencyId: String,
    start: LocalDateTime,
    end: LocalDateTime
  ): List<Hearing>
}
