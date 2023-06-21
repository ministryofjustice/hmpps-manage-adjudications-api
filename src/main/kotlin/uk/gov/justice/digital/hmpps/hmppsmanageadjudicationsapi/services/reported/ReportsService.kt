package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
  ISSUED, NOT_ISSUED
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
    transfersOnly: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage = if (transfersOnly) {
      val pageableOverride = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("dateTimeOfDiscovery").descending())

      reportedAdjudicationRepository.findByOverrideAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        overrideAgencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses,
        pageable = pageableOverride,
      )
    } else {
      reportedAdjudicationRepository.findAllReportsByAgency(
        agencyId = authenticationFacade.activeCaseload,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses.map { it.name },
        pageable = pageable,
      )
    }
    return reportedAdjudicationsPage.map { it.toDto(authenticationFacade.activeCaseload) }
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

  fun getAdjudicationsForIssue(startDate: LocalDate, endDate: LocalDate): List<ReportedAdjudicationDto> {
    return reportedAdjudicationRepository.findReportsForIssue(
      agencyId = authenticationFacade.activeCaseload,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
    ).filter { ReportedAdjudicationStatus.issuableStatuses().contains(it.status) }
      .sortedBy { it.dateTimeOfDiscovery }
      .map { it.toDto(authenticationFacade.activeCaseload) }
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

    val transferReviewTotal = reportedAdjudicationRepository.countByOverrideAgencyIdAndStatusIn(
      overrideAgencyId = agencyId,
      statuses = transferReviewStatuses,
    )

    return AgencyReportCountsDto(
      reviewTotal = reviewTotal,
      transferReviewTotal = transferReviewTotal,
    )
  }

  companion object {
    val transferReviewStatuses = listOf(
      ReportedAdjudicationStatus.UNSCHEDULED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.ADJOURNED,
      ReportedAdjudicationStatus.REFER_INAD,
    )
    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
  }
}
