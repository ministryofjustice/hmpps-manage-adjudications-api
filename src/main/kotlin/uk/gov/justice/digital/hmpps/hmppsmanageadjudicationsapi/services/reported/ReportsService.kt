package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IssuedStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

  fun getAdjudicationsForIssue(agencyId: String, locationId: Long? = null, startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>? = null, pageable: Pageable): Page<ReportedAdjudicationDto> {

    val issueStatusesFilter = issueStatuses ?: IssuedStatus.values().toList()

    if (locationId == null)
      return getAdjudicationsForIssueAllLocations(
        agencyId = agencyId, startDate = startDate, endDate = endDate, issueStatuses = issueStatusesFilter, pageable = pageable
      )

    return getAdjudicationsForIssueOneLocation(
      agencyId = agencyId, locationId = locationId, startDate = startDate, endDate = endDate, issueStatuses = issueStatusesFilter, pageable = pageable
    )
  }

  private fun getAdjudicationsForIssueAllLocations(agencyId: String, startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    if (issueStatuses.containsAll(IssuedStatus.values().toList()))
      return getAllReportedAdjudications(agencyId = agencyId, startDate = startDate, endDate = endDate, statuses = ReportedAdjudicationStatus.issuableStatuses(), pageable = pageable)
    if (issueStatuses.contains(IssuedStatus.ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndDateTimeOfIssueIsNotNull(
        agencyId = agencyId, startDate = reportsFrom(startDate), endDate = reportsTo(endDate), statuses = ReportedAdjudicationStatus.issuableStatuses(), pageable = pageable
      ).map { it.toDto() }
    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndDateTimeOfIssueIsNull(
        agencyId = agencyId, startDate = reportsFrom(startDate), endDate = reportsTo(endDate), statuses = ReportedAdjudicationStatus.issuableStatuses(), pageable = pageable
      ).map { it.toDto() }

    return Page.empty()
  }

  private fun getAdjudicationsForIssueOneLocation(agencyId: String, locationId: Long? = null, startDate: LocalDate, endDate: LocalDate, issueStatuses: List<IssuedStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    if (issueStatuses.containsAll(IssuedStatus.values().toList()))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndLocationId(
        agencyId = agencyId, startDate = reportsFrom(startDate), endDate = reportsTo(endDate), statuses = ReportedAdjudicationStatus.issuableStatuses(), locationId = locationId!!, pageable = pageable
      ).map { it.toDto() }
    if (issueStatuses.contains(IssuedStatus.ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndLocationIdAndDateTimeOfIssueIsNotNull(
        agencyId = agencyId, startDate = reportsFrom(startDate), endDate = reportsTo(endDate), statuses = ReportedAdjudicationStatus.issuableStatuses(), locationId = locationId!!, pageable = pageable
      ).map { it.toDto() }
    if (issueStatuses.contains(IssuedStatus.NOT_ISSUED))
      return reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusInAndLocationIdAndDateTimeOfIssueIsNull(
        agencyId = agencyId, startDate = reportsFrom(startDate), endDate = reportsTo(endDate), statuses = ReportedAdjudicationStatus.issuableStatuses(), locationId = locationId!!, pageable = pageable
      ).map { it.toDto() }

    return Page.empty()
  }

  companion object {
    fun reportsFrom(startDate: LocalDate): LocalDateTime = startDate.atStartOfDay()
    fun reportsTo(endDate: LocalDate): LocalDateTime = endDate.atTime(LocalTime.MAX)
  }
}
