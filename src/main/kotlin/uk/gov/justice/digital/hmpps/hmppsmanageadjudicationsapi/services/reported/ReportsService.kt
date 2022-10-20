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
import java.util.Optional

@Transactional(readOnly = true)
@Service
class ReportsService(
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val authenticationFacade: AuthenticationFacade,
  offenceCodeLookupService: OffenceCodeLookupService
) : ReportedDtoService(offenceCodeLookupService) {
  fun getAllReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, status: Optional<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        agencyId,
        ReportedAdjudicationService.reportsFrom(startDate),
        ReportedAdjudicationService.reportsTo(endDate),
        ReportedAdjudicationService.statuses(status), pageable
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }

  fun getMyReportedAdjudications(agencyId: String, startDate: LocalDate, endDate: LocalDate, status: Optional<ReportedAdjudicationStatus>, pageable: Pageable): Page<ReportedAdjudicationDto> {
    val username = authenticationFacade.currentUsername

    val reportedAdjudicationsPage =
      reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        username!!, agencyId,
        ReportedAdjudicationService.reportsFrom(startDate),
        ReportedAdjudicationService.reportsTo(endDate),
        ReportedAdjudicationService.statuses(status), pageable
      )
    return reportedAdjudicationsPage.map { it.toDto() }
  }
}
