package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DraftAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.SubmittedAdjudicationHistory
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.BankHolidayApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.SubmittedAdjudicationHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

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
      if (currentDateTime.dayOfWeek != DayOfWeek.SUNDAY
          && !bankHolidayDays.contains(currentDateTime.toLocalDate()))
        numberOfWorkingDaysFound++
    } while (numberOfWorkingDaysFound < 2)

    return currentDateTime
  }
}
