package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSearchResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ProvenAdjudicationsSummary
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import java.time.LocalDate

@Service
@Transactional
class SummaryAdjudicationService(
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
) {
  fun getAdjudication(prisonerNumber: String, chargeId: Long): AdjudicationDetail {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getAdjudicationDetailForPrisoner(prisonerNumber, chargeId)
    } else {
      // TODO: get data from this database!
      AdjudicationDetail(adjudicationNumber = chargeId)
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
          results = PageImpl(it.results, PageRequest.of(pageOffset.toInt() / pageSize.toInt(), pageSize.toInt()), totalRecords.toLong()),
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

  fun getAdjudicationSummary(bookingId: Long, awardCutoffDate: LocalDate?, adjudicationCutoffDate: LocalDate?): AdjudicationSummary {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getAdjudicationsForPrisonerForBooking(bookingId, awardCutoffDate, adjudicationCutoffDate)
    } else {
      // TODO: get data from this database!
      AdjudicationSummary(bookingId = bookingId, adjudicationCount = 0, awards = listOf())
    }
  }

  fun getProvenAdjudicationsForBookings(bookingIds: List<Long>, awardCutoffDate: LocalDate?, adjudicationCutoffDate: LocalDate?): List<ProvenAdjudicationsSummary> {
    return if (featureFlagsService.isNomisSourceOfTruth()) {
      legacyNomisGateway.getProvenAdjudicationsForBookings(bookingIds, awardCutoffDate, adjudicationCutoffDate)
    } else {
      // TODO: get data from this database!
      listOf()
    }
  }
}
