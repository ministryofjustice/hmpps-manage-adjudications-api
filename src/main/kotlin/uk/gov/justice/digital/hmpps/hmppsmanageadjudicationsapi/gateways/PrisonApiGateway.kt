package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.pagination.RestResponsePage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.pagination.getPageableUrlParameters

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun getReportedAdjudication(adjudicationNumber: Long): ReportedAdjudication = prisonApiClientCreds
    .get()
    .uri("/adjudications/adjudication/$adjudicationNumber")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<ReportedAdjudication>() {})
    .block()!!

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish): ReportedAdjudication =
    prisonApiClientCreds
      .post()
      .uri("/adjudications/adjudication")
      .bodyValue(adjudicationDetailsToPublish)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<ReportedAdjudication>() {})
      .block()!!

  fun updateAdjudication(adjudicationNumber: Long, adjudicationDetailsToUpdate: AdjudicationDetailsToUpdate): ReportedAdjudication =
    prisonApiClientCreds
      .put()
      .uri("/adjudications/adjudication/$adjudicationNumber")
      .bodyValue(adjudicationDetailsToUpdate)
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<ReportedAdjudication>() {})
      .block()!!

  fun getReportedAdjudications(adjudicationNumbers: Collection<Long>): List<ReportedAdjudication> = prisonApiClientCreds
    .post()
    .uri("/adjudications")
    .bodyValue(adjudicationNumbers)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<ReportedAdjudication>>() {})
    .block()!!

  fun search(request: ReportedAdjudicationRequest, pageable: Pageable): Page<ReportedAdjudication> = prisonApiClientCreds
    .post()
    .uri("/adjudications/search?" + getPageableUrlParameters(pageable))
    .bodyValue(request)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<RestResponsePage<ReportedAdjudication>>() {})
    .block()!!
}
