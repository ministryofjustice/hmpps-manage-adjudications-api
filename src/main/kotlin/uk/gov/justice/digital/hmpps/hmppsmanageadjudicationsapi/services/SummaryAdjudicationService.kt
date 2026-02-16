package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.slf4j.LoggerFactory
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
  companion object {
    private val log = LoggerFactory.getLogger(SummaryAdjudicationService::class.java)
  }

  @Transactional(readOnly = true)
  fun getAdjudicationSummary(
    bookingId: Long,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
  ): AdjudicationSummary {
    log.info("getAdjudicationSummary called for bookingId={}", bookingId)
    val startTime = System.currentTimeMillis()

    val cutOff = adjudicationCutoffDate ?: LocalDate.now().minusMonths(3)
    val provenByOffenderBookingId =
      getReportCountForProfile(
        offenderBookingId = bookingId,
        cutOff = cutOff.atStartOfDay(),
      )
    val countTime = System.currentTimeMillis()
    log.info("getReportCountForProfile completed in {}ms for bookingId={}", countTime - startTime, bookingId)

    val activePunishmentCount = countActivePunishmentsForBooking(
      offenderBookingId = bookingId,
      cutOff = LocalDate.now().minusDays(1),
    )
    val awards = (1..activePunishmentCount).map { Award(bookingId = bookingId) }
    val totalTime = System.currentTimeMillis()
    log.info("getAdjudicationSummary completed in {}ms for bookingId={}, adjudicationCount={}, awards={}", totalTime - startTime, bookingId, provenByOffenderBookingId, awards.size)

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
