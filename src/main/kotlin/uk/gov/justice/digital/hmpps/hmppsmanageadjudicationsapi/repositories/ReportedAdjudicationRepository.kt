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
    value = "select * from reported_adjudications ra $TRANSFER_IN_REPORTS_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findTransfersInByAgency(
    @Param("agencyId") agencyId: String,
    @Param("statuses") statuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $TRANSFER_OUT_REPORTS_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findTransfersOutByAgency(
    @Param("agencyId") agencyId: String,
    @Param("statuses") statuses: List<String>,
    @Param("cutOffDate") cutOffDate: LocalDateTime,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $TRANSFER_ALL_REPORTS_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findTransfersAllByAgency(
    @Param("agencyId") agencyId: String,
    @Param("statuses") statuses: List<String>,
    @Param("cutOffDate") cutOffDate: LocalDateTime,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = "select * from reported_adjudications ra $ALL_REPORTS_WHERE_CLAUSE",
    nativeQuery = true,
  )
  fun findAllReportsByAgency(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
    pageable: Pageable,
  ): Page<ReportedAdjudication>

  @Query(
    value = """
      select * from reported_adjudications ra 
      where ra.status in :statuses and ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate 
      and (ra.originating_agency_id = :agencyId or ra.override_agency_id = :agencyId) order by ra.date_time_of_discovery asc
      """,
    nativeQuery = true,
  )
  fun findReportsForIssue(
    @Param("agencyId") agencyId: String,
    @Param("startDate") startDate: LocalDateTime,
    @Param("endDate") endDate: LocalDateTime,
    @Param("statuses") statuses: List<String>,
  ): List<ReportedAdjudication>

  @Query(
    value = """
      select * from reported_adjudications ra 
      where ra.date_time_of_first_hearing > :startDate and ra.date_time_of_first_hearing <= :endDate 
      and ra.status in :statuses 
      and (ra.originating_agency_id = :agencyId or ra.override_agency_id = :agencyId) order by ra.date_time_of_first_hearing asc
      """,
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

  fun countByOverrideAgencyIdAndStatusInAndDateTimeOfDiscoveryAfter(agencyId: String, statuses: List<ReportedAdjudicationStatus>, cutOffDate: LocalDateTime): Long

  fun countByOriginatingAgencyIdAndOverrideAgencyIdIsNullAndStatusInAndDateTimeOfDiscoveryAfter(agencyId: String, statuses: List<ReportedAdjudicationStatus>, cutOffDate: LocalDateTime): Long

  fun findByPunishmentsActivatedByChargeNumber(chargeNumber: String): List<ReportedAdjudication>

  @Query(
    value = "select count(1) from reported_adjudications ra where ra.status in :statuses $TRANSFER_IN",
    nativeQuery = true,
  )
  fun countTransfersIn(
    @Param("agencyId") agencyId: String,
    @Param("statuses") statuses: List<String>,
  ): Long

  @Query(
    value = "select count(1) from reported_adjudications ra where ra.status in :statuses $TRANSFER_OUT",
    nativeQuery = true,
  )
  fun countTransfersOut(
    @Param("agencyId") agencyId: String,
    @Param("statuses") statuses: List<String>,
    @Param("cutOffDate") cutOffDate: LocalDateTime,
  ): Long

  fun findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn(chargeNumber: String, types: List<PunishmentType>): List<ReportedAdjudication>

  @Query(
    value = """
        SELECT DISTINCT ra.*
        FROM reported_adjudications ra
        JOIN punishment p 
            ON p.reported_adjudication_fk_id = ra.id
        JOIN punishment_schedule ps
            ON ps.punishment_fk_id = p.id
        JOIN reported_adjudications ra2
            ON ra2.charge_number = p.consecutive_to_charge_number
        WHERE p.consecutive_to_charge_number = :chargeNumber
          AND p.type::text IN (:types)
          AND (p.deleted <> true OR p.deleted IS NULL)
    """,
    nativeQuery = true,
  )
  fun findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeInV2(chargeNumber: String, types: List<PunishmentType>): List<ReportedAdjudication>

  @Query(value = "SELECT nextval(:sequenceName)", nativeQuery = true)
  fun getNextChargeSequence(
    @Param("sequenceName") sequenceName: String,
  ): Long

  @Query(
    value = """
    SELECT COUNT(1) 
    FROM (SELECT DISTINCT ra.charge_number FROM reported_adjudications ra
    JOIN hearing h ON h.reported_adjudication_fk_id = ra.id
    JOIN hearing_outcome ho ON ho.id = h.outcome_id
    WHERE
     ra.status = 'CHARGE_PROVED' 
     AND ra.offender_booking_id = :bookingId
     AND h.date_time_of_hearing >= :cutOff 
     AND ho.code = 'COMPLETE') tbl
  """,
    nativeQuery = true,
  )
  fun activeChargeProvedForBookingId(
    @Param("bookingId") bookingId: Long,
    @Param("cutOff") cutOff: LocalDateTime,
  ): Long

  @Query(
    value = "select * from reported_adjudications ra $BOOKING_ID_REPORTS_WITH_DATE_WHERE_CLAUSE",
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

  fun findByPrisonerNumberAndDateTimeOfDiscoveryBetween(prisonerNumber: String, fromDate: LocalDateTime, toDate: LocalDateTime): List<ReportedAdjudication>

  fun findByIncidentRoleAssociatedPrisonersNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByOffenceDetailsVictimPrisonersNumber(prisonerNumber: String): List<ReportedAdjudication>

  fun findByOffenderBookingId(offenderBookingId: Long): List<ReportedAdjudication>

  fun findByOffenderBookingIdAndStatus(offenderBookingId: Long, status: ReportedAdjudicationStatus): List<ReportedAdjudication>

  fun findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
    status: ReportedAdjudicationStatus,
    offenderBookingId: Long,
    cutOff: LocalDate,
  ): List<ReportedAdjudication>

  fun findByPrisonerNumberAndChargeNumberStartsWith(prisonerNumber: String, chargeNumber: String): List<ReportedAdjudication>

  fun existsByOffenderBookingId(offenderBookingId: Long): Boolean

  companion object {

    private const val STATUS_FILTER = "ra.status in :statuses "
    private const val DATE_AND_STATUS_FILTER = "ra.date_time_of_discovery > :startDate and ra.date_time_of_discovery <= :endDate and $STATUS_FILTER"
    private const val AGENCY_AND_TRANSFER_STATUS_FILTER = "and ((ra.originating_agency_id = :agencyId and (ra.override_agency_id is null or ra.status = 'AWAITING_REVIEW')) or ra.override_agency_id = :agencyId)"
    private const val AGENCIES_INC_TRANSFERS_FILTER = "and (ra.originating_agency_id in :agencies or ra.override_agency_id in :agencies)"
    private const val TRANSFER_OUT = """
      and ra.override_agency_id is not null 
      and ra.originating_agency_id = :agencyId 
      and ra.date_time_of_discovery >= :cutOffDate 
      and (
      ra.status = 'AWAITING_REVIEW' 
       or (
        ra.status = 'SCHEDULED' 
        and 
        0 = (select count(1) from hearing h where h.agency_id = ra.override_agency_id and h.charge_number = ra.charge_number)
        )
       )
       """

    private const val TRANSFER_IN = "and ra.override_agency_id = :agencyId and coalesce(ra.last_modified_agency_id,ra.originating_agency_id) != :agencyId"

    private const val PUNISHMENTS_FILTER = """
      (
       (:ada is true and p.type = 'ADDITIONAL_DAYS') 
       or 
       (:suspended is true and p.suspended_until is not null and p.activated_by_charge_number is null) 
       or 
       (:pada is true and p.type = 'PROSPECTIVE_DAYS')
       )
    """

    const val BOOKING_ID_REPORTS_WITH_DATE_WHERE_CLAUSE = "where ra.offender_booking_id = :offenderBookingId and $DATE_AND_STATUS_FILTER $AGENCIES_INC_TRANSFERS_FILTER"

    const val BOOKING_ID_AND_PUNISHMENTS_REPORTS_WITH_DATE_WHERE_CLAUSE = """
      join punishment p on p.reported_adjudication_fk_id = ra.id 
      where ra.offender_booking_id = :offenderBookingId 
      and $PUNISHMENTS_FILTER and $DATE_AND_STATUS_FILTER $AGENCIES_INC_TRANSFERS_FILTER
      """

    const val PRISONER_REPORTS_WITH_DATE_WHERE_CLAUSE = "where ra.prisoner_number = :prisonerNumber and $DATE_AND_STATUS_FILTER"

    const val PRISONER_REPORTS_AND_PUNISHMENTS_WITH_DATE_WHERE_CLAUSE = """
      join punishment p on p.reported_adjudication_fk_id = ra.id 
      where ra.prisoner_number = :prisonerNumber 
      and $PUNISHMENTS_FILTER and $DATE_AND_STATUS_FILTER
      """

    const val ALL_REPORTS_WHERE_CLAUSE = "where $DATE_AND_STATUS_FILTER $AGENCY_AND_TRANSFER_STATUS_FILTER"

    const val TRANSFER_IN_REPORTS_WHERE_CLAUSE = "where $STATUS_FILTER $TRANSFER_IN"

    const val TRANSFER_OUT_REPORTS_WHERE_CLAUSE = "where $STATUS_FILTER $TRANSFER_OUT"

    const val TRANSFER_ALL_REPORTS_WHERE_CLAUSE = """
      where $STATUS_FILTER
      and (
        (ra.status in ('AWAITING_REVIEW','SCHEDULED') $TRANSFER_OUT)
      or 
        ( ra.status in ('REFER_POLICE', 'REFER_INAD','UNSCHEDULED', 'ADJOURNED') $TRANSFER_IN)
      )
       """
  }
}
