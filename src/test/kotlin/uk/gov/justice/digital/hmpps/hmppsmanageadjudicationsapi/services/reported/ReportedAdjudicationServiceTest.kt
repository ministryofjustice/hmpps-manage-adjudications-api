package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class ReportedAdjudicationServiceTest : ReportedAdjudicationTestBase() {
  private val prisonApiGateway: PrisonApiGateway = mock()
  private val telemetryClient: TelemetryClient = mock()
  private var reportedAdjudicationService =
    ReportedAdjudicationService(
      reportedAdjudicationRepository,
      prisonApiGateway,
      offenceCodeLookupService,
      authenticationFacade,
      telemetryClient,
    )

  @Nested
  inner class ReportedAdjudicationDetails {

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
      val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {

        it.hearings[0].dateTimeOfHearing = LocalDateTime.now().plusWeeks(1)

        val newFirstHearing = Hearing(
          dateTimeOfHearing = LocalDateTime.now().plusDays(1),
          oicHearingType = OicHearingType.GOV_ADULT,
          locationId = 1,
          agencyId = "MDI",
          oicHearingId = 1,
          reportNumber = 1235L,
        )

        val thirdHearing = Hearing(
          dateTimeOfHearing = LocalDateTime.now().plusWeeks(3),
          oicHearingType = OicHearingType.INAD_YOI,
          locationId = 1,
          agencyId = "MDI",
          oicHearingId = 1,
          reportNumber = 1235L,
        )

        it.hearings.add(newFirstHearing)
        it.hearings.add(thirdHearing)
      }
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
            2,
            YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER,
            YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
            null,
            null,
            null
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
            2, OFFENCE_CODE_2_PARAGRAPH_NUMBER, OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION, null, null, null
          )
      }

      assertThat(reportedAdjudicationDto.incidentStatement)
        .extracting("statement", "completed")
        .contains(INCIDENT_STATEMENT, true)

      // test order is correct
      assertThat(reportedAdjudicationDto.hearings.size).isEqualTo(3)
      assertThat(reportedAdjudicationDto.hearings[0].oicHearingType).isEqualTo(OicHearingType.GOV_ADULT)
      assertThat(reportedAdjudicationDto.hearings[1].oicHearingType).isEqualTo(OicHearingType.GOV)
      assertThat(reportedAdjudicationDto.hearings[2].oicHearingType).isEqualTo(OicHearingType.INAD_YOI)
    }
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

    @Test
    fun `use of accepted throws validation error `() {
      Assertions.assertThrows(ValidationException::class.java) {
        reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.ACCEPTED)
      }
    }

    @ParameterizedTest
    @CsvSource(
      "UNSCHEDULED, UNSCHEDULED",
      "UNSCHEDULED, REJECTED",
      "UNSCHEDULED, AWAITING_REVIEW",
      "UNSCHEDULED, RETURNED",
      "REJECTED, UNSCHEDULED",
      "REJECTED, REJECTED",
      "REJECTED, AWAITING_REVIEW",
      "REJECTED, RETURNED",
      "RETURNED, UNSCHEDULED",
      "RETURNED, REJECTED",
      "RETURNED, RETURNED",
      "SCHEDULED, SCHEDULED",
      "REFER_POLICE, SCHEDULED",
      "NOT_PROCEED, SCHEDULED",
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
      "AWAITING_REVIEW, UNSCHEDULED, true",
      "AWAITING_REVIEW, REJECTED, false",
      "AWAITING_REVIEW, RETURNED, false",
      "AWAITING_REVIEW, AWAITING_REVIEW, false",
      "RETURNED, AWAITING_REVIEW, false",
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
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
      }
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        }
      )

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.UNSCHEDULED)

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
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
      }
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication
      )

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.UNSCHEDULED)

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
  inner class Issued {

    private val now = LocalDateTime.now()
    private val reportedAdjudication = entityBuilder.reportedAdjudication(1)
      .also {
        it.createdByUserId = "A_SMITH"
        it.createDateTime = LocalDateTime.now()
      }

    @ParameterizedTest
    @CsvSource("SCHEDULED", "UNSCHEDULED")
    fun `issue a reported adjudication DIS form with valid status`(status: ReportedAdjudicationStatus) {
      reportedAdjudication.status = status

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      val response = reportedAdjudicationService.setIssued(1, now)

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.issuingOfficer).isEqualTo("ITAG_USER")
      assertThat(argumentCaptor.value.dateTimeOfIssue).isEqualTo(now)
      assertThat(response).isNotNull
    }

    @ParameterizedTest
    @CsvSource("AWAITING_REVIEW", "REJECTED", "RETURNED")
    fun `throws exception when issuing DIS is wrong status`(status: ReportedAdjudicationStatus) {
      reportedAdjudication.status = status

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        reportedAdjudicationService.setIssued(1, now)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("$status not valid status for DIS issue")
    }
  }

  @Nested
  inner class OutcomesHistory {

    private var reportedAdjudication = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.outcomes.add(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
          o ->
          o.createDateTime = LocalDateTime.now()
        }
      )
    }

    private val reportedAdjudication2 = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.outcomes.add(Outcome(code = OutcomeCode.NOT_PROCEED))
    }

    private var reportedAdjudication3 = reportedAdjudication.also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.outcomes.add(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(1)
        }
      )
    }

    private var reportedAdjudication4 = reportedAdjudication3.also {
      it.hearings.add(
        Hearing(locationId = 1, agencyId = "", reportNumber = 1L, oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingId = 1L)
      )
    }

    private var reportedAdjudication5 = reportedAdjudication4.also {
      it.outcomes.add(
        Outcome(code = OutcomeCode.REFER_INAD).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(2)
        }
      )

      it.hearings.first().also { h ->
        h.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
      }
    }

    private var reportedAdjudication6 = reportedAdjudication5.also {
      it.hearings.add(
        Hearing(
          locationId = 1, agencyId = "", reportNumber = 1L, oicHearingType = OicHearingType.INAD_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(2), oicHearingId = 1L,
        )
      )
    }

    private var reportedAdjudication7 = reportedAdjudication6.also {
      it.outcomes.add(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(3)
        }
      )

      it.hearings.last().also {
        h ->
        h.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
      }
    }

    private val reportedAdjudication8 = reportedAdjudication7.also {
      it.outcomes.add(
        Outcome(code = OutcomeCode.PROSECUTION).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(4)
        }
      )
    }

    private val reportedAdjudication9 = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.outcomes.add(
        Outcome(code = OutcomeCode.REFER_INAD).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(2)
        }
      )
      it.outcomes.add(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(3)
        }
      )
      it.outcomes.add(
        Outcome(code = OutcomeCode.PROSECUTION).also {
          o ->
          o.createDateTime = LocalDateTime.now().plusDays(4)
        }
      )

      it.hearings.add(
        Hearing(
          locationId = 1, agencyId = "", reportNumber = 1L, oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
        )
      )
      it.hearings.add(
        Hearing(
          locationId = 1, agencyId = "", reportNumber = 1L, oicHearingType = OicHearingType.INAD_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(2), oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
        )
      )
    }

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudication2)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudication3)
      whenever(reportedAdjudicationRepository.findByReportNumber(4)).thenReturn(reportedAdjudication4)
      whenever(reportedAdjudicationRepository.findByReportNumber(5)).thenReturn(reportedAdjudication5)
      whenever(reportedAdjudicationRepository.findByReportNumber(6)).thenReturn(reportedAdjudication6)
      whenever(reportedAdjudicationRepository.findByReportNumber(7)).thenReturn(reportedAdjudication7)
      whenever(reportedAdjudicationRepository.findByReportNumber(8)).thenReturn(reportedAdjudication8)
      whenever(reportedAdjudicationRepository.findByReportNumber(9)).thenReturn(reportedAdjudication9)
    }

    @Test
    fun `outcome history DTO - Refer police no hearing`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(result.history.size).isEqualTo(1)
      assertThat(result.history.first().hearing).isNull()
      assertThat(result.history.first().outcome).isNotNull
      assertThat(result.history.first().outcome!!.referralOutcome).isNull()
      assertThat(result.history.first().outcome!!.outcome).isNotNull
      assertThat(result.history.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
    }
    @Test
    fun `outcome history DTO - Not proceed no hearing`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(2)

      assertThat(result.history.size).isEqualTo(1)
      assertThat(result.history.first().hearing).isNull()
      assertThat(result.history.first().outcome).isNotNull
      assertThat(result.history.first().outcome!!.referralOutcome).isNull()
      assertThat(result.history.first().outcome!!.outcome).isNotNull
      assertThat(result.history.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(3).validateFirstItem()

      assertThat(result.history.size).isEqualTo(1)
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, hearing scheduled`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(4).validateFirstItem()

      assertThat(result.history.size).isEqualTo(2)

      assertThat(result.history.last().hearing).isNotNull
      assertThat(result.history.last().hearing!!.outcome).isNull()
      assertThat(result.history.last().outcome).isNull()
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(5).validateFirstItem().validateSecondItem()

      assertThat(result.history.size).isEqualTo(2)
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(6).validateFirstItem().validateSecondItem()

      assertThat(result.history.size).isEqualTo(3)

      assertThat(result.history.last().hearing).isNotNull
      assertThat(result.history.last().hearing!!.outcome).isNull()
      assertThat(result.history.last().outcome).isNull()
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled, refer to police`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(7).validateFirstItem().validateSecondItem()

      assertThat(result.history.size).isEqualTo(3)

      assertThat(result.history.last().hearing).isNotNull
      assertThat(result.history.last().hearing!!.outcome).isNotNull
      assertThat(result.history.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome).isNotNull
      assertThat(result.history.last().outcome!!.outcome).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome!!.referralOutcome).isNull()
    }
    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled, refer to police, prosecution YES`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(8).validateFirstItem().validateSecondItem()

      assertThat(result.history.size).isEqualTo(3)

      assertThat(result.history.last().hearing).isNotNull
      assertThat(result.history.last().hearing!!.outcome).isNotNull
      assertThat(result.history.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome).isNotNull
      assertThat(result.history.last().outcome!!.outcome).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome!!.referralOutcome).isNotNull
      assertThat(result.history.last().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.PROSECUTION)
    }
    @Test
    fun `outcome history DTO - Schedule hearing, refer to inad, scheduled hearing, refer to police, prosecution yes`() {
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(9)

      assertThat(result.history.size).isEqualTo(2)
      assertThat(result.history.first().hearing).isNotNull
      assertThat(result.history.first().hearing!!.outcome).isNotNull
      assertThat(result.history.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(result.history.first().outcome).isNotNull
      assertThat(result.history.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.history.first().outcome!!.referralOutcome).isNotNull
      assertThat(result.history.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)

      assertThat(result.history.last().hearing).isNotNull
      assertThat(result.history.last().hearing!!.outcome).isNotNull
      assertThat(result.history.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome).isNotNull
      assertThat(result.history.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.history.last().outcome!!.referralOutcome).isNotNull
      assertThat(result.history.last().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.PROSECUTION)
    }

    private fun ReportedAdjudicationDto.validateFirstItem(): ReportedAdjudicationDto {
      assertThat(this.history.first().hearing).isNull()
      assertThat(this.history.first().outcome).isNotNull
      assertThat(this.history.first().outcome!!.referralOutcome).isNotNull
      assertThat(this.history.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(this.history.first().outcome!!.outcome).isNotNull
      assertThat(this.history.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)

      return this
    }

    private fun ReportedAdjudicationDto.validateSecondItem(): ReportedAdjudicationDto {
      assertThat(this.history[1].hearing).isNotNull
      assertThat(this.history[1].hearing!!.outcome).isNotNull
      assertThat(this.history[1].hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(this.history[1].outcome).isNotNull
      assertThat(this.history[1].outcome!!.outcome).isNotNull
      assertThat(this.history[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)

      return this
    }
  }

  companion object {
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private const val INCIDENT_STATEMENT = "Example statement"

    private const val OFFENCE_CODE_2_PARAGRAPH_NUMBER = "5(b)"
    private const val OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A paragraph description"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ON_OWN = "5b"
    private const val OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "25z"

    private const val OFFENCE_CODE_3_NOMIS_CODE_ON_OWN = "5f"
    private const val OFFENCE_CODE_3_NOMIS_CODE_ASSISTED = "25f"

    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER = "7(b)"
    private const val YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION = "A youth paragraph description"
    private const val YOUTH_OFFENCE_CODE_2_NOMIS_CODE_ASSISTED = "29z"

    private val INCIDENT_ROLE_CODE = "25a"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    private val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

    assertThatThrownBy {
      reportedAdjudicationService.getReportedAdjudicationDetails(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      reportedAdjudicationService.setIssued(1, LocalDateTime.now())
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
