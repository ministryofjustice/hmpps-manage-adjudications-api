package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication

interface ReportedAdjudicationRepository : CrudRepository<ReportedAdjudication, Long> {
  fun findByReportNumber(adjudicationNumber: Long): ReportedAdjudication?
}
