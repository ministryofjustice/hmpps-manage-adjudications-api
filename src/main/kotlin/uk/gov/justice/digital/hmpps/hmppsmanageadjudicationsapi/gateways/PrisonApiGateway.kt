package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun requestAdjudicationCreationData(offenderNo: String): NomisAdjudicationCreationRequest =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/request-creation-data")
      .bodyValue(offenderNo)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<NomisAdjudicationCreationRequest>() {})
      .block()!!

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

  fun createHearingResult(adjudicationNumber: Long, oicHearingId: Long, oicHearingResultRequest: OicHearingResultRequest): Void? =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication/$adjudicationNumber/hearing/$oicHearingId/result")
      .bodyValue(oicHearingResultRequest)
      .retrieve()
      .bodyToMono<Void>()
      .block()

  fun amendHearingResult(adjudicationNumber: Long, oicHearingId: Long, oicHearingResultRequest: OicHearingResultRequest): Void? =
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
