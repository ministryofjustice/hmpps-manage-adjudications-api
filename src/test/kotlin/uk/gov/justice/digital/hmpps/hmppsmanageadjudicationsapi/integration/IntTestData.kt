package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import java.time.LocalDateTime

class IntTestData {

  companion object {
    const val DEFAULT_ADJUDICATION_NUMBER = 1524242L
    const val DEFAULT_PRISONER_NUMBER = "AA1234A"
    const val DEFAULT_PRISONER_BOOKING_ID = 123L
    const val DEFAULT_AGENCY_ID = "MDI"
    val DEFAULT_DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 11, 12, 10, 0)

    const val UPDATED_DATE_TIME_OF_INCIDENT_TEXT = "2010-11-13T10:00:00"
    const val UPDATED_HANDOVER_DEADLINE_TEXT = "2010-11-16T10:00:00"
    const val UPDATED_LOCATION_ID = 721899L
    const val UPDATED_STATEMENT = "updated test statement"
    val UPDATED_DATE_TIME_OF_INCIDENT = DEFAULT_DATE_TIME_OF_INCIDENT.plusDays(1)
  }

}