package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class ActivePrisonConfig(
  @Value("\${service.active.prisons:}")
  val activePrisons: String,
)
