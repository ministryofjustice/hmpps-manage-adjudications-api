package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationDetail
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationSummary
import java.time.LocalDate
import java.util.Optional

@Service
class LegacyReadNomisGateway(@Qualifier(value = "prisonWebClientClientCredentials") private val prisonApiClient: WebClient) {

  fun getAdjudicationsForPrisoner(
    prisonerNumber: String,
    offenceId: Long?,
    agencyId: String?,
    finding: Finding?,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    pageable: Pageable,
  ): ResponseEntity<AdjudicationResponse> =
    prisonApiClient
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
    prisonApiClient
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
    prisonApiClient
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

@Service
class LegacyNomisGateway(@Qualifier(value = "prisonApiClientCreds") private val prisonApiClientCreds: WebClient) {
  fun requestAdjudicationCreationData(): Long =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/request-creation-data")
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<NomisAdjudicationCreationResponse>() {})
      .block()!!.adjudicationNumber

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish): NomisAdjudication =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication")
      .bodyValue(adjudicationDetailsToPublish)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<NomisAdjudication>() {})
      .block()!!

  fun createHearing(adjudicationNumber: Long, oicHearingRequest: OicHearingRequest): Long =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing")
      .bodyValue(oicHearingRequest)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<OicHearingResponse>() {})
      .block()!!.oicHearingId

  fun amendHearing(adjudicationNumber: Long, oicHearingId: Long, oicHearingRequest: OicHearingRequest): Void? =
    prisonApiClientCreds
      .put()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId")
      .bodyValue(oicHearingRequest)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun deleteHearing(adjudicationNumber: Long, oicHearingId: Long): Void? =
    prisonApiClientCreds
      .delete()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId")
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun createHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long,
    oicHearingResultRequest: OicHearingResultRequest,
  ): Void? =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId/result")
      .bodyValue(oicHearingResultRequest)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun hearingOutcomesExistInNomis(adjudicationNumber: Long, oicHearingId: Long): Boolean {
    return try {
      prisonApiClientCreds
        .get()
        .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId/result")
        .retrieve()
        .onStatus({ it.is4xxClientError }, { Mono.empty() })
        .bodyToMono(object : ParameterizedTypeReference<List<OicHearingResult>>() {})
        .block()!!.isNotEmpty()
    } catch (e: Exception) {
      false
    }
  }

  fun amendHearingResult(
    adjudicationNumber: Long,
    oicHearingId: Long,
    oicHearingResultRequest: OicHearingResultRequest,
  ): Void? =
    prisonApiClientCreds
      .put()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId/result")
      .bodyValue(oicHearingResultRequest)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun deleteHearingResult(adjudicationNumber: Long, oicHearingId: Long): Void? =
    prisonApiClientCreds
      .delete()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId/result")
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun createSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>): Void? =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/$adjudicationNumber/sanctions")
      .bodyValue(sanctions)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun updateSanctions(adjudicationNumber: Long, sanctions: List<OffenderOicSanctionRequest>): Void? =
    prisonApiClientCreds
      .put()
      .uri("/adjudications/adjudication/$adjudicationNumber/sanctions")
      .bodyValue(sanctions)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun quashSanctions(adjudicationNumber: Long): Void? = prisonApiClientCreds
    .put()
    .uri("/adjudications/adjudication/$adjudicationNumber/sanctions/quash")
    .retrieve()
    .bodyToMono<Void>()
    .block()

  fun deleteSanctions(adjudicationNumber: Long): Void? = prisonApiClientCreds
    .delete()
    .uri("/adjudications/adjudication/$adjudicationNumber/sanctions")
    .retrieve()
    .bodyToMono<Void>()
    .block()
}

data class OicHearingResult(
  val pleaFindingCode: String,
  val findingCode: String,
)
