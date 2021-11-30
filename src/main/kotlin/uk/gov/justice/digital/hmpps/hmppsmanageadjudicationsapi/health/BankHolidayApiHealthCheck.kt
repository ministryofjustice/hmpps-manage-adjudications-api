package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway

@Component
class BankHolidayApiHealthCheck @Autowired constructor(
  val bankHolidayApiGateway: BankHolidayApiGateway
) : HealthIndicator {
  override fun health(): Health {
    try {
      bankHolidayApiGateway.getBankHolidays()
    } catch (e: Exception) {
      log.info(e.message)
      return Health.down(e).build()
    }
    return Health.up().build()
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
