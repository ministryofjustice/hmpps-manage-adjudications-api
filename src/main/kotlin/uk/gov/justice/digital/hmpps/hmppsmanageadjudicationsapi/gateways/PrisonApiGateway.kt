package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination.PageResponse

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

  fun getReportedAdjudications(adjudicationNumbers: Set<Long>): List<ReportedAdjudication> = prisonApiClientCreds
    .post()
    .uri("/adjudications")
    .bodyValue(adjudicationNumbers)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<List<ReportedAdjudication>>() {})
    .block()!!

  fun getReportedAdjudications(request: ReportedAdjudicationRequest): PageResponse<ReportedAdjudication> = prisonApiClientCreds
    .post()
    .uri("/adjudications")
    .bodyValue(request)
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<PageResponse<ReportedAdjudication>>() {})
    .block()!!
}
