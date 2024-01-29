package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import java.time.LocalDate
import java.util.Optional

enum class Finding {
  APPEAL, D, DISMISSED, GUILTY, NOT_GUILTY, NOT_PROCEED, NOT_PROVEN, PROSECUTED, PROVED, QUASHED, REFUSED, REF_POLICE, S, UNFIT, ADJOURNED,
}

@Service
class LegacyNomisGateway(private val prisonApiClientCreds: WebClient) {

  fun getAdjudicationsForPrisoner(
    prisonerNumber: String,
    offenceId: Long?,
    agencyId: String?,
    finding: Finding?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): ResponseEntity<AdjudicationResponse> =
    prisonApiClientCreds
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/offenders/$prisonerNumber/adjudications")
          .queryParamIfPresent("offenceId", Optional.ofNullable(offenceId))
          .queryParamIfPresent("agencyId", Optional.ofNullable(agencyId))
          .queryParamIfPresent("finding", Optional.ofNullable(finding))
          .queryParamIfPresent("fromDate", Optional.ofNullable(fromDate))
          .queryParamIfPresent("toDate", Optional.ofNullable(toDate))
          .build()
      }
      .headers { httpHeaders -> httpHeaders.addAll(populateHeaders(pageable)) }
      .retrieve()
      .toEntity(AdjudicationResponse::class.java)
      .block()!!

  private fun populateHeaders(
    pageable: Pageable,
  ): LinkedMultiValueMap<String, String> {
    val headers = LinkedMultiValueMap<String, String>()
    headers.add("Page-Offset", pageable.offset.toString())
    headers.add("Page-Limit", pageable.pageSize.toString())
    return headers
  }

  fun getAdjudicationDetailForPrisoner(prisonerNumber: String, chargeId: Long) =
    prisonApiClientCreds
      .get()
      .uri("/offenders/$prisonerNumber/adjudications/$chargeId")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<AdjudicationDetail>() {})
      .block()!!

  fun getAdjudicationsForPrisonerForBooking(
    bookingId: Long,
    awardCutoffDate: LocalDate?,
    adjudicationCutoffDate: LocalDate?,
  ) =
    prisonApiClientCreds
      .get()
      .uri { uriBuilder ->
        uriBuilder.path("/bookings/$bookingId/adjudications")
          .queryParamIfPresent("awardCutoffDate", Optional.ofNullable(awardCutoffDate))
          .queryParamIfPresent("adjudicationCutoffDate", Optional.ofNullable(adjudicationCutoffDate))
          .build()
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<AdjudicationSummary>() {})
      .block()!!
}
