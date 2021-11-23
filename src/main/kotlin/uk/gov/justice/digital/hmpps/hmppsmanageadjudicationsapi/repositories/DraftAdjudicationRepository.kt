package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication

interface DraftAdjudicationRepository : CrudRepository<DraftAdjudication, Long> {
  fun save(draftAdjudication: DraftAdjudication?): DraftAdjudication

  @Query("SELECT da from DraftAdjudication da where da.agencyId = :agencyId and da.createdByUserId = :username and da.reportNumber IS NULL")
  fun findUnsubmittedByAgencyIdAndCreatedByUserId(agencyId: String, username: String): List<DraftAdjudication>
}
