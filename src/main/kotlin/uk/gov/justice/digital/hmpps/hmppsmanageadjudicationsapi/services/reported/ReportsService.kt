package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.AgencyReportCountsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.ReportsForIssueDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.LocationService
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
  private val offenceCodeLookupService: OffenceCodeLookupService,
  private val authenticationFacade: AuthenticationFacade,
  private val locationService: LocationService,
) {
  fun getAllReportedAdjudications(
    startDate: LocalDate,
    endDate: LocalDate,
    statuses: List<ReportedAdjudicationStatus>,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findAllReportsByAgency(
      agencyId = authenticationFacade.activeCaseload,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
      statuses = statuses.map { it.name },
      pageable = pageable,
    ).map {
      it.toDto(
        offenceCodeLookupService = offenceCodeLookupService,
        locationService = locationService,
        activeCaseload = authenticationFacade.activeCaseload,
      )
    }

  fun getTransferReportedAdjudications(
    statuses: List<ReportedAdjudicationStatus>,
    transferType: TransferType,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> =
    when (transferType) {
      TransferType.IN -> reportedAdjudicationRepository.findTransfersInByAgency(
        agencyId = authenticationFacade.activeCaseload,
        statuses = statuses.filter { transferReviewStatuses.contains(it) }.map { it.name },
        pageable = pageable,
      )
      TransferType.OUT -> reportedAdjudicationRepository.findTransfersOutByAgency(
        agencyId = authenticationFacade.activeCaseload,
        statuses = statuses.filter { transferOutStatuses.contains(it) }.map { it.name },
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
        pageable = pageable,
      )
      TransferType.ALL -> reportedAdjudicationRepository.findTransfersAllByAgency(
        agencyId = authenticationFacade.activeCaseload,
        statuses = statuses.filter { transferOutStatuses.plus(transferReviewStatuses).contains(it) }.map { it.name },
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
        pageable = pageable,
      )
    }.map {
      it.toDto(
        offenceCodeLookupService = offenceCodeLookupService,
        locationService = locationService,
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
    return reportedAdjudicationsPage.map { it.toDto(offenceCodeLookupService, locationService) }
  }

  fun getAdjudicationsForIssue(startDate: LocalDate, endDate: LocalDate): List<ReportsForIssueDto> = reportedAdjudicationRepository.findReportsForIssue(
    agencyId = authenticationFacade.activeCaseload,
    startDate = reportsFrom(startDate),
    endDate = reportsTo(endDate),
    statuses = ReportedAdjudicationStatus.issuableStatuses().map { it.name },
  ).map {
    ReportsForIssueDto(
      chargeNumber = it.chargeNumber,
      prisonerNumber = it.prisonerNumber,
      dateTimeOfIncident = it.dateTimeOfIncident,
      dateTimeOfDiscovery = it.dateTimeOfDiscovery,
      issuingOfficer = it.issuingOfficer,
      dateTimeOfIssue = it.dateTimeOfIssue,
      disIssueHistory = it.disIssueHistory.map { dih -> dih.toDto() },
      dateTimeOfFirstHearing = it.dateTimeOfFirstHearing,
    )
  }

  fun getAdjudicationsForPrint(startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>): List<ReportedAdjudicationDto> {
    val reportsForPrint = reportedAdjudicationRepository.findReportsForPrint(
      agencyId = authenticationFacade.activeCaseload,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
      statuses = ReportedAdjudicationStatus.issuableStatusesForPrint().map { it.name },
    )

    if (issueStatuses.containsAll(IssuedStatus.entries)) {
      return reportsForPrint.map {
        it.toDto(
          offenceCodeLookupService = offenceCodeLookupService,
          locationService = locationService,
          activeCaseload = authenticationFacade.activeCaseload,
        )
      }
    }

    if (issueStatuses.contains(IssuedStatus.ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue != null }.map {
        it.toDto(
          offenceCodeLookupService = offenceCodeLookupService,
          locationService = locationService,
          activeCaseload = authenticationFacade.activeCaseload,
        )
      }
    }

    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue == null }.map {
        it.toDto(
          offenceCodeLookupService = offenceCodeLookupService,
          locationService = locationService,
          activeCaseload = authenticationFacade.activeCaseload,
        )
      }
    }

    return emptyList()
  }

  fun getReportCounts(): AgencyReportCountsDto {
    val agencyId = authenticationFacade.activeCaseload

    val reviewTotal =
      reportedAdjudicationRepository.countByOriginatingAgencyIdAndStatus(
        agencyId = agencyId,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
      )

    val transferReviewTotal =
      reportedAdjudicationRepository.countTransfersIn(
        agencyId = agencyId,
        statuses = transferReviewStatuses.map { it.name },
      )

    val transferOutTotal =
      reportedAdjudicationRepository.countTransfersOut(
        agencyId = agencyId,
        statuses = transferOutStatuses.map { it.name },
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
      )

    val hearingsToScheduleTotal =
      reportedAdjudicationRepository.countByOriginatingAgencyIdAndOverrideAgencyIdIsNullAndStatusInAndDateTimeOfDiscoveryAfter(
        agencyId = agencyId,
        statuses = hearingsToScheduleStatuses,
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
      )

    val overrideHearingsToScheduleTotal =
      reportedAdjudicationRepository.countByOverrideAgencyIdAndStatusInAndDateTimeOfDiscoveryAfter(
        agencyId = agencyId,
        statuses = hearingsToScheduleStatuses,
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
      )

    return AgencyReportCountsDto(
      reviewTotal = reviewTotal,
      transferReviewTotal = transferReviewTotal,
      transferOutTotal = transferOutTotal,
      hearingsToScheduleTotal = hearingsToScheduleTotal + overrideHearingsToScheduleTotal,
    ).also {
      it.transferAllTotal = it.transferReviewTotal + it.transferOutTotal
    }
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
      endDate = reportsTo(endDate ?: LocalDate.now()),
      agencies = agencies,
      statuses = (statuses ?: ReportedAdjudicationStatus.entries).map { it.name },
      pageable = pageable,
    )
  } else {
    reportedAdjudicationRepository.findAdjudicationsForBookingWithPunishments(
      offenderBookingId = bookingId,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: LocalDate.now()),
      agencies = agencies,
      statuses = (statuses ?: ReportedAdjudicationStatus.entries).map { it.name },
      ada = ada,
      pada = pada,
      suspended = suspended,
      pageable = pageable,
    )
  }.map {
    it.toDto(
      offenceCodeLookupService = offenceCodeLookupService,
      locationService = locationService,
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
      endDate = reportsTo(endDate ?: LocalDate.now()),
      statuses = (statuses ?: ReportedAdjudicationStatus.entries).map { it.name },
      pageable = pageable,
    )
  } else {
    reportedAdjudicationRepository.findAdjudicationsForPrisonerWithPunishments(
      prisonerNumber = prisonerNumber,
      startDate = reportsFrom(startDate ?: minDate),
      endDate = reportsTo(endDate ?: LocalDate.now()),
      statuses = (statuses ?: ReportedAdjudicationStatus.entries).map { it.name },
      ada = ada,
      pada = pada,
      suspended = suspended,
      pageable = pageable,
    )
  }.map {
    it.toDto(
      offenceCodeLookupService = offenceCodeLookupService,
      locationService = locationService,
      activeCaseload = authenticationFacade.activeCaseload,
      isAlo = authenticationFacade.isAlo,
    )
  }

  fun getReportsForPrisoner(prisonerNumber: String): List<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber = prisonerNumber).map { it.toDto(offenceCodeLookupService, locationService) }

  fun getReportsForBooking(offenderBookingId: Long): List<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findByOffenderBookingId(offenderBookingId).map { it.toDto(offenceCodeLookupService, locationService) }

  companion object {
    val minDate: LocalDate = LocalDate.EPOCH
    val transferOutAndHearingsToScheduledCutOffDate: LocalDateTime = LocalDate.of(2024, 1, 1).atStartOfDay()
    val transferReviewStatuses = listOf(
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.REFER_INAD,
    )
    val hearingsToScheduleStatuses = listOf(
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.REFER_INAD,
      ReportedAdjudicationStatus.REFER_GOV,
      ReportedAdjudicationStatus.REFER_POLICE,
    )

    val transferOutStatuses = listOf(ReportedAdjudicationStatus.AWAITING_REVIEW, ReportedAdjudicationStatus.SCHEDULED)

    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
  }
}
