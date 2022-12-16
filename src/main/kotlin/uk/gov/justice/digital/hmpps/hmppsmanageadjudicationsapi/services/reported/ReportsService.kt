package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
  offenceCodeLookupService: OffenceCodeLookupService
) : ReportedDtoService(offenceCodeLookupService) {
  fun getAllReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, statuses: List<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        agencyId,
        reportsFrom(startDate),
        reportsTo(endDate),
        statuses, pageable
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getMyReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, statuses: List<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername

    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        username!!, agencyId,
        reportsFrom(startDate),
        reportsTo(endDate),
        statuses, pageable
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getAdjudicationsForIssue(agencyId: String, startDate: LocalDate, endDate: LocalDate): List<ReportedAdjudicationDto> =
    reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetween(
      agencyId = agencyId,
      startDate = reportsFrom(startDate),
      endDate = reportsTo(endDate),
    ).filter { ReportedAdjudicationStatus.issuableStatuses().contains(it.status) }
      .sortedBy { it.dateTimeOfDiscovery }
      .map { it.toDto() }

  fun getAdjudicationsForPrint(agencyId: String, startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>): List<ReportedAdjudicationDto> {
    if (issueStatuses.containsAll(IssuedStatus.values().toList()))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfFirstHearingBetweenAndStatusIn(
        agencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = ReportedAdjudicationStatus.issuableStatusesForPrint(),
      ).sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }

    if (issueStatuses.contains(IssuedStatus.ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfFirstHearingBetweenAndStatusInAndDateTimeOfIssueIsNotNull(
        agencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = ReportedAdjudicationStatus.issuableStatusesForPrint(),
      ).sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }

    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfFirstHearingBetweenAndStatusInAndDateTimeOfIssueIsNull(
        agencyId = agencyId,
        startDate = reportsFrom(startDate),
        endDate = reportsTo(endDate),
        statuses = ReportedAdjudicationStatus.issuableStatusesForPrint(),
      ).sortedBy { it.dateTimeOfFirstHearing }.map { it.toDto() }

    return emptyList()
  }

  companion object {
    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
  }
}
