package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atMost
import org.mockito.kotlin.never
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Status.UP
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration.Companion.BANK_HOLIDAYS_CACHE_NAME
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.RegionBankHolidays
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health.BankHolidayApiHealthCheck

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class BankHolidayCachingTest {

  @MockBean
  lateinit var bankHolidayApiGateway: BankHolidayApiGateway
  @Autowired
  lateinit var cacheConfiguration: CacheConfiguration
  @Autowired
  lateinit var bankHolidayFacade: BankHolidayFacade
  @Autowired
  lateinit var bankHolidayApiHealthCheck: BankHolidayApiHealthCheck

  @BeforeEach
  fun init() {
    `when`(bankHolidayApiGateway.getBankHolidays()).thenReturn(
      BankHolidays(
        englandAndWales = RegionBankHolidays(
          division = "1", events = listOf()
        ),
        scotland = RegionBankHolidays(
          division = "2", events = listOf()
        ),
        northernIreland = RegionBankHolidays(
          division = "3", events = listOf()
        )
      ),
      BankHolidays(
        englandAndWales = RegionBankHolidays(
          division = "4", events = listOf()
        ),
        scotland = RegionBankHolidays(
          division = "5", events = listOf()
        ),
        northernIreland = RegionBankHolidays(
          division = "6", events = listOf()
        )
      )
    )
  }

  @Test
  fun `testing cache eviction and load, with health check to update`() {
    // 1 confirm the cache is empty and we have not called the api
    assertNull(
      cacheConfiguration.cacheManager().getCache(BANK_HOLIDAYS_CACHE_NAME).get(SimpleKey.EMPTY)
    )
    verify(bankHolidayApiGateway, never()).getBankHolidays()

    // 2 call to get data and confirm we have called the api
    assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.division).isEqualTo("1")
    verify(bankHolidayApiGateway, atLeast(1)).getBankHolidays()

    // 3 call to get data and confirm we have called the cache
    assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.division).isEqualTo("1")
    verify(bankHolidayApiGateway, atMost(1)).getBankHolidays()

    // update cache via health check and confirm we have new values in the cache
    assertThat(bankHolidayApiHealthCheck.health().status).isEqualTo(UP)
    assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.division).isEqualTo("4")
    verify(bankHolidayApiGateway, atLeast(2)).getBankHolidays()
  }
}
