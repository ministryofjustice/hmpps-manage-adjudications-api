package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.convert.ApplicationConversionService
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository
import uk.gov.justice.hmpps.kotlin.auth.HmppsWebClientConfiguration
import java.util.function.Supplier

class WebClientConfigTest {

  private val contextRunner = WebApplicationContextRunner()
    .withInitializer { it.beanFactory.conversionService = ApplicationConversionService.getSharedInstance() }
    .withBean(ClientRegistrationRepository::class.java, Supplier { mock() })
    .withBean(OAuth2AuthorizedClientRepository::class.java, Supplier { mock() })
    .withBean(OAuth2AuthorizedClientProvider::class.java, Supplier { mock() })
    .withPropertyValues(
      "hmpps.auth.url=http://localhost:8090/auth",
      "prison.nomis.location.api.endpoint.url=http://localhost:8091",
      "prison.location.api.endpoint.url=http://localhost:8092",
      "prison.prisoner-search.api.endpoint.url=http://localhost:8093",
    )
    .withConfiguration(AutoConfigurations.of(HmppsWebClientConfiguration::class.java))
    .withUserConfiguration(WebClientConfig::class.java)

  @Test
  fun `uses request independent OAuth client manager for client credentials calls`() {
    contextRunner.run { context ->
      assertThat(context).hasNotFailed()
      assertThat(context).hasSingleBean(OAuth2AuthorizedClientManager::class.java)
      assertThat(context.getBean(OAuth2AuthorizedClientManager::class.java))
        .isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager::class.java)
    }
  }
}
