package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

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
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.WitnessRequestItem
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
        telemetryClient
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
  inner class UpdateDamages {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.damages = mutableListOf(
          ReportedDamage(code = DamageCode.CLEANING, details = "details", reporter = "Rod"),
          ReportedDamage(code = DamageCode.REDECORATION, details = "details 3", reporter = "Fred")
        )
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
      reportedAdjudication.createdByUserId = "Jane"
      reportedAdjudication.createDateTime = LocalDateTime.now()
    }

    @Test
    fun `update damages for adjudication`() {
      val response = reportedAdjudicationService.updateDamages(
        1,
        listOf(
          DamageRequestItem(
            reportedAdjudication.damages.first().code,
            reportedAdjudication.damages.first().details,
            reportedAdjudication.damages.first().reporter
          ),
          DamageRequestItem(DamageCode.ELECTRICAL_REPAIR, "details 2", "Fred")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages.size).isEqualTo(2)
      assertThat(argumentCaptor.value.damages.first().code).isEqualTo(DamageCode.CLEANING)
      assertThat(argumentCaptor.value.damages.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo("Rod")
      assertThat(argumentCaptor.value.damages.last().code).isEqualTo(DamageCode.ELECTRICAL_REPAIR)
      assertThat(argumentCaptor.value.damages.last().details).isEqualTo("details 2")
      assertThat(argumentCaptor.value.damages.last().reporter).isEqualTo("Fred")

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.updateDamages(1, listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }
  }

  @Nested
  inner class UpdateEvidence {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.evidence = mutableListOf(
          ReportedEvidence(code = EvidenceCode.PHOTO, identifier = "identifier", details = "details", reporter = "Rod"),
          ReportedEvidence(code = EvidenceCode.BAGGED_AND_TAGGED, identifier = "identifier", details = "details 3", reporter = "Fred")
        )
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
      reportedAdjudication.createdByUserId = "Jane"
      reportedAdjudication.createDateTime = LocalDateTime.now()
    }

    @Test
    fun `update evidence for adjudication`() {
      val response = reportedAdjudicationService.updateEvidence(
        1,
        listOf(
          EvidenceRequestItem(
            reportedAdjudication.evidence.first().code,
            reportedAdjudication.evidence.first().identifier,
            reportedAdjudication.evidence.first().details,
            reportedAdjudication.evidence.first().reporter
          ),
          EvidenceRequestItem(EvidenceCode.BODY_WORN_CAMERA, "identifier 2", "details 2", "Fred")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence.size).isEqualTo(2)
      assertThat(argumentCaptor.value.evidence.first().code).isEqualTo(EvidenceCode.PHOTO)
      assertThat(argumentCaptor.value.evidence.first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.evidence.first().identifier).isEqualTo("identifier")
      assertThat(argumentCaptor.value.evidence.first().reporter).isEqualTo("Rod")
      assertThat(argumentCaptor.value.evidence.last().code).isEqualTo(EvidenceCode.BODY_WORN_CAMERA)
      assertThat(argumentCaptor.value.evidence.last().details).isEqualTo("details 2")
      assertThat(argumentCaptor.value.evidence.last().identifier).isEqualTo("identifier 2")
      assertThat(argumentCaptor.value.evidence.last().reporter).isEqualTo("Fred")

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.updateEvidence(1, listOf(EvidenceRequestItem(EvidenceCode.PHOTO, "", "details")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }
  }

  @Nested
  inner class UpdateWitnesses {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.witnesses = mutableListOf(
          ReportedWitness(code = WitnessCode.STAFF, firstName = "first", lastName = "last", reporter = "Rod"),
          ReportedWitness(code = WitnessCode.OFFICER, firstName = "first", lastName = "last", reporter = "Fred")
        )
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
      reportedAdjudication.createdByUserId = "Jane"
      reportedAdjudication.createDateTime = LocalDateTime.now()
    }

    @Test
    fun `update witnesses for adjudication`() {
      val response = reportedAdjudicationService.updateWitnesses(
        1,
        listOf(
          WitnessRequestItem(
            reportedAdjudication.witnesses.first().code,
            reportedAdjudication.witnesses.first().firstName,
            reportedAdjudication.witnesses.first().lastName,
            reportedAdjudication.witnesses.first().reporter
          ),
          WitnessRequestItem(WitnessCode.OTHER_PERSON, "first", "last", "Fred")
        )
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses.size).isEqualTo(2)
      assertThat(argumentCaptor.value.witnesses.first().code).isEqualTo(WitnessCode.STAFF)
      assertThat(argumentCaptor.value.witnesses.first().firstName).isEqualTo("first")
      assertThat(argumentCaptor.value.witnesses.first().reporter).isEqualTo("Rod")
      assertThat(argumentCaptor.value.witnesses.last().code).isEqualTo(WitnessCode.OTHER_PERSON)
      assertThat(argumentCaptor.value.witnesses.last().firstName).isEqualTo("first")
      assertThat(argumentCaptor.value.witnesses.last().reporter).isEqualTo("Fred")

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.updateWitnesses(1, listOf(WitnessRequestItem(WitnessCode.STAFF, "first", "last")))
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }
  }

  @Nested
  inner class CreateHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.clear()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `create a hearing`() {
      val now = LocalDateTime.now()
      val response = reportedAdjudicationService.createHearing(
        1235L,
        1,
        now
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now)
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().prisonerNumber).isEqualTo(reportedAdjudication.prisonerNumber)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.createHearing(1, 1, LocalDateTime.now())
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }
  }

  @Nested
  inner class AmendHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `amend a hearing`() {
      val now = LocalDateTime.now()
      val response = reportedAdjudicationService.amendHearing(
        1235L,
        1,
        2,
        now.plusDays(1)
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now.plusDays(1))
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().prisonerNumber).isEqualTo(reportedAdjudication.prisonerNumber)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.amendHearing(1, 1, 1, LocalDateTime.now())
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      assertThatThrownBy {
        reportedAdjudicationService.amendHearing(1, 1, 1, LocalDateTime.now())
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }
  }

  @Nested
  inner class DeleteHearing {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

      assertThatThrownBy {
        reportedAdjudicationService.deleteHearing(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      assertThatThrownBy {
        reportedAdjudicationService.deleteHearing(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }

    @Test
    fun `delete a hearing`() {
      val response = reportedAdjudicationService.deleteHearing(
        1235L,
        1,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(0)

      assertThat(response).isNotNull
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
