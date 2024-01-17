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

  fun findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(status: ReportedAdjudicationStatus, prisonerNumber: String, date: LocalDate): List<ReportedAdjudication>

  fun findByPrisonerNumberAndStatusInAndPunishmentsSuspendedUntilAfter(prisonerNumber: String, statuses: List<ReportedAdjudicationStatus>, date: LocalDate): List<ReportedAdjudication>

  fun findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(status: ReportedAdjudicationStatus, prisonerNumber: String, punishmentType: PunishmentType): List<ReportedAdjudication>

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

  fun findByPunishmentsConsecutiveChargeNumberAndPunishmentsTypeIn(consecutiveChargeNumber: String, types: List<PunishmentType>): List<ReportedAdjudication>

  @Query(value = "SELECT nextval(:sequenceName)", nativeQuery = true)
  fun getNextChargeSequence(
    @Param("sequenceName") sequenceName: String,
  ): Long

  fun findByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
    bookingId: Long,
    status: ReportedAdjudicationStatus,
    cutOff: LocalDateTime,
  ): List<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $prisonerReportsWithDateWhereClause",
    countQuery = "select count(1) from reported_adjudications ra $prisonerReportsWithDateWhereClause",
    nativeQuery = true,
  )
  fun findAdjudicationsForBooking(
    @Param("offenderBookingId") offenderBookingId: Long,
    @Param("agencies") agencies: List<String>,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    @Param("transferIgnoreStatuses") transferIgnoreStatuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  fun findByPrisonerNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleStartDateIsAfter(
    status: ReportedAdjudicationStatus,
    offenderBookingId: Long,
    cutOff: LocalDate,
  ): List<ReportedAdjudication>

  fun findByChargeNumberContains(chargeNumber: String): List<ReportedAdjudication>

  companion object {

    private const val dateAndStatusFilter = "ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate and ra.status in :statuses "
    private const val agencyAndStatusFilter = "and (" +
      "ra.originating_agency_id = :agencyId " +
      "or ra.override_agency_id = :agencyId and ra.status not in :transferIgnoreStatuses and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId " +
      "or ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) = :agencyId" +
      ")"

    private const val agenciesAndStatusFilter = "and (" +
      "ra.originating_agency_id in :agencies " +
      "or ra.override_agency_id in :agencies and ra.status not in :transferIgnoreStatuses and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) not in :agencies " +
      "or ra.override_agency_id in :agencies and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) in :agencies" +
      ")"

    const val prisonerReportsWithDateWhereClause = "where ra.offender_booking_id = :offenderBookingId and $dateAndStatusFilter $agenciesAndStatusFilter"

    const val allReportsWhereClause =
      "where $dateAndStatusFilter $agencyAndStatusFilter"

    const val transferReportsWhereClause =
      "where $dateAndStatusFilter" +
        "and ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId "
  }
}
