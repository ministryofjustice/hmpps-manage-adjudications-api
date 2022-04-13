package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import javax.persistence.EntityNotFoundException

class ReportedAdjudicationServiceTest {
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val offenceCodeLookupService: OffenceCodeLookupService = mock()
  private val authenticationFacade: AuthenticationFacade = mock()
  private lateinit var reportedAdjudicationService: ReportedAdjudicationService

  @BeforeEach
  fun beforeEach() {
    whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")

    reportedAdjudicationService =
      ReportedAdjudicationService(
        draftAdjudicationRepository,
        reportedAdjudicationRepository,
        offenceCodeLookupService,
        authenticationFacade
      )

    whenever(offenceCodeLookupService.getParagraphNumber(2)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getParagraphNumber(3)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
  }

  @Nested
  inner class ReportedAdjudicationDetails {
    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `returns the reported adjudication`() {
      val reportedAdjudication =
        ReportedAdjudication(
          reportNumber = 1, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
          incidentRoleCode = "25b", incidentRoleAssociatedPrisonersNumber = "BB2345B",
          offenceDetails = mutableListOf(
            ReportedOffence(
              offenceCode = 2,
              paragraphCode = OFFENCE_CODE_2_PARAGRAPH_CODE,
            ),
            ReportedOffence(
              offenceCode = 3,
              paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
              victimPrisonersNumber = "BB2345B",
              victimStaffUsername = "DEF34G",
              victimOtherPersonsName = "Another Name",
            ),
          ),
          handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
          status = ReportedAdjudicationStatus.AWAITING_REVIEW,
          statusReason = null,
          statusDetails = null,
        )
      reportedAdjudication.createdByUserId = "A_SMITH" // Add audit information
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime")
        .contains(1L, "AA1234A", 123L, "A_SMITH", REPORTED_DATE_TIME)

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(345L, DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)

      assertThat(reportedAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains("25b", IncidentRoleRuleLookup.getOffenceRuleDetails("25b"), "BB2345B")

      assertThat(reportedAdjudicationDto.offenceDetails)
        .extracting(
          "offenceCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "victimPrisonersNumber",
          "victimStaffUsername",
          "victimOtherPersonsName"
        )
        .contains(
          Tuple(2, OFFENCE_CODE_2_PARAGRAPH_NUMBER, OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION, null, null, null),
          Tuple(
            3,
            OFFENCE_CODE_3_PARAGRAPH_NUMBER,
            OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION,
            "BB2345B",
            "DEF34G",
            "Another Name"
          ),
        )

      assertThat(reportedAdjudicationDto.incidentStatement)
        .extracting("statement", "completed")
        .contains(INCIDENT_STATEMENT, true)
    }
  }

  @Nested
  inner class AllReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 = ReportedAdjudication(
        id = 1,
        prisonerNumber = "AA1234A",
        bookingId = 123,
        reportNumber = 1,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 3,
            paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
            victimPrisonersNumber = "BB2345B",
            victimStaffUsername = "DEF34G",
            victimOtherPersonsName = "Another Name",
          )
        ),
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
      )
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 = ReportedAdjudication(
        id = 2,
        prisonerNumber = "AA1234B",
        bookingId = 456,
        reportNumber = 2,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        offenceDetails = null,
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
      )
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
          any(),
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
    }

    @Test
    fun `makes a call to the reported adjudication repository to get the page of adjudications`() {
      reportedAdjudicationService.getAllReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        Optional.empty(),
        Pageable.ofSize(20).withPage(0)
      )

      verify(reportedAdjudicationRepository).findByAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
        "MDI",
        LocalDate.now().atStartOfDay(),
        LocalDate.now().atTime(LocalTime.MAX),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(20).withPage(0)
      )
    }

    @Test
    fun `returns all reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getAllReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        Optional.empty(),
        Pageable.ofSize(20).withPage(0)
      )

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "AA1234B", 456L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 = ReportedAdjudication(
        id = 1,
        prisonerNumber = "AA1234A",
        bookingId = 123,
        reportNumber = 1,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 3,
            paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
            victimPrisonersNumber = "BB2345B",
            victimStaffUsername = "DEF34G",
            victimOtherPersonsName = "Another Name",
          )
        ),
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
      )
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 = ReportedAdjudication(
        id = 2,
        prisonerNumber = "AA1234B",
        bookingId = 456,
        reportNumber = 2,
        agencyId = "MDI",
        locationId = 345,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_OF_INCIDENT.plusDays(2),
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        offenceDetails = null,
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
      )
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
          any(),
          any(),
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        PageImpl(
          listOf(reportedAdjudication1, reportedAdjudication2)
        )
      )
    }

    @Test
    fun `returns my reported adjudications`() {
      val myReportedAdjudications = reportedAdjudicationService.getMyReportedAdjudications(
        "MDI",
        LocalDate.now(),
        LocalDate.now(),
        Optional.empty(),
        Pageable.ofSize(20).withPage(0)
      )

      assertThat(myReportedAdjudications.content)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime")
        .contains(
          Tuple.tuple(1L, "AA1234A", 123L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "AA1234B", 456L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class ReportedAdjudicationSetStatus {
    private fun reportedAdjudication(): ReportedAdjudication {
      val reportedAdjudication = ReportedAdjudication(
        reportNumber = 123, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
        incidentRoleCode = "25b", incidentRoleAssociatedPrisonersNumber = "BB2345B",
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 3,
            paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
            victimPrisonersNumber = "BB2345B",
            victimStaffUsername = "DEF34G",
            victimOtherPersonsName = "Another Name",
          )
        ),
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
      )
      reportedAdjudication.createdByUserId = "A_SMITH"
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME
      return reportedAdjudication
    }

    @ParameterizedTest
    @CsvSource(
      "ACCEPTED, ACCEPTED",
      "ACCEPTED, REJECTED",
      "ACCEPTED, AWAITING_REVIEW",
      "ACCEPTED, RETURNED",
      "REJECTED, ACCEPTED",
      "REJECTED, REJECTED",
      "REJECTED, AWAITING_REVIEW",
      "REJECTED, RETURNED",
      "RETURNED, ACCEPTED",
      "RETURNED, REJECTED",
      "RETURNED, RETURNED"
    )
    fun `setting status for a reported adjudication throws an illegal state exception for invalid transitions`(
      from: ReportedAdjudicationStatus,
      to: ReportedAdjudicationStatus
    ) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication().also {
          it.status = from
        }
      )
      Assertions.assertThrows(IllegalStateException::class.java) {
        reportedAdjudicationService.setStatus(1, to)
      }
    }

    @ParameterizedTest
    @CsvSource(
      "AWAITING_REVIEW, ACCEPTED",
      "AWAITING_REVIEW, REJECTED",
      "AWAITING_REVIEW, RETURNED",
      "AWAITING_REVIEW, AWAITING_REVIEW",
      "RETURNED, AWAITING_REVIEW"
    )
    fun `setting status for a reported adjudication for valid transitions`(
      from: ReportedAdjudicationStatus,
      to: ReportedAdjudicationStatus
    ) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication().also {
          it.status = from
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication().also { it.status = to })
      reportedAdjudicationService.setStatus(1, to)
      verify(reportedAdjudicationRepository).save(reportedAdjudication().also { it.status = to })
    }
  }

  @Nested
  inner class CreateDraftFromReported {
    private val reportedAdjudication = ReportedAdjudication(
      reportNumber = 123, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
      dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
      incidentRoleCode = "25b", incidentRoleAssociatedPrisonersNumber = "BB2345B",
      offenceDetails = mutableListOf(
        ReportedOffence(
          offenceCode = 3,
          paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
          victimPrisonersNumber = "BB2345B",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Another Name",
        )
      ),
      handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
      status = ReportedAdjudicationStatus.AWAITING_REVIEW,
      statusReason = null,
      statusDetails = null,
    )

    private val expectedSavedDraftAdjudication = DraftAdjudication(
      prisonerNumber = "AA1234A",
      reportNumber = 123L,
      reportByUserId = "A_SMITH",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 345L,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
      ),
      incidentRole = IncidentRole(
        roleCode = "25b",
        associatedPrisonersNumber = "BB2345B",
      ),
      offenceDetails = mutableListOf(
        Offence(
          offenceCode = 3,
          paragraphCode = OFFENCE_CODE_3_PARAGRAPH_CODE,
          victimPrisonersNumber = "BB2345B",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Another Name",
        )
      ),
      incidentStatement = IncidentStatement(
        completed = true,
        statement = INCIDENT_STATEMENT
      )
    )

    private val savedDraftAdjudication = expectedSavedDraftAdjudication.copy(
      id = 1,
    )

    @BeforeEach
    fun beforeEach() {
      reportedAdjudication.createdByUserId = "A_SMITH"
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(draftAdjudicationRepository.save(any())).thenReturn(savedDraftAdjudication)
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.createDraftFromReportedAdjudication(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `adds the relevant draft data to the repository`() {
      reportedAdjudicationService.createDraftFromReportedAdjudication(123)

      verify(draftAdjudicationRepository).save(expectedSavedDraftAdjudication)
    }

    @Test
    fun `returns the correct data`() {
      val createdDraft = reportedAdjudicationService.createDraftFromReportedAdjudication(123)

      assertThat(createdDraft)
        .extracting("prisonerNumber", "id", "adjudicationNumber", "startedByUserId")
        .contains("AA1234A", 1L, 123L, "A_SMITH")
      assertThat(createdDraft.incidentDetails)
        .extracting("dateTimeOfIncident", "handoverDeadline", "locationId")
        .contains(DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, 345L)
      assertThat(createdDraft.incidentRole)
        .extracting(
          "roleCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "associatedPrisonersNumber"
        )
        .contains("25b", "25(b)", "Incites another prisoner to commit any of the foregoing offences:", "BB2345B")
      assertThat(createdDraft.offenceDetails)
        .extracting(
          "offenceCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "victimPrisonersNumber",
          "victimStaffUsername",
          "victimOtherPersonsName"
        )
        .contains(
          Tuple(
            3,
            OFFENCE_CODE_3_PARAGRAPH_NUMBER,
            OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION,
            "BB2345B",
            "DEF34G",
            "Another Name"
          )
        )
      assertThat(createdDraft.incidentStatement)
        .extracting("completed", "statement")
        .contains(true, INCIDENT_STATEMENT)
    }
  }

  companion object {
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private const val INCIDENT_STATEMENT = "A statement"

    private const val OFFENCE_CODE_2_PARAGRAPH_CODE = "5b"
    private const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    private const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private const val OFFENCE_CODE_3_PARAGRAPH_CODE = "6a"
    private const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"
  }
}
