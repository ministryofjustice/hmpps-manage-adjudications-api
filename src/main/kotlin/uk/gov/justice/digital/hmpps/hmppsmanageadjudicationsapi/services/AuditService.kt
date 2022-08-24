package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.io.Writer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.transaction.Transactional

class ReportDate(val now: LocalDateTime = LocalDate.now().atStartOfDay()) {

  fun getStartOfWeek(): LocalDateTime {
    return when (now.dayOfWeek!!) {
      DayOfWeek.MONDAY -> now.minusWeeks(1)
      DayOfWeek.TUESDAY -> now.minusDays(8)
      DayOfWeek.WEDNESDAY -> now.minusDays(9)
      DayOfWeek.THURSDAY -> now.minusDays(10)
      DayOfWeek.FRIDAY -> now.minusDays(11)
      DayOfWeek.SATURDAY -> now.minusDays(12)
      DayOfWeek.SUNDAY -> now.minusWeeks(1).plusDays(1)
    }
  }
}

@Transactional
@Service
class AuditService(
  val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  val draftAdjudicationRepository: DraftAdjudicationRepository,
  val reportDate: ReportDate = ReportDate()
) {
  fun getDraftAdjudicationReport(writer: Writer, historic: Boolean?) {
    val startDate = if (historic != null && historic) LocalDate.parse(
      "2022-01-01", DateTimeFormatter.ISO_LOCAL_DATE
    ).atStartOfDay() else reportDate.getStartOfWeek()

    writer.appendLine(DRAFT_ADJUDICATION_CSV_HEADERS)
    draftAdjudicationRepository.findByCreateDateTimeAfterAndReportNumberIsNull(startDate).forEach {
      writer.appendLine(it.toCsvLine())
    }

    writer.flush()
  }

  fun getReportedAdjudicationReport(writer: Writer, historic: Boolean?) {
    val startDate = if (historic != null && historic) LocalDate.parse(
      "2022-01-01", DateTimeFormatter.ISO_LOCAL_DATE
    ).atStartOfDay() else reportDate.getStartOfWeek()

    writer.appendLine(REPORTED_ADJUDICATION_CSV_HEADERS)
    // reports submitted this week
    reportedAdjudicationRepository.findByCreateDateTimeAfter(startDate).forEach {
      createReportedAdjudicationCsvRows(writer, it)
    }
    // reports submitted before this week but reviewed this week
    if (historic == null || !historic) {
      reportedAdjudicationRepository.findByCreateDateTimeBefore(startDate)
        .filter {
          it.statusAudit.filter { status -> ReportedAdjudicationStatus.AWAITING_REVIEW != status.status }
            .any { status -> status.createDateTime!!.isAfter(startDate) }
        }.forEach {
          createReportedAdjudicationCsvRows(writer, it)
        }
    }
    writer.flush()
  }

  private fun createReportedAdjudicationCsvRows(writer: Writer, reportedAdjudication: ReportedAdjudication) {
    if (reportedAdjudication.statusAudit.isEmpty())
      writer.appendLine(
        reportedAdjudication.toCsvLine(
          returnedCounter = 0, returnedDetailsCounter = 0, returnedOffencesCounter = 0, returnedStatementCounter = 0
        )
      )
    else
      reportedAdjudication.statusAudit.forEach {
        reportedAdjudication.status = it.status
        reportedAdjudication.statusReason = it.statusReason
        reportedAdjudication.statusDetails = it.statusDetails
        reportedAdjudication.modifiedDateTime = it.createDateTime
        reportedAdjudication.modifiedByUserId = it.createdByUserId

        writer.appendLine(
          reportedAdjudication.toCsvLine(
            it.offenceCodes,
            reportedAdjudication.statusAudit.count { status -> status.status == ReportedAdjudicationStatus.RETURNED },
            reportedAdjudication.statusAudit.count { status -> status.statusReason == "offence" },
            reportedAdjudication.statusAudit.count { status -> status.statusReason == "details" },
            reportedAdjudication.statusAudit.count { status -> status.statusReason == "statement" },
          )
        )
      }
  }

  private fun DraftAdjudication.toCsvLine(): String =
    "${this.agencyId},${this.createdByUserId},${this.createDateTime},${this.prisonerNumber},${this.incidentDetails.dateTimeOfIncident}," +
      "${this.incidentDetails.locationId},${this.isYouthOffender},${this.incidentRole?.roleCode}," +
      "\"${this.offenceDetails?.map { it.offenceCode }}\",\"${this.damages?.map { Pair(it.code.name, it.details) }}\"," +
      "\"${this.evidence?.map { Triple(it.code.name, it.details, it.identifier) }}\",\"${this.witnesses?.map { it.code.name }}\",${this.incidentStatement?.completed},\"${this.incidentStatement?.statement}\""

  private fun ReportedAdjudication.toCsvLine(offenceCodes: String? = null, returnedCounter: Int, returnedOffencesCounter: Int, returnedDetailsCounter: Int, returnedStatementCounter: Int): String =
    "${this.reportNumber},${this.agencyId},${this.createdByUserId},${this.createDateTime},${this.modifiedByUserId},${this.modifiedDateTime}," +
      "${this.prisonerNumber},${this.dateTimeOfIncident},${this.locationId},${this.isYouthOffender},${this.incidentRoleCode}," +
      "\"$offenceCodes\",\"${this.damages.map { Pair(it.code.name, it.details) }}\"," +
      "\"${this.evidence.map { Triple(it.code.name, it.details, it.identifier) }}\",\"${this.witnesses.map { it.code.name }}\"," +
      "\"${this.statement}\",${this.status},${this.statusReason},\"${this.statusDetails}\"," +
      "$returnedCounter,$returnedDetailsCounter,$returnedOffencesCounter,$returnedStatementCounter"

  companion object {
    const val DRAFT_ADJUDICATION_CSV_HEADERS = "agency_id,created_by,created_on,prisoner_number,date_of_incident,location_id,youth_offender,role_code,offence_codes,damages,evidence,witnesses,statement_complete,statement"
    const val REPORTED_ADJUDICATION_CSV_HEADERS = "report_number,agency_id,created_by,created_on,updated_by,updated_on,prisoner_number,date_of_incident,location_id,youth_offender,role_code,offence_codes,damages,evidence," +
      "witnesses,statement,status,status_reason,status_details,returned_counter,returned_details_counter,returned_statement_counter,returned_offences_counter"
  }
}
