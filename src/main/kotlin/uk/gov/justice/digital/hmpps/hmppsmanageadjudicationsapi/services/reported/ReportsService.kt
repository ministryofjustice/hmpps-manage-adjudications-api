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
    agencyId: String,
    startDate: LocalDate,
    endDate: LocalDate,
    statuses: List<ReportedAdjudicationStatus>,
    transfersOnly: Boolean,
    pageable: Pageable,
  ): Page<ReportedAdjudicationDto> {
    if (authenticationFacade.activeCaseload != agencyId) return Page.empty()

    val reportedAdjudicationsPage = if (transfersOnly) {
      val pageableOverride = PageRequest.of(pageable.pageNumber, pageable.pageSize, Sort.by("dateTimeOfDiscovery").descending())

      reportedAdjudicationRepository.findByOverrideAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        overrideAgencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses,
        pageable = pageableOverride,
      )
    } else {
      reportedAdjudicationRepository.findAllReportsByAgency(
        agencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses.map { it.name },
        pageable = pageable,
      )
    }
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getMyReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, statuses: List<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername

    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        userId = username!!,
        agencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = statuses,
        pageable = pageable,
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getAdjudicationsForIssue(agencyId: String, startDate: LocalDate, endDate: LocalDate): List<ReportedAdjudicationDto> {
    if (authenticationFacade.activeCaseload != agencyId) return emptyList()

    return reportedAdjudicationRepository.findReportsForIssue(
      agencyId = agencyId,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
    ).filter { ReportedAdjudicationStatus.issuableStatuses().contains(it.status) }
      .sortedBy { it.dateTimeOfDiscovery }
      .map { it.toDto() }
  }

  fun getAdjudicationsForPrint(agencyId: String, startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>): List<ReportedAdjudicationDto> {
    if (authenticationFacade.activeCaseload != agencyId) return emptyList()

    val reportsForPrint = reportedAdjudicationRepository.findReportsForPrint(
      agencyId = agencyId,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
      statuses = ReportedAdjudicationStatus.issuableStatusesForPrint().map { it.name },
    )

    if (issueStatuses.containsAll(IssuedStatus.values().toList())) {
      return reportsForPrint.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }
    }

    if (issueStatuses.contains(IssuedStatus.ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue != null }.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }
    }

    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED)) {
      return reportsForPrint.filter { it.dateTimeOfIssue == null }.sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }
    }

    return emptyList()
  }

  fun getReportCounts(agencyId: String): AgencyReportCountsDto {
    val reviewTotal = reportedAdjudicationRepository.countByAgencyIdAndStatus(
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
