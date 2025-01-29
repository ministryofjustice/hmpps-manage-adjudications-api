package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.mockito.Mockito.mock
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager

@TestConfiguration
class TestOAuth2Config {
  @Bean
  fun authorizedClientManager(): OAuth2AuthorizedClientManager {
    // Return a mock to satisfy the dependency
    return mock()
  }
}