package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.*
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.HearingService.Companion.getHearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.NomisOutcomeService.Companion.getAdjudicator
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationService
import java.time.LocalDate

@Service
@Transactional
class SummaryAdjudicationService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val reportedAdjudicationService: ReportedAdjudicationService,
) {
  fun getAdjudication(prisonerNumber: String, chargeId: Long): AdjudicationDetail {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber, chargeId)
    } else {

      val adjudication = reportedAdjudicationRepository.findByPrisonerNumberAndReportNumber(prisonerNumber, chargeId)
        ?: throw EntityNotFoundException("${prisonerNumber}/${chargeId}")
      val adjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(chargeId)

      AdjudicationDetail(
        adjudicationNumber = adjudication.reportNumber,
        incidentTime = adjudicationDto.incidentDetails.dateTimeOfDiscovery,
        reportTime = adjudication.createDateTime,
        prisonId = adjudication.overrideAgencyId ?: adjudication.originatingAgencyId, //TODO: Lookup?
        interiorLocationId = adjudication.locationId, //TODO: lookup
        incidentDetails = adjudication.statement,
        reportType = "Governor's Report",  // WHERE ?
        reporterUsername = adjudication.createdByUserId,//TODO: lookup staff name?

        hearings = listOf(
          Hearing(
            oicHearingId = adjudication.getHearing().oicHearingId,
            hearingType = adjudication.getHearing().oicHearingType.name,
            hearingTime = adjudication.getHearing().dateTimeOfHearing,
            prisonId = adjudication.getHearing().agencyId,
            locationId = adjudication.getHearing().locationId, // TODO: lookup location
            heardByUsername = adjudication.getHearing().getAdjudicator(), // TODO: Adjudicator
            otherRepresentatives = null, // TODO: WHERE
            comment = null, // TODO: WHERE
            results = listOf(
//              HearingResult(
//                oicOffenceCode = reportedAdjudicationService.getNomisCodes(adjudication.incidentRoleCode, adjudication.offenceDetails, adjudication.isYouthOffender).get(0)
//                offenceType = null,
//                offenceDescription = null,
//                plea = outcome.outcome?.plea?.name,
//                finding = adjudication.getOutcomes()
//                  .find { o -> o.oicHearingId == hearing.oicHearingId }?.code?.finding?.name,
//                sanctions = listOf(),
//              ),
//            ),
            ),
          ),
        ),
      )
    }
  }

  fun getAdjudications(
    prisonerNumber: String,
    offenceId: Long?,
    prisonId: String?,
    finding: Finding?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): AdjudicationSearchResponse {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      val response = legacyNomisGateway.getAdjudicationsForPrisoner(
        prisonerNumber,
        offenceId,
        prisonId,
        finding,
        fromDate,
        toDate,
        pageable,
      )

      val pageOffset = response.headers.getHeader("Page-Offset")
      val pageSize = response.headers.getHeader("Page-Limit")
      val totalRecords = response.headers.getHeader("Total-Records")
      response.body?.let {
        AdjudicationSearchResponse(
          results = PageImpl(
            it.results,
            PageRequest.of(pageOffset.toInt() / pageSize.toInt(), pageSize.toInt()),
            totalRecords.toLong(),
          ),
          offences = it.offences,
          agencies = it.agencies,
        )
      } ?: AdjudicationSearchResponse(results = Page.empty(), offences = listOf(), agencies = listOf())
    } else {
      // TODO: get data from this database!
      AdjudicationSearchResponse(results = Page.empty(), offences = listOf(), agencies = listOf())
    }
  }

  private fun HttpHeaders.getHeader(key: String) = this[key]?.get(0) ?: "0"

  fun getAdjudicationSummary(
    bookingId: Long,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
  ): AdjudicationSummary {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getAdjudicationsForPrisonerForBooking(bookingId, awardCutoffDate, adjudicationCutoffDate)
    } else {
      // TODO: get data from this database!
      AdjudicationSummary(adjudicationCount = 0, awards = listOf())
    }
  }

  fun getProvenAdjudicationsForBookings(
    bookingIds: List<Long>,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
  ): List<ProvenAdjudicationsSummary> {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getProvenAdjudicationsForBookings(bookingIds, awardCutoffDate, adjudicationCutoffDate)
    } else {
      // TODO: get data from this database!
      listOf()
    }
  }
}
