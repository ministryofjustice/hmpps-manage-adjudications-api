package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import java.time.LocalDateTime

interface DraftAdjudicationRepository : CrudRepository<DraftAdjudication, Long> {
  fun save(draftAdjudication: DraftAdjudication?): DraftAdjudication

  fun findByAgencyIdAndCreatedByUserIdAndReportNumberIsNullAndIncidentDetailsDateTimeOfDiscoveryBetween(
    agencyId: String,
    username: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    pageable: Pageable,
  ): Page<DraftAdjudication>

  fun deleteDraftAdjudicationByCreateDateTimeBeforeAndReportNumberIsNotNull(createdTime: LocalDateTime): List<DraftAdjudication>
}
