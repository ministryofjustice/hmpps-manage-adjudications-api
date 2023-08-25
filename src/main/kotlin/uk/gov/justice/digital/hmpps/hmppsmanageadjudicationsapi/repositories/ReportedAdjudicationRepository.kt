package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDate
import java.time.LocalDateTime

interface ReportedAdjudicationRepository : CrudRepository<ReportedAdjudication, Long> {

  fun findByCreatedByUserIdAndOriginatingAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
    userId: String,
    agencyId: String,
    startDate: LocalDateTime,
    endDate: LocalDateTime,
    statuses: List<ReportedAdjudicationStatus>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $transferReportsWhereClause",
    countQuery = "select count(1) from reported_adjudications ra $transferReportsWhereClause",
    nativeQuery = true,
  )
  fun findTransfersByAgency(
    @Param("agencyId") overrideAgencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $allReportsWhereClause",
    countQuery = "select count(1) from reported_adjudications ra $allReportsWhereClause",
    nativeQuery = true,
  )
  fun findAllReportsByAgency(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    @Param("transferIgnoreStatuses") transferIgnoreStatuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra " +
      "where ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate " +
      "and (ra.originating_agency_id = :agencyId or ra.override_agency_id = :agencyId)",
    nativeQuery = true,
  )
  fun findReportsForIssue(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
  ): List<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra " +
      "where ra.date_time_of_first_hearing > :startDate and ra.date_time_of_first_hearing <= :endDate " +
      "and ra.status in :statuses " +
      "and (ra.originating_agency_id = :agencyId or ra.override_agency_id = :agencyId)",
    nativeQuery = true,
  )
  fun findReportsForPrint(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
  ): List<ReportedAdjudication>

  fun findByChargeNumber(chargeNumber: String): ReportedAdjudication?

  fun findByChargeNumberIn(chargeNumbers: List<String>): List<ReportedAdjudication>

  fun findByPrisonerNumberAndPunishmentsSuspendedUntilAfter(prisonerNumber: String, date: LocalDate): List<ReportedAdjudication>

  fun findByPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(prisonerNumber: String, punishmentType: PunishmentType): List<ReportedAdjudication>

  fun findByPrisonerNumberAndStatusIn(prisonerNumber: String, statuses: List<ReportedAdjudicationStatus>): List<ReportedAdjudication>

  fun countByOriginatingAgencyIdAndStatus(agencyId: String, status: ReportedAdjudicationStatus): Long

  @Query(
    value = "select count(1) from reported_adjudications ra " +
      "where ra.override_agency_id = :overrideAgencyId " +
      "and ra.status in :statuses " +
      "and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :overrideAgencyId",
    nativeQuery = true,
  )
  fun countTransfers(
    @Param("overrideAgencyId") overrideAgencyId: String,
    @Param("statuses") statuses: List<String>,
  ): Long

  fun findByPunishmentsConsecutiveChargeNumberAndPunishmentsType(consecutiveChargeNumber: String, type: PunishmentType): List<ReportedAdjudication>

  @Query(
    value = "select ra.charge_number from reported_adjudications ra where ra.migrated is false and ra.originating_agency_id = :agency",
    nativeQuery = true,
  )
  fun findRecordsToReset(@Param("agency") agency: String): List<String>

  @Query(
    value = "select ra.id from reported_adjudications ra where ra.migrated is true and ra.originating_agency_id = :agency",
    nativeQuery = true,
  )
  fun findRecordsToDelete(@Param("agency") agency: String): List<Long>

  @Query(
    value = "select distinct ra.originating_agency_id from reported_adjudications ra",
    nativeQuery = true,
  )
  fun getDistinctAgencies(): List<String>

  companion object {

    private const val dateAndStatusFilter = "ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate and ra.status in :statuses "

    const val allReportsWhereClause =
      "where $dateAndStatusFilter" +
        "and (" +
        "ra.originating_agency_id = :agencyId " +
        "or ra.override_agency_id = :agencyId and ra.status not in :transferIgnoreStatuses and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId " +
        "or ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) = :agencyId" +
        ")"

    const val transferReportsWhereClause =
      "where $dateAndStatusFilter" +
        "and ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId "
  }
}
