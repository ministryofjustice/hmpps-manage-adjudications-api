package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import java.time.LocalDateTime

interface DraftAdjudicationRepository : CrudRepository<DraftAdjudication, Long> {
  fun save(draftAdjudication: DraftAdjudication?): DraftAdjudication

  fun findDraftAdjudicationByAgencyIdAndCreatedByUserIdAndReportNumberIsNull(agencyId: String, username: String): List<DraftAdjudication>

  fun deleteDraftAdjudicationByCreateDateTimeBeforeAndReportNumberIsNotNull(createdTime: LocalDateTime): List<DraftAdjudication>

  fun findByCreateDateTimeAfterAndReportNumberIsNull(createdTime: LocalDateTime): List<DraftAdjudication>
}
