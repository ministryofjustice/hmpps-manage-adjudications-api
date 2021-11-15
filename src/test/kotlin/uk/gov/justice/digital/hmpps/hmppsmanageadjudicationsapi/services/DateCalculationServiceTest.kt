package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHoliday
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidays
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.RegionBankHolidays
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

class DateCalculationServiceTest {
  private val bankHolidayApi: BankHolidayApiGateway = mock()
  private lateinit var dateCalculationService: DateCalculationService

  @BeforeEach
  fun beforeEach() {
    dateCalculationService =
      DateCalculationService(bankHolidayApi)

    whenever(bankHolidayApi.getBankHolidays()).thenReturn(
      BankHolidays(
        RegionBankHolidays(
          "england-and-wales",
          listOf(
            BankHoliday(
              "Test Bank Holiday",
              LocalDate.of(2021, 12, 14)
            ),
            BankHoliday(
              "Test Bank Holiday",
              LocalDate.of(2021, 12, 27)
            ),
            BankHoliday(
              "Test Bank Holiday",
              LocalDate.of(2021, 12, 28)
            )
          )
        ),
        RegionBankHolidays(
          "scotland",
          listOf()
        ),
        RegionBankHolidays(
          "northern-ireland",
          listOf()
        )
      )
    )
  }

  @ParameterizedTest
  @MethodSource("providedAndExpectedCalculatedDateTime")
  fun `check calculations`(providedDateTime: LocalDateTime, expectedDaysAdded: Long) {
    val actualCalculatedDateTime = dateCalculationService.calculate48WorkingHoursFrom(providedDateTime)
    assertThat(actualCalculatedDateTime).isEqualTo(providedDateTime.plusDays(expectedDaysAdded))
  }

  companion object {
    @JvmStatic
    fun providedAndExpectedCalculatedDateTime(): Stream<Arguments?>? =
      Stream.of(
        // Normal working week
        Arguments.arguments(LocalDateTime.of(2021, 12, 1, 10, 0), 2),
        // Reported on a Sunday
        Arguments.arguments(LocalDateTime.of(2021, 12, 5, 10, 0), 2),
        // Spans a Sunday
        Arguments.arguments(LocalDateTime.of(2021, 12, 4, 10, 0), 3),
        // Spanning a bank holidays
        Arguments.arguments(LocalDateTime.of(2021, 12, 13, 10, 0), 3),
        // Spanning a couple of bank holidays and a Sunday
        Arguments.arguments(LocalDateTime.of(2021, 12, 24, 10, 0), 5)
      )
  }
}
