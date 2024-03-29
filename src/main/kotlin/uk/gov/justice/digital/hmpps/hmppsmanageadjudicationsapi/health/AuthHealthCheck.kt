package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

@Profile("!test")
@Component
class AuthHealthCheck @Autowired constructor(
  @Qualifier("authWebClient") authWebClient: WebClient,
  @Value("\${api.health-timeout-ms}") timeout: Duration,
) : HealthCheck(authWebClient, timeout)
