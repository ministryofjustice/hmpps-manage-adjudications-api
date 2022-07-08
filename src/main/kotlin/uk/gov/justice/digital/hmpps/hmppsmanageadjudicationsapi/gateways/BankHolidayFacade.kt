package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration

@Component
class BankHolidayFacade @Autowired constructor(
  private val bankHolidayApiGateway: BankHolidayApiGateway
) {
  @Cacheable(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME)
  fun getBankHolidays(): BankHolidays = bankHolidayApiGateway.getBankHolidays()

  @CachePut(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME)
  fun updateCache(): BankHolidays = bankHolidayApiGateway.getBankHolidays()
}
