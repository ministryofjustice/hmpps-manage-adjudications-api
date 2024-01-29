package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient

@Deprecated("remove post migrate - no more prison api")
@Configuration
class WebClientConfig(
  @Value("\${prison.api.endpoint.url}") private val prisonApiUrl: String,
  @Value("\${oauth.endpoint.url}") val authBaseUri: String,
  val objectMapper: ObjectMapper,
) {

  @Bean
  fun prisonApiNoAuthWebClient(builder: WebClient.Builder): WebClient = builder
    .baseUrl(prisonApiUrl)
    .build()

  @Bean
  fun authWebClient(): WebClient {
    return WebClient.builder()
      .baseUrl(authBaseUri)
      .build()
  }

  @Bean
  fun prisonWebClientClientCredentials(
    @Qualifier(value = "authorizedClientManagerAppScope") authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = getOAuthWebClient(authorizedClientManager, builder, "$prisonApiUrl/api")

  private fun getOAuthWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
    rootUri: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager)
    oauth2Client.setDefaultClientRegistrationId("manage-adjudications-api")
    return builder.baseUrl(rootUri)
      .apply(oauth2Client.oauth2Configuration())
      .build()
  }

  @Bean
  fun authorizedClientManagerAppScope(
    clientRegistrationRepository: ClientRegistrationRepository?,
    oAuth2AuthorizedClientService: OAuth2AuthorizedClientService?,
  ): OAuth2AuthorizedClientManager {
    val authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build()
    val authorizedClientManager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
      clientRegistrationRepository,
      oAuth2AuthorizedClientService,
    )
    authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider)
    return authorizedClientManager
  }
}
