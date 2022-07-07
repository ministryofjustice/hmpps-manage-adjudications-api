package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BankHolidayApiGateway(@Qualifier("bankHolidayApiWebClient") private val webClient: WebClient) {
  private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

  fun getBankHolidays(): BankHolidays {
    println("getting actual data")
    return webClient.get()
      .uri("/bank-holidays.json")
      .retrieve()
      .bodyToMono(typeReference<BankHolidays>())
      .block()!!
  }
}
