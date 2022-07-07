package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayFacade

@ActiveProfiles("test")
@SpringBootTest
class BankholidayTest @Autowired constructor(
  val bankHolidayFacade: BankHolidayFacade
) {

  @Test
  fun `testing`(){
    IntRange(1, 100).forEach {
      assertThat(bankHolidayFacade.getBankHolidays().englandAndWales.events.size).isEqualTo(57)
      println("got data")
    }

    Thread.sleep(1000)
  }
}