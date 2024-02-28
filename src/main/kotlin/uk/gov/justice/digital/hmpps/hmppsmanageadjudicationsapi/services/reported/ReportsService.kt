package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AgencyReportCountsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class IssuedStatus {
  ISSUED,
  NOT_ISSUED,
}

enum class TransferType {
  IN,
  OUT,
  ALL,
}

@Transactional(readOnly = true)
@Service
class ReportsService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val authenticationFacade: AuthenticationFacade,
  offenceCodeLookupService: OffenceCodeLookupService,
) : ReportedDtoService(offenceCodeLookupService) {
  fun getAllReportedAdjudications(
    startDate: LocalDate,
    endDate: LocalDate,
    statuses: List<ReportedAdjudicationStatus>,
    // this will de deleted once front end uses new endpoint
    transfersOnly: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    when (transfersOnly) {
      true -> reportedAdjudicationRepository.findTransfersInByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses.map { it.name },
        pageable = pageable,
      )

      false -> reportedAdjudicationRepository.findAllReportsByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses.map { it.name },
        pageable = pageable,
      )
    }.map {
      it.toDto(
        activeCaseload = authenticationFacade.activeCaseload,
      )
    }

  fun getTransferReportedAdjudications(
    startDate: LocalDate?,
    endDate: LocalDate?,
    statuses: List<ReportedAdjudicationStatus>,
    transferType: TransferType,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    when (transferType) {
      TransferType.IN -> reportedAdjudicationRepository.findTransfersInByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate ?: minDate),
        endDate = reportsTo(endDate ?: maxDate),
        statuses = statuses.filter { transferReviewStatuses.contains(it) }.map { it.name },
        pageable = pageable,
      )
      TransferType.OUT -> reportedAdjudicationRepository.findTransfersOutByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate ?: minDate),
        endDate = reportsTo(endDate ?: maxDate),
        statuses = statuses.filter { transferOutStatuses.contains(it) }.map { it.name },
        pageable = pageable,
      )
      TransferType.ALL -> reportedAdjudicationRepository.findTransfersAllByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate ?: minDate),
        endDate = reportsTo(endDate ?: maxDate),
        statuses = statuses.filter { transferOutStatuses.plus(transferReviewStatuses).contains(it) }.map { it.name },
        pageable = pageable,
      )
    }.map {
      it.toDto(
        activeCaseload = authenticationFacade.activeCaseload,
      )
    }

  fun getMyReportedAdjudications(startDate: LocalDate, endDate: LocalDate, statuses: List<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername

    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByCreatedByUserIdAndOriginatingAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        userId = username!!,
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses,
        pageable = pageable,
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getAdjudicationsForIssue(startDate: LocalDate, endDate: LocalDate): List<ReportedAdjudicationDto> = reportedAdjudicationRepository.findReportsForIssue(
    agencyId = authenticationFacade.activeCaseload,
    startDate = reportsFrom(startDate),
    endDate = reportsTo(endDate),
  ).filter { ReportedAdjudicationStatus.issuableStatuses().contains(it.status) }
    .sortedBy { it.dateTimeOfDiscovery }
    .map {
      it.toDto(
        activeCaseload = authenticationFacade.activeCaseload,
      )
    }

  fun getAdjudicationsForPrint(startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>): List<ReportedAdjudicationDto> {
    val reportsForPrint = reportedAdjudicationRepository.findReportsForPrint(
      agencyId = authenticationFacade.activeCaseload,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
      statuses = ReportedAdjudicationStatus.issuableStatusesForPrint().map { it.name },
    )

    if (issueStatuses.containsAll(IssuedStatus.values().toList())) {
      return reportsForPrint.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto(authenticationFacade.activeCaseload) }
    }

    if (issueStatuses.contains(IssuedStatus.ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue != null }.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto(authenticationFacade.activeCaseload) }
    }

    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue == null }.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto(authenticationFacade.activeCaseload) }
    }

    return emptyList()
  }

  fun getReportCounts(): AgencyReportCountsDto {
    val agencyId = authenticationFacade.activeCaseload

    val reviewTotal = reportedAdjudicationRepository.countByOriginatingAgencyIdAndStatus(
      agencyId = agencyId,
      status = ReportedAdjudicationStatus.AWAITING_REVIEW,
    )

    val transferReviewTotal = reportedAdjudicationRepository.countTransfersIn(
      agencyId = agencyId,
      statuses = transferReviewStatuses.map { it.name },
    )

    val transferOutTotal = reportedAdjudicationRepository.countTransfersOut(
      agencyId = agencyId,
      statuses = transferOutStatuses.map { it.name },
    )

    return AgencyReportCountsDto(
      reviewTotal = reviewTotal,
      transferReviewTotal = transferReviewTotal,
      transferOutTotal = transferOutTotal,
      transferAllTotal = transferReviewTotal + transferOutTotal,
    )
  }

  fun getAdjudicationsForBooking(
    bookingId: Long,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    agencies: List<String>,
    statuses: List<ReportedAdjudicationStatus>?,
    ada: Boolean,
    pada: Boolean,
    suspended: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> = if (!ada && !suspended && !pada) {
    reportedAdjudicationRepository.findAdjudicationsForBooking(
      offenderBookingId = bookingId,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: maxDate),
      agencies = agencies,
      statuses = (statuses ?: ReportedAdjudicationStatus.values().toList()).map { it.name },
      pageable = pageable,
    )
  } else {
    reportedAdjudicationRepository.findAdjudicationsForBookingWithPunishments(
      offenderBookingId = bookingId,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: maxDate),
      agencies = agencies,
      statuses = (statuses ?: ReportedAdjudicationStatus.values().toList()).map { it.name },
      ada = ada,
      pada = pada,
      suspended = suspended,
      pageable = pageable,
    )
  }.map {
    it.toDto(
      activeCaseload = authenticationFacade.activeCaseload,
      isAlo = authenticationFacade.isAlo,
    )
  }

  fun getAdjudicationsForPrisoner(
    prisonerNumber: String,
    startDate: LocalDate? = null,
    endDate: LocalDate? = null,
    statuses: List<ReportedAdjudicationStatus>?,
    ada: Boolean,
    pada: Boolean,
    suspended: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> = if (!ada && !suspended && !pada) {
    reportedAdjudicationRepository.findAdjudicationsForPrisoner(
      prisonerNumber = prisonerNumber,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: maxDate),
      statuses = (statuses ?: ReportedAdjudicationStatus.values().toList()).map { it.name },
      pageable = pageable,
    )
  } else {
    reportedAdjudicationRepository.findAdjudicationsForPrisonerWithPunishments(
      prisonerNumber = prisonerNumber,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: maxDate),
      statuses = (statuses ?: ReportedAdjudicationStatus.values().toList()).map { it.name },
      ada = ada,
      pada = pada,
      suspended = suspended,
      pageable = pageable,
    )
  }.map {
    it.toDto(
      activeCaseload = authenticationFacade.activeCaseload,
      isAlo = authenticationFacade.isAlo,
    )
  }

  fun getReportsForPrisoner(prisonerNumber: String): List<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prisonerNumber).map { it.toDto() }

  fun getReportsForBooking(offenderBookingId: Long): List<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findByOffenderBookingId(offenderBookingId).map { it.toDto() }

  companion object {
    val minDate: LocalDate = LocalDate.of(1901, 1, 1)
    val maxDate: LocalDate = LocalDate.of(2999, 1, 1)
    val transferReviewStatuses = listOf(
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.REFER_INAD,
    )
    val transferOutStatuses = listOf(ReportedAdjudicationStatus.AWAITING_REVIEW, ReportedAdjudicationStatus.SCHEDULED)

    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
  }
}
