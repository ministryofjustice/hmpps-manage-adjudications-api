package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
  @Value("\${oauth.endpoint.url}") val authBaseUri: String,
  @Value("\${prison.nomis.location.api.endpoint.url}") val prisonNomisBaseUri: String,
  @Value("\${prison.prisoner-search.api.endpoint.url}") val prisonerSearchBaseUri: String,
) {

  @Bean
  fun authWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(authBaseUri)
      .build()
  }

  @Bean
  fun prisonLocationWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(prisonNomisBaseUri)
      .build()
  }

  @Bean
  fun prisonerSearchWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(prisonerSearchBaseUri)
      .build()
  }
}
