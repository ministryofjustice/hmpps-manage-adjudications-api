package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory

interface SubmittedAdjudicationHistoryRepository : CrudRepository<SubmittedAdjudicationHistory, Long> {
  fun findByCreatedByUserIdAndAgencyId(userId: String, agencyId: String, pageable: Pageable): Page<SubmittedAdjudicationHistory>
  fun findByAgencyId(agencyId: String, pageable: Pageable): Page<SubmittedAdjudicationHistory>
  fun findByAdjudicationNumber(adjudicationNumber: Long): SubmittedAdjudicationHistory?
}
