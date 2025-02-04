package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.auth.authorisedWebClient
import java.time.Duration

@Configuration
class WebClientConfig(
  @Value("\${oauth.endpoint.url}") val authBaseUri: String,
  @Value("\${prison.nomis.location.api.endpoint.url}") val prisonNomisBaseUri: String,
  @Value("\${prison.location.api.endpoint.url}") val prisonLocationDetailBaseUri: String,
  @Value("\${prison.prisoner-search.api.endpoint.url}") val prisonerSearchBaseUri: String,
  @Value("\${prison.timeout:30s}") private val apiTimeout: Duration,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Bean
  fun authWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(authBaseUri)
      .build()
  }

  @Bean("prisonLocationWebClient")
  fun prisonLocationWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "prison-nomis-api", prisonNomisBaseUri, apiTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating prison nomis api web client") }

  @Bean("prisonLocationDetailWebClient")
  fun prisonLocationDetailWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "prison-location-api", prisonLocationDetailBaseUri, apiTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating manage prison location api web client") }

  @Bean("prisonerSearchWebClient")
  fun prisonerSearchWebClient(authorizedClientManager: OAuth2AuthorizedClientManager, builder: WebClient.Builder) = builder
    .authorisedWebClient(authorizedClientManager, "prisoner-search-api", prisonerSearchBaseUri, apiTimeout)
    .also { log.info("WEB CLIENT CONFIG: creating prisoner search api web client") }
}
