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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
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
  private val prisonApiGateway: PrisonApiGateway = mock()
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
        prisonApiGateway,
        offenceCodeLookupService,
        authenticationFacade
      )

    whenever(offenceCodeLookupService.getParagraphNumber(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, false)).thenReturn(listOf(OFFENCE_CODE_2_NOMIS_CODE_ON_OWN))
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, false)).thenReturn(OFFENCE_CODE_2_NOMIS_CODE_ASSISTED)

    whenever(offenceCodeLookupService.getParagraphNumber(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, false)).thenReturn(listOf(OFFENCE_CODE_3_NOMIS_CODE_ON_OWN))
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, false)).thenReturn(OFFENCE_CODE_3_NOMIS_CODE_ASSISTED)

    whenever(offenceCodeLookupService.getParagraphNumber(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, true)).thenReturn(listOf(YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN))
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED)

    // TODO - Review whether this is required
    whenever(offenceCodeLookupService.getParagraphNumber(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, true)).thenReturn(listOf(YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN))
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED)
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

    @ParameterizedTest
    @CsvSource(
      "true",
      "false",
    )
    fun `returns the reported adjudication`(
      isYouthOffender: Boolean,
    ) {
      var offenceDetails = mutableListOf(
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
      )
      if (isYouthOffender) {
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 2,
            paragraphCode = YOUTH_OFFENCE_CODE_2_PARAGRAPH_CODE,
          ),
        )
      }

      val reportedAdjudication =
        ReportedAdjudication(
          reportNumber = 1, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
          dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
          isYouthOffender = isYouthOffender,
          incidentRoleCode = "25b",
          incidentRoleAssociatedPrisonersNumber = "BB2345B",
          incidentRoleAssociatedPrisonersName = "Associated Prisoner",
          offenceDetails = offenceDetails,
          handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
          status = ReportedAdjudicationStatus.REJECTED,
          reviewUserId = "A_REVIEWER",
          statusReason = "Status Reason",
          statusDetails = "Status Reason String",
          damages = mutableListOf(),
          evidence = mutableListOf(),
          witnesses = mutableListOf()
        )
      reportedAdjudication.createdByUserId = "A_SMITH" // Add audit information
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting("adjudicationNumber", "prisonerNumber", "bookingId", "createdByUserId", "createdDateTime", "isYouthOffender")
        .contains(1L, "AA1234A", 123L, "A_SMITH", REPORTED_DATE_TIME, isYouthOffender)

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(345L, DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)

      assertThat(reportedAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains("25b", IncidentRoleRuleLookup.getOffenceRuleDetails("25b", isYouthOffender), "BB2345B")

      assertThat(reportedAdjudicationDto)
        .extracting("status", "reviewedByUserId", "statusReason", "statusDetails")
        .contains(ReportedAdjudicationStatus.REJECTED, "A_REVIEWER", "Status Reason", "Status Reason String")

      if (isYouthOffender) {
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
            Tuple(2, YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER, YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION, null, null, null),
          )
      } else {
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
      }

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
        isYouthOffender = false,
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        incidentRoleAssociatedPrisonersName = "Associated Prisoner",
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
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = true,
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        incidentRoleAssociatedPrisonersName = null,
        offenceDetails = null,
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = false,
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        incidentRoleAssociatedPrisonersName = "Associated Prisoner",
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
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = true,
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        incidentRoleAssociatedPrisonersName = null,
        offenceDetails = null,
        statement = INCIDENT_STATEMENT,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = false,
        incidentRoleCode = "25b",
        incidentRoleAssociatedPrisonersNumber = "BB2345B",
        incidentRoleAssociatedPrisonersName = "Associated Prisoner",
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
        reviewUserId = null,
        statusReason = null,
        statusDetails = null,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
      "AWAITING_REVIEW, ACCEPTED, true",
      "AWAITING_REVIEW, REJECTED, false",
      "AWAITING_REVIEW, RETURNED, false",
      "AWAITING_REVIEW, AWAITING_REVIEW, false",
      "RETURNED, AWAITING_REVIEW, false"
    )
    fun `setting status for a reported adjudication for valid transitions`(
      from: ReportedAdjudicationStatus,
      to: ReportedAdjudicationStatus,
      updatesNomis: Boolean,
    ) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication().also {
          it.status = from
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication().also { it.status = to })
      reportedAdjudicationService.setStatus(1, to)
      verify(reportedAdjudicationRepository).save(
        reportedAdjudication().also {
          it.status = to
          it.reviewUserId = if (to == ReportedAdjudicationStatus.AWAITING_REVIEW) null else "ITAG_USER"
        }
      )
      if (updatesNomis) {
        verify(prisonApiGateway).publishAdjudication(any())
      } else {
        verify(prisonApiGateway, never()).publishAdjudication(any())
      }
    }

    @Test
    fun `returns correct status information`() {
      val existingReportedAdjudication = existingReportedAdjudication(true, true, false)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        existingReportedAdjudication
      )

      val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
        it.status = ReportedAdjudicationStatus.REJECTED
        it.statusReason = "Status Reason"
        it.statusDetails = "Status Reason String"
      }
      returnedReportedAdjudication.reviewUserId = "ITAG_USER"
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication
      )

      val actualReturnedReportedAdjudication = reportedAdjudicationService.setStatus(
        1,
        ReportedAdjudicationStatus.REJECTED,
        "Status Reason",
        "Status Reason String",
      )

      verify(reportedAdjudicationRepository).save(returnedReportedAdjudication)
      assertThat(actualReturnedReportedAdjudication.status).isEqualTo(ReportedAdjudicationStatus.REJECTED)
      assertThat(actualReturnedReportedAdjudication.reviewedByUserId).isEqualTo("ITAG_USER")
      assertThat(actualReturnedReportedAdjudication.statusReason).isEqualTo("Status Reason")
      assertThat(actualReturnedReportedAdjudication.statusDetails).isEqualTo("Status Reason String")
    }

    @ParameterizedTest
    @CsvSource(
      "true",
      "false",
    )
    fun `setting status with different roles submits to prison api with correct data`(
      committedOnOwn: Boolean,
    ) {
      val existingReportedAdjudication = existingReportedAdjudication(committedOnOwn, true, false)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        existingReportedAdjudication
      )

      val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication
      )

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.ACCEPTED)

      var expectedOffenceCodes = listOf(OFFENCE_CODE_2_NOMIS_CODE_ON_OWN, OFFENCE_CODE_3_NOMIS_CODE_ON_OWN)
      var expectedConnectedOffenderIds: List<String> = emptyList()
      if (!committedOnOwn) {
        expectedOffenceCodes = listOf(OFFENCE_CODE_2_NOMIS_CODE_ASSISTED, OFFENCE_CODE_3_NOMIS_CODE_ASSISTED)
        expectedConnectedOffenderIds = listOf(INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
      }

      var expectedVictimOffenderIds: List<String> = listOf("A1234AA")
      var expectedVictimStaffUsernames: List<String> = listOf("ABC12D")

      val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
        offenderNo = "A12345",
        adjudicationNumber = 234L,
        bookingId = 123L,
        reporterName = "A_USER",
        reportedDateTime = REPORTED_DATE_TIME,
        agencyId = "MDI",
        incidentLocationId = 345L,
        incidentTime = DATE_TIME_OF_INCIDENT,
        statement = INCIDENT_STATEMENT,
        offenceCodes = expectedOffenceCodes,
        victimOffenderIds = expectedVictimOffenderIds,
        victimStaffUsernames = expectedVictimStaffUsernames,
        connectedOffenderIds = expectedConnectedOffenderIds,
      )
      verify(prisonApiGateway).publishAdjudication(expectedAdjudicationToPublish)
    }

    @ParameterizedTest
    @CsvSource(
      "true",
      "false",
    )
    fun `setting status with different youth offender flag submits to prison api with correct data`(
      isYouthOffender: Boolean,
    ) {
      val existingReportedAdjudication = existingReportedAdjudication(false, false, isYouthOffender)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        existingReportedAdjudication
      )

      val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication
      )

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.ACCEPTED)

      var expectedOffenceCodes = listOf(OFFENCE_CODE_2_NOMIS_CODE_ASSISTED)
      if (isYouthOffender) {
        expectedOffenceCodes =
          listOf(YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED)
      }

      var expectedConnectedOffenderIds = listOf(INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
      var expectedVictimOffenderIds: List<String> = emptyList()
      var expectedVictimStaffUsernames: List<String> = emptyList()

      val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
        offenderNo = "A12345",
        adjudicationNumber = 234L,
        bookingId = 123L,
        reporterName = "A_USER",
        reportedDateTime = REPORTED_DATE_TIME,
        agencyId = "MDI",
        incidentLocationId = 345L,
        incidentTime = DATE_TIME_OF_INCIDENT,
        statement = INCIDENT_STATEMENT,
        offenceCodes = expectedOffenceCodes,
        victimOffenderIds = expectedVictimOffenderIds,
        victimStaffUsernames = expectedVictimStaffUsernames,
        connectedOffenderIds = expectedConnectedOffenderIds,
      )
      verify(prisonApiGateway).publishAdjudication(expectedAdjudicationToPublish)
    }

    private fun existingReportedAdjudication(
      committedOnOwn: Boolean,
      hasSecondOffenceWithAllAssociatedPersonsSet: Boolean,
      isYouthOffender: Boolean,
    ): ReportedAdjudication {
      var incidentRoleCode: String? = null
      var incidentRoleAssociatedPrisonersNumber: String? = null
      var incidentRoleAssociatedPrisonersName: String? = null
      if (!committedOnOwn) {
        incidentRoleCode = INCIDENT_ROLE_CODE
        incidentRoleAssociatedPrisonersNumber = INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER
        incidentRoleAssociatedPrisonersName = INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME
      }

      val offenceDetails = mutableListOf(
        ReportedOffence(
          offenceCode = 2,
          paragraphCode = "5b",
        ),
      )
      if (hasSecondOffenceWithAllAssociatedPersonsSet) {
        offenceDetails.add(
          ReportedOffence(
            offenceCode = 3,
            paragraphCode = "6a",
            victimPrisonersNumber = "A1234AA",
            victimStaffUsername = "ABC12D",
            victimOtherPersonsName = "A name"
          )
        )
      }

      val reportedAdjudication = ReportedAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        reportNumber = 234L,
        bookingId = 123L,
        agencyId = "MDI",
        locationId = 345L,
        isYouthOffender = isYouthOffender,
        incidentRoleCode = incidentRoleCode,
        incidentRoleAssociatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber,
        incidentRoleAssociatedPrisonersName = incidentRoleAssociatedPrisonersName,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
        statement = INCIDENT_STATEMENT,
        offenceDetails = offenceDetails,
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
      )
      // Add audit information
      reportedAdjudication.createdByUserId = "A_USER"
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME
      return reportedAdjudication
    }
  }

  @Nested
  inner class CreateDraftFromReported {
    private val reportedAdjudication = ReportedAdjudication(
      reportNumber = 123, prisonerNumber = "AA1234A", bookingId = 123, agencyId = "MDI",
      dateTimeOfIncident = DATE_TIME_OF_INCIDENT, locationId = 345, statement = INCIDENT_STATEMENT,
      isYouthOffender = false,
      incidentRoleCode = "25b",
      incidentRoleAssociatedPrisonersNumber = "BB2345B",
      incidentRoleAssociatedPrisonersName = "Associated Prisoner",
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
      damages = mutableListOf(
        ReportedDamage(code = DamageCode.CLEANING, details = "details", reporter = "Fred")
      ),
      evidence = mutableListOf(
        ReportedEvidence(code = EvidenceCode.PHOTO, details = "details", reporter = "Fred")
      ),
      witnesses = mutableListOf(
        ReportedWitness(code = WitnessCode.PRISON_OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred")
      )
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
        associatedPrisonersName = "Associated Prisoner",
      ),
      offenceDetails = mutableListOf(
        Offence(
          offenceCode = 3,
          victimPrisonersNumber = "BB2345B",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Another Name",
        )
      ),
      incidentStatement = IncidentStatement(
        completed = true,
        statement = INCIDENT_STATEMENT
      ),
      isYouthOffender = false,
      damages = mutableListOf(
        Damage(code = DamageCode.CLEANING, details = "details", reporter = "Fred")
      ),
      evidence = mutableListOf(
        Evidence(code = EvidenceCode.PHOTO, identifier = null, details = "details", reporter = "Fred")
      ),
      witnesses = mutableListOf(
        Witness(code = WitnessCode.PRISON_OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred")
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
      assertThat(createdDraft.damages)
        .extracting("code", "details", "reporter")
        .contains(
          Tuple(
            DamageCode.CLEANING, "details", "Fred"
          )
        )
      assertThat(createdDraft.evidence)
        .extracting("code", "details", "reporter")
        .contains(
          Tuple(
            EvidenceCode.PHOTO, "details", "Fred"
          )
        )
      assertThat(createdDraft.witnesses)
        .extracting("code", "firstName", "lastName", "reporter")
        .contains(
          Tuple(
            WitnessCode.PRISON_OFFICER, "prison", "officer", "Fred"
          )
        )
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
    private const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"

    private const val OFFENCE_CODE_3_PARAGRAPH_CODE = "6a"
    private const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"

    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_CODE = "7b"
    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER = "7(b)"
    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "7b"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "29z"

    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER = "17(b)"
    private const val YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "17b"
    private const val YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "29f"

    private val INCIDENT_ROLE_CODE = "25a"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"
  }
}
