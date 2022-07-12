package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Status
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.RegionBankHolidays
import java.lang.RuntimeException

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class BankHolidayApiHealthCheckTest {

  private val bankHolidays =
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

  @MockBean
  lateinit var bankHolidayApiGateway: BankHolidayApiGateway
  @Autowired
  lateinit var cacheConfiguration: CacheConfiguration
  @Autowired
  lateinit var bankHolidayApiHealthCheck: BankHolidayApiHealthCheck

  @Test
  fun `expect health DOWN down when gateway is down and cache is empty`() {
    Mockito.`when`(bankHolidayApiGateway.getBankHolidays()).thenThrow(RuntimeException())
    assertThat(bankHolidayApiHealthCheck.health().status).isEqualTo(Status.DOWN)
  }

  @Test
  fun `expect health UP when gateway is down and cache is not empty`() {
    Mockito.`when`(bankHolidayApiGateway.getBankHolidays()).thenThrow(RuntimeException())
    cacheConfiguration.cacheManager().getCache(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME).put(SimpleKey.EMPTY, bankHolidays)
    assertThat(bankHolidayApiHealthCheck.health().status).isEqualTo(Status.UP)
  }

  @Test
  fun `expect health UP when gateway is up and cache is not empty`() {
    Mockito.`when`(bankHolidayApiGateway.getBankHolidays()).thenReturn(bankHolidays)
    cacheConfiguration.cacheManager().getCache(CacheConfiguration.BANK_HOLIDAYS_CACHE_NAME).put(SimpleKey.EMPTY, bankHolidays)
    assertThat(bankHolidayApiHealthCheck.health().status).isEqualTo(Status.UP)
  }
}
