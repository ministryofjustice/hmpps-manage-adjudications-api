package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PrisonApiGateway(private val prisonApiClientCreds: WebClient) {
  fun getReportedAdjudication(adjudicationNumber: Long): ReportedAdjudication = prisonApiClientCreds
    .get()
    .uri("/adjudications/adjudication/$adjudicationNumber")
    .retrieve()
    .bodyToMono(object : ParameterizedTypeReference<ReportedAdjudication>() {})
    .block()!!
}
