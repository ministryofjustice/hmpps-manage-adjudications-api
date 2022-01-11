package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication

interface ReportedAdjudicationRepository : CrudRepository<ReportedAdjudication, Long> {
  fun findByCreatedByUserIdAndAgencyId(userId: String, agencyId: String, pageable: Pageable): Page<ReportedAdjudication>
  fun findByAgencyId(agencyId: String, pageable: Pageable): Page<ReportedAdjudication>
  fun findByReportNumber(adjudicationNumber: Long): ReportedAdjudication?
}
