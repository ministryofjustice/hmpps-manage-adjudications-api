package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.HasAdjudicationsResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.Award
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsReportQueryService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationBaseService
import java.time.LocalDate

@Service
class SummaryAdjudicationService(
  private val punishmentsReportQueryService: PunishmentsReportQueryService,
  reportedAdjudicationRepository: ReportedAdjudicationRepository,
  offenceCodeLookupService: OffenceCodeLookupService,
  authenticationFacade: AuthenticationFacade,
) : ReportedAdjudicationBaseService(
  reportedAdjudicationRepository,
  offenceCodeLookupService,
  authenticationFacade,
) {

  @Transactional(readOnly = true)
  fun getAdjudicationSummary(
    bookingId: Long,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
  ): AdjudicationSummary {
    val cutOff = adjudicationCutoffDate ?: LocalDate.now().minusMonths(3)
    val provenByOffenderBookingId =
      getReportCountForProfile(
        offenderBookingId = bookingId,
        cutOff = cutOff.atStartOfDay(),
      )
    val awards =
      punishmentsReportQueryService.getReportsWithActivePunishments(offenderBookingId = bookingId).map { it.second }
        .flatten().map {
          Award(
            bookingId = bookingId,
          )
        }

    return AdjudicationSummary(
      bookingId = bookingId,
      adjudicationCount = provenByOffenderBookingId.toInt(),
      awards = awards,

    )
  }

  @Transactional(readOnly = true)
  fun hasAdjudications(bookingId: Long): HasAdjudicationsResponse = HasAdjudicationsResponse(
    hasAdjudications = offenderHasAdjudications(bookingId),
  )
}
