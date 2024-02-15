package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
  @Value("\${oauth.endpoint.url}") val authBaseUri: String,
) {

  @Bean
  fun authWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(authBaseUri)
      .build()
  }
}
