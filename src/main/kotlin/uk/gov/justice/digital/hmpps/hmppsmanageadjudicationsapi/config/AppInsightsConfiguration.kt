package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppInsightsConfiguration {
  @Bean
  @ConditionalOnExpression("T(org.apache.commons.lang3.StringUtils).isBlank('\${applicationinsights.connection.string:}')")
  fun telemetryClient(): TelemetryClient {
    log.warn("Application insights configuration missing, returning dummy bean instead")

    return TelemetryClient()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
