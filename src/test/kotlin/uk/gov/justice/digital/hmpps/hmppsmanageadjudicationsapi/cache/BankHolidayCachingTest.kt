package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atMost
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.RegionBankHolidays

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
class BankHolidayCachingTest {

  @MockBean
  lateinit var bankHolidayApiGateway: BankHolidayApiGateway
  @Autowired
  lateinit var bankHolidayFacade: BankHolidayFacade

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
  fun `test facade cache`() {
    // 1 call facade and confirm api is called
    assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.division).isEqualTo("1")
    verify(bankHolidayApiGateway, atLeast(1)).getBankHolidays()

    // 2 update cache and confirm api call
    bankHolidayFacade.updateCache()
    verify(bankHolidayApiGateway, atLeast(2)).getBankHolidays()

    // 3 confirm we use the cache value
    assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.division).isEqualTo("4")
    verify(bankHolidayApiGateway, atMost(2)).getBankHolidays()
  }
}
