package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication

interface DraftAdjudicationRepository : CrudRepository<DraftAdjudication, Long> {
  fun save(draftAdjudication: DraftAdjudication?): DraftAdjudication
  fun findByCreatedByUserId(createdUserId: String): List<DraftAdjudication>
}
