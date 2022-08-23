package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatusAudit
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.io.File
import java.io.PrintWriter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuditServiceTest {
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val auditService: AuditService = AuditService(
    reportedAdjudicationRepository, draftAdjudicationRepository,
    ReportDate(now = LocalDate.parse("2022-08-15", DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay())
  )

  private val now = LocalDateTime.now()
  private val draftAdjudication =
    DraftAdjudication(
      id = 1,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2,
        dateTimeOfIncident = now,
        handoverDeadline = now
      ),
      incidentRole = IncidentRole(
        roleCode = "1"
      ),
      offenceDetails = mutableListOf(
        Offence(offenceCode = 10)
      ),
      incidentStatement = IncidentStatement(
        statement = "Example statement",
        completed = false
      ),
      damages = mutableListOf(
        Damage(code = DamageCode.CLEANING, details = "t", reporter = "")
      ),
      evidence = mutableListOf(
        Evidence(code = EvidenceCode.PHOTO, details = "", reporter = "")
      ),
      witnesses = mutableListOf(
        Witness(code = WitnessCode.STAFF, reporter = "", firstName = "", lastName = "")
      ),
      isYouthOffender = true
    )

  private val reportedAdjudication = ReportedAdjudication(
    id = 2,
    prisonerNumber = "AA1234B",
    bookingId = 456,
    reportNumber = 1,
    agencyId = "MDI",
    locationId = 345,
    dateTimeOfIncident = now,
    handoverDeadline = now,
    isYouthOffender = true,
    incidentRoleCode = "21",
    incidentRoleAssociatedPrisonersNumber = null,
    incidentRoleAssociatedPrisonersName = null,
    offenceDetails = mutableListOf(
      ReportedOffence(offenceCode = 1, paragraphCode = "2")
    ),
    statement = "statement",
    status = ReportedAdjudicationStatus.AWAITING_REVIEW,
    statusReason = null,
    statusDetails = null,
    damages = mutableListOf(
      ReportedDamage(code = DamageCode.CLEANING, details = "t", reporter = "")
    ),
    evidence = mutableListOf(
      ReportedEvidence(code = EvidenceCode.PHOTO, details = "", reporter = "")
    ),
    witnesses = mutableListOf(
      ReportedWitness(code = WitnessCode.STAFF, reporter = "", firstName = "", lastName = "")
    ),
    statusAudit = mutableListOf(
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.RETURNED),
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.AWAITING_REVIEW),
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.ACCEPTED),
    )
  )

  private val reportedAdjudication2 = ReportedAdjudication(
    id = 2,
    prisonerNumber = "AA1234B",
    bookingId = 456,
    reportNumber = 2,
    agencyId = "MDI",
    locationId = 345,
    dateTimeOfIncident = now,
    handoverDeadline = now,
    isYouthOffender = true,
    incidentRoleCode = "21",
    incidentRoleAssociatedPrisonersNumber = null,
    incidentRoleAssociatedPrisonersName = null,
    offenceDetails = mutableListOf(
      ReportedOffence(offenceCode = 1, paragraphCode = "2")
    ),
    statement = "statement",
    status = ReportedAdjudicationStatus.AWAITING_REVIEW,
    statusReason = null,
    statusDetails = null,
    damages = mutableListOf(
      ReportedDamage(code = DamageCode.REDECORATION, details = "t", reporter = "")
    ),
    evidence = mutableListOf(
      ReportedEvidence(code = EvidenceCode.PHOTO, details = "", reporter = "")
    ),
    witnesses = mutableListOf(
      ReportedWitness(code = WitnessCode.STAFF, reporter = "", firstName = "", lastName = "")
    ),
    statusAudit = mutableListOf(
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.RETURNED, statusReason = "reason", statusDetails = "details"),
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.AWAITING_REVIEW),
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.ACCEPTED),
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.RETURNED),
    )
  )

  private val reportedAdjudication3 = ReportedAdjudication(
    id = 2,
    prisonerNumber = "AA1234B",
    bookingId = 456,
    reportNumber = 3,
    agencyId = "MDI",
    locationId = 345,
    dateTimeOfIncident = now,
    handoverDeadline = now,
    isYouthOffender = true,
    incidentRoleCode = "21",
    incidentRoleAssociatedPrisonersNumber = null,
    incidentRoleAssociatedPrisonersName = null,
    offenceDetails = mutableListOf(
      ReportedOffence(offenceCode = 1, paragraphCode = "2")
    ),
    statement = "statement",
    status = ReportedAdjudicationStatus.AWAITING_REVIEW,
    statusReason = null,
    statusDetails = null,
    damages = mutableListOf(),
    evidence = mutableListOf(),
    witnesses = mutableListOf(),
    statusAudit = mutableListOf()
  )
  // should be picked up as reviewed this week, created before report date
  private val reportedAdjudication4 = ReportedAdjudication(
    id = 2,
    prisonerNumber = "AA1234B",
    bookingId = 456,
    reportNumber = 4,
    agencyId = "MDI",
    locationId = 345,
    dateTimeOfIncident = now,
    handoverDeadline = now,
    isYouthOffender = true,
    incidentRoleCode = "21",
    incidentRoleAssociatedPrisonersNumber = null,
    incidentRoleAssociatedPrisonersName = null,
    offenceDetails = mutableListOf(
      ReportedOffence(offenceCode = 1, paragraphCode = "2")
    ),
    statement = "statement",
    status = ReportedAdjudicationStatus.ACCEPTED,
    statusReason = null,
    statusDetails = null,
    damages = mutableListOf(),
    evidence = mutableListOf(),
    witnesses = mutableListOf(),
    statusAudit = mutableListOf(
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.ACCEPTED),
    )
  )
  // should not be picked up as wrong review status
  private val reportedAdjudication5 = ReportedAdjudication(
    id = 2,
    prisonerNumber = "AA1234B",
    bookingId = 456,
    reportNumber = 5,
    agencyId = "MDI",
    locationId = 345,
    dateTimeOfIncident = now,
    handoverDeadline = now,
    isYouthOffender = true,
    incidentRoleCode = "21",
    incidentRoleAssociatedPrisonersNumber = null,
    incidentRoleAssociatedPrisonersName = null,
    offenceDetails = mutableListOf(
      ReportedOffence(offenceCode = 1, paragraphCode = "2")
    ),
    statement = "statement",
    status = ReportedAdjudicationStatus.AWAITING_REVIEW,
    statusReason = null,
    statusDetails = null,
    damages = mutableListOf(),
    evidence = mutableListOf(),
    witnesses = mutableListOf(),
    statusAudit = mutableListOf(
      ReportedAdjudicationStatusAudit(status = ReportedAdjudicationStatus.AWAITING_REVIEW),
    )
  )

  @BeforeEach
  fun `init`() {
    draftAdjudication.createdByUserId = "Fred"
    draftAdjudication.createDateTime = now

    reportedAdjudication.statusAudit.forEach {
      it.createdByUserId = "Fred"
      it.createDateTime = now
    }

    reportedAdjudication2.statusAudit.forEach {
      it.createdByUserId = "Fred"
      it.createDateTime = now
    }

    reportedAdjudication3.createdByUserId = "Fred"
    reportedAdjudication3.createDateTime = now

    reportedAdjudication4.createdByUserId = "Fred"
    reportedAdjudication4.createDateTime = now.minusWeeks(2)

    reportedAdjudication4.statusAudit.forEach {
      it.createdByUserId = "Fred"
      it.createDateTime = now
    }

    reportedAdjudication5.createdByUserId = "Fred"
    reportedAdjudication5.createDateTime = now.minusWeeks(2)

    reportedAdjudication5.statusAudit.forEach {
      it.createdByUserId = "Fred"
      it.createDateTime = now
    }
  }

  @ParameterizedTest
  @CsvSource("true,false")
  fun `get draft adjudications report`(historic: Boolean) {
    whenever(draftAdjudicationRepository.findByCreateDateTimeAfterAndReportNumberIsNull(any())).thenReturn(listOf(draftAdjudication))

    val printWriter = PrintWriter("draft.csv")
    auditService.getDraftAdjudicationReport(printWriter, historic)

    val lines = File("draft.csv").bufferedReader().readLines()

    assertThat(
      lines.containsAll(
        listOf(
          AuditService.DRAFT_ADJUDICATION_CSV_HEADERS,
          "MDI,Fred,$now,A12345,$now,2,true,1,\"[10]\",\"[CLEANING]\",\"[PHOTO]\",\"[STAFF]\",false,\"Example statement\""
        )
      )
    ).isEqualTo(true)
  }

  @ParameterizedTest
  @CsvSource("true,false")
  fun `get reported adjudications report`(historic: Boolean) {
    whenever(reportedAdjudicationRepository.findByCreateDateTimeAfter(any())).thenReturn(listOf(reportedAdjudication, reportedAdjudication2, reportedAdjudication3))
    whenever(reportedAdjudicationRepository.findByCreateDateTimeBefore(any())).thenReturn(listOf(reportedAdjudication4, reportedAdjudication5))

    val printWriter = PrintWriter("reported.csv")
    auditService.getReportedAdjudicationReport(printWriter, historic)

    File("reported.csv").bufferedReader().readLines()

    val lines = File("reported.csv").bufferedReader().readLines()

    val results = mutableListOf(
      AuditService.REPORTED_ADJUDICATION_CSV_HEADERS,
      "1,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[CLEANING]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",RETURNED,null,\"null\",1",
      "1,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[CLEANING]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",AWAITING_REVIEW,null,\"null\",1",
      "1,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[CLEANING]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",ACCEPTED,null,\"null\",1",
      "3,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[]\",\"[]\",\"[]\",\"statement\",AWAITING_REVIEW,null,\"null\",0",
      "2,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[REDECORATION]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",RETURNED,reason,\"details\",2",
      "2,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[REDECORATION]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",ACCEPTED,null,\"null\",2",
      "2,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[REDECORATION]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",AWAITING_REVIEW,null,\"null\",2",
      "2,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[REDECORATION]\",\"[PHOTO]\",\"[STAFF]\",\"statement\",RETURNED,null,\"null\",2"
    )
    if (!historic) {
      results.add("4,MDI,Fred,$now,AA1234B,$now,345,true,21,\"[(2, 1)]\",\"[]\",\"[]\",\"[]\",\"statement\",ACCEPTED,null,\"null\",0")
      assertThat(
        lines.containsAll(results)
      ).isEqualTo(true)
    } else {
      lines.containsAll(results)
    }
  }

  @ParameterizedTest
  @CsvSource("2022-08-15", "2022-08-16", "2022-08-17", "2022-08-18", "2022-08-19", "2022-08-20", "2022-08-21")
  fun `get start of week as a monday`(date: String) {
    val reportDate = ReportDate(LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay())
    assertThat(reportDate.getStartOfWeek().dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @AfterEach
  fun `tidy up`() {
    File("draft.csv").delete()
    File("reported.csv").delete()
  }
}
