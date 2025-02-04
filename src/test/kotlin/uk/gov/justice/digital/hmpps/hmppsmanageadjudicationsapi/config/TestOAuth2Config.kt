package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository

@TestConfiguration
class TestOAuth2Config {

  @Bean
  fun clientRegistrationRepository(): ClientRegistrationRepository {
    return mock()
  }

  @Bean
  fun authorizedClientRepository(): OAuth2AuthorizedClientRepository {
    return mock()
  }

  @Bean
  fun authorizedClientManager(
    clientRegistrationRepository: ClientRegistrationRepository,
    authorizedClientRepository: OAuth2AuthorizedClientRepository,
  ): OAuth2AuthorizedClientManager {
    return mock()
  }
}
