package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration.Companion.BANK_HOLIDAYS_CACHE_NAME
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway

@Component
class BankHolidayApiHealthCheck @Autowired constructor(
  val bankHolidayApiGateway: BankHolidayApiGateway,
  val cacheConfiguration: CacheConfiguration
) : HealthIndicator {
  override fun health(): Health {
    try {
      bankHolidayApiGateway.getBankHolidays()
    } catch (e: Exception) {
      log.info(e.message)
      return withCacheCheck(e)
    }
    return withCacheCheck(null)
  }

  fun withCacheCheck(e: Exception?): Health {
    val cache = cacheConfiguration.cacheManager().getCache(BANK_HOLIDAYS_CACHE_NAME) ?: return Health.up().build()
    if (cache.get(SimpleKey.EMPTY) != null) return Health.up().build()

    return Health.down(e ?: Exception("$BANK_HOLIDAYS_CACHE_NAME has no data")).build()
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
