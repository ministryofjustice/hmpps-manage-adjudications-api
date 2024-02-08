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
    value = "select * from reported_adjudications ra $TRANSFER_REPORTS_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $TRANSFER_REPORTS_WHERE_CLAUSE",
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
    value = "select * from reported_adjudications ra $ALL_REPORTS_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $ALL_REPORTS_WHERE_CLAUSE",
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

  fun countByOffenderBookingIdAndStatusAndHearingsDateTimeOfHearingAfter(
    bookingId: Long,
    status: ReportedAdjudicationStatus,
    cutOff: LocalDateTime,
  ): Long

  @Query(
    value = "select * from reported_adjudications ra $BOOKING_ID_REPORTS_WITH_DATE_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $BOOKING_ID_REPORTS_WITH_DATE_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findAdjudicationsForBooking(
    @Param("offenderBookingId") offenderBookingId: Long,
    @Param("agencies") agencies: List<String>,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select ra.* from reported_adjudications ra $BOOKING_ID_AND_PUNISHMENTS_REPORTS_WITH_DATE_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $BOOKING_ID_AND_PUNISHMENTS_REPORTS_WITH_DATE_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findAdjudicationsForBookingWithPunishments(
    @Param("offenderBookingId") offenderBookingId: Long,
    @Param("agencies") agencies: List<String>,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    @Param("ada") ada: Boolean,
    @Param("pada") pada: Boolean,
    @Param("suspended") suspended: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $PRISONER_REPORTS_WITH_DATE_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $PRISONER_REPORTS_WITH_DATE_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findAdjudicationsForPrisoner(
    @Param("prisonerNumber") prisonerNumber: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select ra.* from reported_adjudications ra $PRISONER_REPORTS_AND_PUNISHMENTS_WITH_DATE_WHERE_CLAUSE",
    countQuery = "select count(1) from reported_adjudications ra $PRISONER_REPORTS_AND_PUNISHMENTS_WITH_DATE_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findAdjudicationsForPrisonerWithPunishments(
    @Param("prisonerNumber") prisonerNumber: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    @Param("ada") ada: Boolean,
    @Param("pada") pada: Boolean,
    @Param("suspended") suspended: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  fun findByPrisonerNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByIncidentRoleAssociatedPrisonersNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByOffenceDetailsVictimPrisonersNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByOffenderBookingId(offenderBookingId: Long): List<ReportedAdjudication>

  fun findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
    status: ReportedAdjudicationStatus,
    offenderBookingId: Long,
    cutOff: LocalDate,
  ): List<ReportedAdjudication>

  fun findByPrisonerNumberAndChargeNumberStartsWith(prisonerNumber: String, chargeNumber: String): List<ReportedAdjudication>

  fun findByMigratedIsFalseAndStatus(status: ReportedAdjudicationStatus): List<ReportedAdjudication>

  companion object {

    private const val DATE_AND_STATUS_FILTER = "ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate and ra.status in :statuses "
    private const val AGENCY_AND_TRANSFER_STATUS_FILTER = "and (" +
      "ra.originating_agency_id = :agencyId " +
      "or ra.override_agency_id = :agencyId and ra.status not in :transferIgnoreStatuses and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId " +
      "or ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) = :agencyId" +
      ")"

    private const val AGENCIES_INC_TRANSFERS_FILTER = "and (" +
      "ra.originating_agency_id in :agencies or ra.override_agency_id in :agencies)"

    const val BOOKING_ID_REPORTS_WITH_DATE_WHERE_CLAUSE = "where ra.offender_booking_id = :offenderBookingId and $DATE_AND_STATUS_FILTER $AGENCIES_INC_TRANSFERS_FILTER"

    const val BOOKING_ID_AND_PUNISHMENTS_REPORTS_WITH_DATE_WHERE_CLAUSE = "join punishment p on p.reported_adjudication_fk_id = ra.id where ra.offender_booking_id = :offenderBookingId " +
      "and ((:ada is true and p.type = 'ADDITIONAL_DAYS') or (:suspended is true and p.suspended_until is not null) or (:pada is true and p.type = 'PROSPECTIVE_DAYS')) and $DATE_AND_STATUS_FILTER $AGENCIES_INC_TRANSFERS_FILTER"

    const val PRISONER_REPORTS_WITH_DATE_WHERE_CLAUSE = "where ra.prisoner_number = :prisonerNumber and $DATE_AND_STATUS_FILTER"

    const val PRISONER_REPORTS_AND_PUNISHMENTS_WITH_DATE_WHERE_CLAUSE = "join punishment p on p.reported_adjudication_fk_id = ra.id where ra.prisoner_number = :prisonerNumber " +
      "and ((:ada is true and p.type = 'ADDITIONAL_DAYS') or (:suspended is true and p.suspended_until is not null) or (:pada and p.type = 'PROSPECTIVE_DAYS')) and $DATE_AND_STATUS_FILTER"

    const val ALL_REPORTS_WHERE_CLAUSE =
      "where $DATE_AND_STATUS_FILTER $AGENCY_AND_TRANSFER_STATUS_FILTER"

    const val TRANSFER_REPORTS_WHERE_CLAUSE =
      "where $DATE_AND_STATUS_FILTER" +
        "and ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId "
  }
}
