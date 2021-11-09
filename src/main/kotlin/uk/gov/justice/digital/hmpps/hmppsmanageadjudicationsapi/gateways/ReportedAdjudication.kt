package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.IncidentStatementDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import java.time.LocalDateTime

data class ReportedAdjudication(
  val adjudicationNumber: Long,
  val reporterStaffId: Long,
  val offenderNo: String,
  val bookingId: Long,
  val offenderNo: String,
  val incidentTime: LocalDateTime,
  val incidentLocationId: Long,
  val statement: String
) {
  fun toDto(): ReportedAdjudicationDto =
    ReportedAdjudicationDto(
      adjudicationNumber = adjudicationNumber,
      prisonerNumber = offenderNo,
      bookingId = bookingId,
      incidentDetails = IncidentDetailsDto(
        locationId = incidentLocationId,
        dateTimeOfIncident = incidentTime
      ),
      incidentStatement = IncidentStatementDto(
        statement = statement
      )
    )
}
