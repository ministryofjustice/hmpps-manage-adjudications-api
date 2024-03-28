package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health

import org.slf4j.LoggerFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Duration

abstract class HealthCheck(private val webClient: WebClient, private val timeout: Duration) : HealthIndicator {
  override fun health(): Health = try {
    val responseEntity = webClient.get()
      .uri("/health/ping")
      .retrieve()
      .toEntity(String::class.java)
      .block(timeout)
    Health.up().withDetail("HttpStatus", responseEntity?.statusCode).build()
  } catch (e: WebClientResponseException) {
    log.info(e.message)
    Health.down(e).withDetail("body", e.responseBodyAsString).build()
  } catch (e: Exception) {
    log.info(e.message)
    Health.down(e).build()
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
