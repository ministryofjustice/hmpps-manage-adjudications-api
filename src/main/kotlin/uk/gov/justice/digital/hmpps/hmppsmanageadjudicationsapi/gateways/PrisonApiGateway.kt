package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

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

  fun updateAdjudication(adjudicationNumber: Long, adjudicationDetailsToUpdate: AdjudicationDetailsToUpdate): NomisAdjudication =
    prisonApiClientCreds
      .put()
      .uri("/adjudications/adjudication/$adjudicationNumber")
      .bodyValue(adjudicationDetailsToUpdate)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<NomisAdjudication>() {})
      .block()!!
}
