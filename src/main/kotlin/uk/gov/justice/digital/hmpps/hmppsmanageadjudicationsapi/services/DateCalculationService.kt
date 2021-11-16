package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class DateCalculationService(
  val bankHolidayApiGateway: BankHolidayApiGateway,
) {
  fun calculate48WorkingHoursFrom(dateTimeOfIncident: LocalDateTime): LocalDateTime {
    val bankHolidayDetails = bankHolidayApiGateway.getBankHolidays()
    val bankHolidays = bankHolidayDetails.englandAndWales.events
    val bankHolidayDays = bankHolidays.map { it.date }

    return find48HoursFrom(dateTimeOfIncident, bankHolidayDays)
  }

  private fun find48HoursFrom(startDateTime: LocalDateTime, bankHolidayDays: List<LocalDate>): LocalDateTime {
    var numberOfWorkingDaysFound = 0
    var currentDateTime = startDateTime
    do {
      // We ignore the start day so increment first
      currentDateTime = currentDateTime.plusDays(1)
      if (currentDateTime.dayOfWeek != DayOfWeek.SUNDAY && !bankHolidayDays.contains(currentDateTime.toLocalDate()))
        numberOfWorkingDaysFound++
    } while (numberOfWorkingDaysFound < 2)

    return currentDateTime
  }
}
