package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDateTime

interface ReportedAdjudicationRepository : CrudRepository<ReportedAdjudication, Long> {
  fun findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
    userId: String,
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    statuses: List<ReportedAdjudicationStatus>,
    pageable: Pageable
  ): Page<ReportedAdjudication>
  fun findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    statuses: List<ReportedAdjudicationStatus>,
    pageable: Pageable
  ): Page<ReportedAdjudication>
  fun findByAgencyIdAndDateTimeOfDiscoveryBetween(
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
  ): List<ReportedAdjudication>
  fun findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndDateTimeOfIssueIsNotNull(
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    statuses: List<ReportedAdjudicationStatus>,
  ): List<ReportedAdjudication>
  fun findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndDateTimeOfIssueIsNull(
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    statuses: List<ReportedAdjudicationStatus>,
  ): List<ReportedAdjudication>
  fun findByReportNumber(adjudicationNumber: Long): ReportedAdjudication?
  fun findByReportNumberIn(adjudicationNumbers: List<Long>): List<ReportedAdjudication>
}
