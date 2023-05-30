package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

@Service
class LegacyNomisGateway(private val prisonApiClientCreds: WebClient) {
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

  fun createSanction(adjudicationNumber: Long, sanction: OffenderOicSanctionRequest): Long =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/$adjudicationNumber/sanction")
      .bodyValue(sanction)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<OffenderOicSanctionResponse>() {})
      .block()!!.sanctionSeq

  fun deleteSanction(adjudicationNumber: Long, sanctionSeq: Long): Void? = prisonApiClientCreds
    .delete()
    .uri("/adjudications/adjudication/$adjudicationNumber/sanction/$sanctionSeq")
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
