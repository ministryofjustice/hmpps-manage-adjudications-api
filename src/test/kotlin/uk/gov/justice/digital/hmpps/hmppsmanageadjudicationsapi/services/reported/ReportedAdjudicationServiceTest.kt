package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.HearingRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
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
  private val telemetryClient: TelemetryClient = mock()
  private val hearingRepository: HearingRepository = mock()
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
        authenticationFacade,
        telemetryClient,
        hearingRepository,
      )

    whenever(offenceCodeLookupService.getParagraphNumber(2, false)).thenReturn(OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(
      offenceCodeLookupService.getParagraphDescription(
        2,
        false
      )
    ).thenReturn(OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, false)).thenReturn(
      OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )

    whenever(offenceCodeLookupService.getParagraphNumber(3, false)).thenReturn(OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(
      offenceCodeLookupService.getParagraphDescription(
        3,
        false
      )
    ).thenReturn(OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION)
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, false)).thenReturn(
      OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )

    whenever(offenceCodeLookupService.getParagraphNumber(2, true)).thenReturn(YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION
    )
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(2, true)).thenReturn(
      YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED
    )

    // TODO - Review whether this is required
    whenever(offenceCodeLookupService.getParagraphNumber(3, true)).thenReturn(YOUTH_OFFENCE_CODE_3_PARAGRAPH_NUMBER)
    whenever(offenceCodeLookupService.getParagraphDescription(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION
    )
    whenever(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ON_OWN
    )
    whenever(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(3, true)).thenReturn(
      YOUTH_OFFENCE_CODE_3_NOMIS_CODE_ASSISTED
    )
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
        ),
        ReportedOffence(
          offenceCode = 3,
          victimPrisonersNumber = "BB2345B",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Another Name",
        ),
      )
      if (isYouthOffender) {
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 2,
          ),
        )
      }
      val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication.createdByUserId = "A_SMITH" // Add audit information
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.isYouthOffender = isYouthOffender
          it.offenceDetails = offenceDetails
          it.status = ReportedAdjudicationStatus.REJECTED
          it.statusReason = "Status Reason"
          it.statusDetails = "Status Reason String"
          it.reviewUserId = "A_REVIEWER"
        }
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting(
          "adjudicationNumber",
          "prisonerNumber",
          "bookingId",
          "createdByUserId",
          "createdDateTime",
          "isYouthOffender"
        )
        .contains(1235L, "A12345", 234L, "A_SMITH", REPORTED_DATE_TIME, isYouthOffender)

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(2L, DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)

      assertThat(reportedAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains("25a", IncidentRoleRuleLookup.getOffenceRuleDetails("25a", isYouthOffender), "B23456")

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
            Tuple(
              2,
              YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER,
              YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
              null,
              null,
              null
            ),
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
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(reportNumber = 1L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(reportNumber = 2L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
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

      verify(reportedAdjudicationRepository).findByAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
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
          Tuple.tuple(1L, "A12345", 234L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "A12345", 234L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class MyReportedAdjudications {
    @BeforeEach
    fun beforeEach() {
      val reportedAdjudication1 =
        entityBuilder.reportedAdjudication(reportNumber = 1L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication1.createdByUserId = "A_SMITH"
      reportedAdjudication1.createDateTime = REPORTED_DATE_TIME
      val reportedAdjudication2 =
        entityBuilder.reportedAdjudication(reportNumber = 2L, dateTime = DATE_TIME_OF_INCIDENT)
      reportedAdjudication2.createdByUserId = "P_SMITH"
      reportedAdjudication2.createDateTime = REPORTED_DATE_TIME.plusDays(2)
      whenever(
        reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
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
          Tuple.tuple(1L, "A12345", 234L, "A_SMITH", REPORTED_DATE_TIME),
          Tuple.tuple(2L, "A12345", 234L, "P_SMITH", REPORTED_DATE_TIME.plusDays(2))
        )
    }
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

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
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
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
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = from
          it.createdByUserId = "A_SMITH"
          it.createDateTime = REPORTED_DATE_TIME
        }
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = to
          it.createdByUserId = "A_SMITH"
          it.createDateTime = REPORTED_DATE_TIME
        }
      )
      reportedAdjudicationService.setStatus(1, to)
      verify(reportedAdjudicationRepository).save(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = to
          it.reviewUserId = if (to == ReportedAdjudicationStatus.AWAITING_REVIEW) null else "ITAG_USER"
        }
      )
      if (updatesNomis) {
        verify(prisonApiGateway).publishAdjudication(any())
      } else {
        verify(prisonApiGateway, never()).publishAdjudication(any())
      }

      verify(telemetryClient).trackEvent(
        ReportedAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "reportNumber" to entityBuilder.reportedAdjudication().reportNumber.toString(),
          "agencyId" to entityBuilder.reportedAdjudication().agencyId,
          "status" to to.name,
          "reason" to null,
        ),
        null
      )
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
        "Status Reason String"
      )

      verify(reportedAdjudicationRepository).save(returnedReportedAdjudication)
      assertThat(actualReturnedReportedAdjudication.status).isEqualTo(ReportedAdjudicationStatus.REJECTED)
      assertThat(actualReturnedReportedAdjudication.reviewedByUserId).isEqualTo("ITAG_USER")
      assertThat(actualReturnedReportedAdjudication.statusReason).isEqualTo("Status Reason")
      assertThat(actualReturnedReportedAdjudication.statusDetails).isEqualTo("Status Reason String")

      verify(telemetryClient).trackEvent(
        ReportedAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "reportNumber" to existingReportedAdjudication.reportNumber.toString(),
          "agencyId" to existingReportedAdjudication.agencyId,
          "status" to ReportedAdjudicationStatus.REJECTED.name,
          "reason" to "Status Reason"
        ),
        null
      )
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
        returnedReportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        }
      )

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.ACCEPTED)

      var expectedOffenceCodes = listOf(OFFENCE_CODE_2_NOMIS_CODE_ON_OWN, OFFENCE_CODE_3_NOMIS_CODE_ON_OWN)
      var expectedConnectedOffenderIds: List<String> = emptyList()
      if (!committedOnOwn) {
        expectedOffenceCodes = listOf(OFFENCE_CODE_2_NOMIS_CODE_ASSISTED, OFFENCE_CODE_3_NOMIS_CODE_ASSISTED)
        expectedConnectedOffenderIds = listOf(INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
      }

      val expectedVictimOffenderIds: List<String> = listOf("A1234AA")
      val expectedVictimStaffUsernames: List<String> = listOf("ABC12D")

      val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
        offenderNo = "A12345",
        adjudicationNumber = 1235L,
        bookingId = 234L,
        reporterName = "A_USER",
        reportedDateTime = REPORTED_DATE_TIME,
        agencyId = "MDI",
        incidentLocationId = 2L,
        incidentTime = DATE_TIME_OF_INCIDENT.plusDays(1),
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
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(existingReportedAdjudication)

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

      val expectedConnectedOffenderIds = listOf(INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER)
      val expectedVictimOffenderIds: List<String> = emptyList()
      val expectedVictimStaffUsernames: List<String> = emptyList()

      val expectedAdjudicationToPublish = AdjudicationDetailsToPublish(
        offenderNo = "A12345",
        adjudicationNumber = 1235L,
        bookingId = 234L,
        reporterName = "A_USER",
        reportedDateTime = REPORTED_DATE_TIME,
        agencyId = "MDI",
        incidentLocationId = 2L,
        incidentTime = DATE_TIME_OF_INCIDENT.plusDays(1),
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
        ),
      )
      if (hasSecondOffenceWithAllAssociatedPersonsSet) {
        offenceDetails.add(
          ReportedOffence(
            offenceCode = 3,
            victimPrisonersNumber = "A1234AA",
            victimStaffUsername = "ABC12D",
            victimOtherPersonsName = "A name"
          )
        )
      }

      val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
        .also {
          it.offenceDetails = offenceDetails
          it.isYouthOffender = isYouthOffender
          it.incidentRoleCode = incidentRoleCode
          it.incidentRoleAssociatedPrisonersName = incidentRoleAssociatedPrisonersName
          it.incidentRoleAssociatedPrisonersNumber = incidentRoleAssociatedPrisonersNumber
        }
      // Add audit information
      reportedAdjudication.createdByUserId = "A_USER"
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME
      return reportedAdjudication
    }
  }

  @Nested
  inner class CreateDraftFromReported {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    private val expectedSavedDraftAdjudication = DraftAdjudication(
      prisonerNumber = "A12345",
      reportNumber = 1235L,
      reportByUserId = "A_SMITH",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2L,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES
      ),
      incidentRole = IncidentRole(
        roleCode = "25a",
        associatedPrisonersNumber = "B23456",
        associatedPrisonersName = "Associated Prisoner",
      ),
      offenceDetails = mutableListOf(
        Offence(
          // offence with minimal data set
          offenceCode = 2,
        ),
        Offence(
          // offence with all data set
          offenceCode = 3,
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "A Person",
        ),
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
        Evidence(code = EvidenceCode.PHOTO, identifier = "identifier", details = "details", reporter = "Fred")
      ),
      witnesses = mutableListOf(
        Witness(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred")
      ),
      damagesSaved = true,
      evidenceSaved = true,
      witnessesSaved = true,
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
        .contains("A12345", 1L, 1235L, "A_SMITH")
      assertThat(createdDraft.incidentDetails)
        .extracting("dateTimeOfIncident", "handoverDeadline", "locationId")
        .contains(DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES, 2L)
      assertThat(createdDraft.incidentRole)
        .extracting(
          "roleCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "associatedPrisonersNumber"
        )
        .contains("25a", "25(a)", "Attempts to commit any of the foregoing offences:", "B23456")
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
            "A1234AA",
            "ABC12D",
            "A Person"
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
        .extracting("code", "details", "reporter", "identifier")
        .contains(
          Tuple(
            EvidenceCode.PHOTO, "details", "Fred", "identifier"
          )
        )
      assertThat(createdDraft.witnesses)
        .extracting("code", "firstName", "lastName", "reporter")
        .contains(
          Tuple(
            WitnessCode.OFFICER, "prison", "officer", "Fred"
          )
        )
    }
  }

  @Nested
  inner class AllHearings {
    val now = LocalDate.now()

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(
        hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
          "MDI", now.atStartOfDay(),
          now.plusDays(1).atStartOfDay()
        )
      ).thenReturn(
        reportedAdjudication.hearings
      )
      whenever(reportedAdjudicationRepository.findByReportNumberIn(listOf(reportedAdjudication.reportNumber))).thenReturn(
        listOf(reportedAdjudication)
      )
    }

    @Test
    fun `get all hearings `() {

      val response = reportedAdjudicationService.getAllHearingsByAgencyIdAndDate(
        "MDI", LocalDate.now()
      )

      assertThat(response).isNotNull
      assertThat(response.size).isEqualTo(1)
      assertThat(response.first().adjudicationNumber).isEqualTo(reportedAdjudication.reportNumber)
      assertThat(response.first().prisonerNumber).isEqualTo(reportedAdjudication.prisonerNumber)
      assertThat(response.first().dateTimeOfHearing).isEqualTo(reportedAdjudication.hearings.first().dateTimeOfHearing)
      assertThat(response.first().dateTimeOfDiscovery).isEqualTo(reportedAdjudication.dateTimeOfDiscovery)
    }

    @Test
    fun `empty response test `() {
      whenever(
        hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
          "LEI", now.atStartOfDay(), now.plusDays(1).atStartOfDay()
        )
      ).thenReturn(emptyList())

      val response = reportedAdjudicationService.getAllHearingsByAgencyIdAndDate(
        "LEI", now
      )

      assertThat(response).isNotNull
      assertThat(response.isEmpty()).isTrue
    }
  }

  companion object {
    private val entityBuilder: EntityBuilder = EntityBuilder()
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private const val INCIDENT_STATEMENT = "Example statement"

    private const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    private const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"

    private const val OFFENCE_CODE_3_PARAGRAPH_NUMBER = "6(a)"
    private const val OFFENCE_CODE_3_PARAGRAPH_DESCRIPTION = "Another paragraph description"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"

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
