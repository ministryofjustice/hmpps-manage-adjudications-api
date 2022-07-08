package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableCaching
class CacheConfiguration {

  @Bean
  fun cacheManager(): CacheManager {
    return ConcurrentMapCacheManager(BANK_HOLIDAYS_CACHE_NAME)
  }

  companion object {
    const val BANK_HOLIDAYS_CACHE_NAME: String = "bankHolidays"
  }
}
