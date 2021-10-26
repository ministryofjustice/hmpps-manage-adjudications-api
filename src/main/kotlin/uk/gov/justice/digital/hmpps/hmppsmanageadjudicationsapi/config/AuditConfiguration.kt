package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.util.Optional

@Configuration
@EnableJpaAuditing
class AuditConfiguration {
  @Bean
  fun auditorAware(authenticationFacade: AuthenticationFacade): AuditorAware<String> =
    AuditorAware(authenticationFacade)
}

class AuditorAware(private val authenticationFacade: AuthenticationFacade) : AuditorAware<String> {
  override fun getCurrentAuditor(): Optional<String> = Optional.ofNullable(authenticationFacade.currentUsername)
}
