package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayFacade
import java.util.concurrent.TimeUnit

@Configuration
@EnableCaching
@EnableScheduling
class CacheConfiguration @Autowired constructor(
  val bankHolidayFacade: BankHolidayFacade
) {

  @Bean
  fun cacheManager(): CacheManager {
    return ConcurrentMapCacheManager(BANK_HOLIDAYS_CACHE_NAME)
  }

  @Scheduled(fixedDelay = TTL, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
  fun update() {
    bankHolidayFacade.updateCache()
    //  cacheManager().getCache(BANK_HOLIDAYS_CACHE_NAME).put("timestamp",System.currentTimeMillis())
    log.info("updating cache $BANK_HOLIDAYS_CACHE_NAME")
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    const val BANK_HOLIDAYS_CACHE_NAME: String = "bankHolidays"
    const val TTL: Long = 7 * 24 * 60
  }
}
