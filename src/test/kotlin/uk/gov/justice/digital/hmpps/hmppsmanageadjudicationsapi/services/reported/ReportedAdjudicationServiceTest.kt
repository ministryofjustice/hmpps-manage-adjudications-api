package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import java.time.LocalDateTime

class ReportedAdjudicationServiceTest : ReportedAdjudicationTestBase() {
  private val legacySyncService: LegacySyncService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val reportedAdjudicationService =
    ReportedAdjudicationService(
      reportedAdjudicationRepository,
      legacySyncService,
      offenceCodeLookupService,
      authenticationFacade,
      telemetryClient,
    )

  @Nested
  inner class ReportedAdjudicationDetails {

    @Test
    fun `outcome entered in nomis flag is set `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1L)

      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `filter out caution and damages owed from dto if present in punishments `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.addPunishment(
            Punishment(
              type = PunishmentType.DAMAGES_OWED,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.CAUTION,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
        },
      )
      assertThat(reportedAdjudicationService.getReportedAdjudicationDetails(1L).punishments).isEmpty()
    }

    @Test
    fun `adjudication is not part of active case load throws exception `() {
      whenever(authenticationFacade.activeCaseload).thenReturn("OTHER")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(entityBuilder.reportedAdjudication())

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override agency is not found throws exception `() {
      whenever(authenticationFacade.activeCaseload).thenReturn("TJW")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.overrideAgencyId = "XXX"
        },
      )

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override agency is not found throws exception when no case load or override set `() {
      whenever(authenticationFacade.activeCaseload).thenReturn(null)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override caseload allows access`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("TJW")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.overrideAgencyId = "TJW"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(result).isNotNull
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
        },
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(reportedAdjudicationDto)
        .extracting(
          "adjudicationNumber",
          "prisonerNumber",
          "createdByUserId",
          "createdDateTime",
          "isYouthOffender",
        )
        .contains(1235L, "A12345", "A_SMITH", REPORTED_DATE_TIME, isYouthOffender)

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
            "victimOtherPersonsName",
          )
          .contains(
            2,
            YOUTH_OFFENCE_CODE_2_PARAGRAPH_NUMBER,
            YOUTH_OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
            null,
            null,
            null,
          )
      } else {
        assertThat(reportedAdjudicationDto.offenceDetails)
          .extracting(
            "offenceCode",
            "offenceRule.paragraphNumber",
            "offenceRule.paragraphDescription",
            "victimPrisonersNumber",
            "victimStaffUsername",
            "victimOtherPersonsName",
          )
          .contains(
            2,
            OFFENCE_CODE_2_PARAGRAPH_NUMBER,
            OFFENCE_CODE_2_PARAGRAPH_DESCRIPTION,
            null,
            null,
            null,
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
    @EnumSource(ReportedAdjudicationStatus::class)
    fun `setting status for a reported adjudication throws an illegal state exception for invalid transitions`(
      from: ReportedAdjudicationStatus,
    ) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = from
        },
      )
      ReportedAdjudicationStatus.values().filter { it != ReportedAdjudicationStatus.ACCEPTED }.filter { !from.nextStates().contains(it) }.forEach {
        Assertions.assertThrows(IllegalStateException::class.java) {
          reportedAdjudicationService.setStatus(1, it)
        }
      }
    }

    @Test
    fun `ensure all status are connected - should catch any new status not wired up - if this test fails you need to add the status to the correct next states `() {
      val endStatus = ReportedAdjudicationStatus.values().filter { it.nextStates().isEmpty() }
      val transitionStatus = ReportedAdjudicationStatus.values().filter { it.nextStates().isNotEmpty() }
      repeat(endStatus.size) {
        assertThat(transitionStatus.any { it.nextStates().contains(it) }).isTrue()
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
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = to
          it.createdByUserId = "A_SMITH"
          it.createDateTime = REPORTED_DATE_TIME
        },
      )
      reportedAdjudicationService.setStatus(1, to)
      verify(reportedAdjudicationRepository).save(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = to
          it.reviewUserId = if (to == ReportedAdjudicationStatus.AWAITING_REVIEW) null else "ITAG_USER"
        },
      )
      if (updatesNomis) {
        verify(legacySyncService).publishAdjudication(any())
      } else {
        verify(legacySyncService, never()).publishAdjudication(any())
      }

      verify(telemetryClient).trackEvent(
        ReportedAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "reportNumber" to entityBuilder.reportedAdjudication().reportNumber.toString(),
          "agencyId" to entityBuilder.reportedAdjudication().agencyId,
          "status" to to.name,
          "reason" to null,
        ),
        null,
      )
    }

    @Test
    fun `returns correct status information`() {
      val existingReportedAdjudication = existingReportedAdjudication(true, true, false)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        existingReportedAdjudication,
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
        returnedReportedAdjudication,
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

      verify(telemetryClient).trackEvent(
        ReportedAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "reportNumber" to existingReportedAdjudication.reportNumber.toString(),
          "agencyId" to existingReportedAdjudication.agencyId,
          "status" to ReportedAdjudicationStatus.REJECTED.name,
          "reason" to "Status Reason",
        ),
        null,
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
        existingReportedAdjudication,
      )

      val returnedReportedAdjudication = existingReportedAdjudication.copy().also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
      }
      returnedReportedAdjudication.createdByUserId = "A_USER"
      returnedReportedAdjudication.createDateTime = REPORTED_DATE_TIME
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        },
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
      verify(legacySyncService).publishAdjudication(expectedAdjudicationToPublish)
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
        returnedReportedAdjudication,
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
      verify(legacySyncService).publishAdjudication(expectedAdjudicationToPublish)
    }

    @Test
    fun `create adjudication removes offender from multiple roles for prison api `() {
      val now = LocalDateTime.now()
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createdByUserId = ""
        it.createDateTime = now
        it.incidentRoleAssociatedPrisonersNumber = null
        it.offenceDetails.first().victimPrisonersNumber = "A12345"
        it.offenceDetails.first().victimStaffUsername = null
        it.offenceDetails.first().victimOtherPersonsName = null
      }

      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      reportedAdjudicationService.setStatus(1, ReportedAdjudicationStatus.UNSCHEDULED)

      verify(legacySyncService, atLeastOnce()).publishAdjudication(
        adjudicationDetailsToPublish = AdjudicationDetailsToPublish(
          offenderNo = reportedAdjudication.prisonerNumber,
          adjudicationNumber = reportedAdjudication.reportNumber,
          reporterName = "",
          reportedDateTime = now,
          agencyId = reportedAdjudication.agencyId,
          incidentLocationId = reportedAdjudication.locationId,
          incidentTime = reportedAdjudication.dateTimeOfDiscovery,
          statement = reportedAdjudication.statement,
          offenceCodes = listOf("25f"),
          victimStaffUsernames = emptyList(),
          victimOffenderIds = emptyList(),
          connectedOffenderIds = emptyList(),
        ),
      )
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
            victimOtherPersonsName = "A name",
          ),
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

    private val reportedAdjudicationDisIssued = entityBuilder.reportedAdjudication(1)
      .also {
        it.createdByUserId = "A_SMITH"
        it.createDateTime = now.minusHours(2)
        it.issuingOfficer = "B_JOHNSON"
        it.dateTimeOfIssue = now.minusHours(1)
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
      assertThat(argumentCaptor.value.disIssueHistory.size).isEqualTo(0)
      assertThat(response).isNotNull
    }

    @ParameterizedTest
    @CsvSource("SCHEDULED", "UNSCHEDULED")
    fun `re-issue a reported adjudication DIS form`(status: ReportedAdjudicationStatus) {
      reportedAdjudicationDisIssued.status = status

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationDisIssued)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudicationDisIssued)

      val response = reportedAdjudicationService.setIssued(1, now)

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.issuingOfficer).isEqualTo("ITAG_USER")
      assertThat(argumentCaptor.value.dateTimeOfIssue).isEqualTo(now)
      assertThat(argumentCaptor.value.disIssueHistory[0].issuingOfficer).isEqualTo("B_JOHNSON")
      assertThat(argumentCaptor.value.disIssueHistory[0].dateTimeOfIssue).isEqualTo(now.minusHours(1))
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

    private val reportedAdjudicationReferPolice = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
            o ->
          o.createDateTime = LocalDateTime.now()
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
            o ->
          o.deleted = true
          o.createDateTime = LocalDateTime.now().plusDays(1)
        },
      )
    }

    private val reportedAdjudicationNotProceed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.addOutcome(
        Outcome(code = OutcomeCode.NOT_PROCEED).also {
            o ->
          o.createDateTime = LocalDateTime.now()
        },
      )
    }

    private val reportedAdjudicationNoProsecution = entityBuilder.reportedAdjudication().also {
      it.hearings.clear()
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      reportedAdjudicationReferPolice.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      it.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
          it.createDateTime = LocalDateTime.now().plusDays(1)
        },
      )
      it.hearings.add(
        Hearing(locationId = 1, agencyId = "", reportNumber = 1L, oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingId = 1L),
      )
    }

    private val reportedAdjudicationReferInad = entityBuilder.reportedAdjudication().also {
      it.hearings.clear()
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      reportedAdjudicationNoProsecution.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      reportedAdjudicationNoProsecution.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_INAD).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(2)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(2).plusHours(1)
        },
      )

      it.hearings.first().also { h ->
        h.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
      }
    }

    private val reportedAdjudicationInadHearing = entityBuilder.reportedAdjudication().also {
      it.hearings.clear()
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      reportedAdjudicationReferInad.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      reportedAdjudicationReferInad.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.INAD_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(2),
          oicHearingId = 1L,
        ),
      )
    }

    private val reportedAdjudicationInadReferPolice = entityBuilder.reportedAdjudication().also {
      it.hearings.clear()
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      reportedAdjudicationInadHearing.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      reportedAdjudicationInadHearing.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(4)
        },
      )

      it.hearings.last().also {
          h ->
        h.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
      }
    }

    private val reportedAdjudicationProsecution = entityBuilder.reportedAdjudication().also {
      it.hearings.clear()
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      reportedAdjudicationInadReferPolice.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      reportedAdjudicationInadReferPolice.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.PROSECUTION).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(5)
        },
      )
    }

    private val reportedAdjudicationReferPoliceNotProceed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(2)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.NOT_PROCEED).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(3)
        },
      )
    }

    private val reportedAdjudicationProsecutionAllHearings = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_INAD).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(2)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(3)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(4)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.PROSECUTION).also {
            o ->
          o.createDateTime = LocalDateTime.now().plusDays(5)
        },
      )

      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.GOV_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(1),
          oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = ""),
        ),
      )
      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.INAD_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(2),
          oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
        ),
      )
    }

    private val reportedAdjudicationReferInadNotProceed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.GOV_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(1),
          oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = ""),
        ),
      )

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_INAD).also {
          it.createDateTime = LocalDateTime.now().plusDays(1)
        },
      )
      it.addOutcome(
        Outcome(code = OutcomeCode.NOT_PROCEED).also {
          it.createDateTime = LocalDateTime.now().plusDays(2)
        },
      )
    }

    private val reportedAdjudicationAdjourned = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationReferPolice.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
          it.createDateTime = LocalDateTime.now().plusDays(1)
        },
      )

      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.GOV_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(1),
          oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
        ),
      )
    }

    private val reportedAdjudicationReferPoliceReferInadAdjourned = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationInadHearing.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }
      reportedAdjudicationInadHearing.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.hearings.last().also {
          h ->
        h.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "")
      }
    }

    private val reportedAdjudicationCompletedHearing = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      it.hearings.add(
        Hearing(
          oicHearingId = 1,
          dateTimeOfHearing = LocalDateTime.now(),
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.GOV,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
        ),
      )
    }

    private val reportedAdjudicationCompletedHearingDismissed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationCompletedHearing.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.DISMISSED),
      )
    }

    private val reportedAdjudicationCompletedHearingNotProceed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationCompletedHearing.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.NOT_PROCEED),
      )
    }

    private val reportedAdjudicationCompletedHearingChargeProved = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationCompletedHearing.hearings.forEach { h -> it.hearings.add(h.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.CHARGE_PROVED),
      )
    }

    private val reportedAdjudicationCompletedHearingAfterAdjourn = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationReferPoliceReferInadAdjourned.hearings.forEach { h -> it.hearings.add(h.copy()) }
      reportedAdjudicationReferPoliceReferInadAdjourned.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }

      it.hearings.add(
        Hearing(
          oicHearingId = 1,
          dateTimeOfHearing = LocalDateTime.now().plusDays(5),
          locationId = 1,
          agencyId = "",
          reportNumber = 1L,
          oicHearingType = OicHearingType.GOV,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
        ),
      )

      it.addOutcome(
        Outcome(code = OutcomeCode.CHARGE_PROVED),
      )
    }

    private val reportedAdjudicationCompletedHearingNotProceedQuashed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()

      reportedAdjudicationCompletedHearingNotProceed.hearings.forEach { h -> it.hearings.add(h.copy()) }
      reportedAdjudicationCompletedHearingNotProceed.getOutcomes().forEach { o -> it.addOutcome(o.copy()) }

      it.addOutcome(
        Outcome(code = OutcomeCode.QUASHED),
      )
    }

    @Test
    fun `no data`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(0)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.clear()
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(0)

      assertThat(result.outcomes.isEmpty()).isTrue
    }

    @Test
    fun `single hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(10)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(10)

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police no hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationReferPolice)

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
    }

    @Test
    fun `outcome history DTO - Not proceed no hearing`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(2)

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, hearing scheduled`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationNoProsecution)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(4).validateFirstItem()

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNull()
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationReferInad)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(5).validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(2)
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationInadHearing)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(6).validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNull()
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled, refer to police`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationInadReferPolice)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(7).validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome!!.referralOutcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled, refer to police, prosecution YES`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationProsecution)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(8).validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.PROSECUTION)
    }

    @Test
    fun `outcome history DTO - Schedule hearing, refer to inad, scheduled hearing, refer to police, prosecution yes`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationProsecutionAllHearings)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(9)

      assertThat(result.outcomes.size).isEqualTo(2)
      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.PROSECUTION)
    }

    @Test
    fun `outcome history DTO - Refer police no hearing, No prosecution, Not proceed`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationReferPoliceNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(11)
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - hearing refers to INAD who chooses NOT_PROCEED `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationReferInadNotProceed)

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(12)
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - refer to police, no prosecution, hearing scheduled and adjourned`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationAdjourned)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(13).validateFirstItem()
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - refer to police, no prosecution, hearing scheduled, refer to inad, hearing scheduled and adjourned`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationReferPoliceReferInadAdjourned)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(14).validateFirstItem().validateSecondItem()
      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - hearing completed - dismissed `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationCompletedHearingDismissed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(15)
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.DISMISSED)
    }

    @Test
    fun `outcome history DTO - hearing completed - not proceed `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationCompletedHearingNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(16)
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - hearing completed - charge proved `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationCompletedHearingChargeProved)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(17)
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @Test
    fun `outcome history DTO - refer to police, no prosecution, hearing scheduled and adjourned, rescheduled and charge proved `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationCompletedHearingAfterAdjourn)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(18).validateFirstItem().validateSecondItem()
      assertThat(result.outcomes.size).isEqualTo(4)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(result.outcomes.last().outcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.last().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @Test
    fun `outcome history DTO - quashed `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudicationCompletedHearingNotProceedQuashed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails(19)
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.QUASHED)
    }

    @Test
    fun `outcome history DTO - multiple same outcomes `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.clear()

          it.hearings.add(
            Hearing(
              agencyId = "",
              locationId = 1L,
              oicHearingId = 1L,
              oicHearingType = OicHearingType.GOV,
              dateTimeOfHearing = LocalDateTime.now(),
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
              reportNumber = 1L,
            ),
          )

          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE, details = "refer 2").also {
                o ->
              o.createDateTime = LocalDateTime.now().plusDays(2)
            },
          )

          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE, details = "refer 1").also {
                o ->
              o.createDateTime = LocalDateTime.now()
            },
          )

          it.addOutcome(
            Outcome(code = OutcomeCode.SCHEDULE_HEARING).also {
                o ->
              o.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )

          it.hearings.add(
            Hearing(
              agencyId = "",
              locationId = 1L,
              oicHearingId = 1L,
              oicHearingType = OicHearingType.GOV,
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
              reportNumber = 1L,
            ),
          )
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(20)
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.first().outcome!!.outcome.details).isEqualTo("refer 1")
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome!!.outcome.details).isEqualTo("refer 2")
    }

    @Test
    fun `original nomis hearing outcome status - 1 hearing `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.NOMIS,
            adjudicator = "",
          )
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `original nomis hearing outcome status - multiple hearing `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.NOMIS,
            adjudicator = "",
          )
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 1, oicHearingType = OicHearingType.GOV, reportNumber = 1),
          )
          it.hearings.last().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomes.last().outcome).isNull()
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `subsequent nomis hearing outcome status - original outcome created via adjudications`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.REFER_POLICE,
            adjudicator = "",
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE).also { o ->
              o.createDateTime = LocalDateTime.now()
            },
          )
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 1, oicHearingType = OicHearingType.GOV, reportNumber = 1),
          )
          it.hearings.last().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails(1)

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome).isNull()
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    private fun ReportedAdjudicationDto.validateFirstItem(): ReportedAdjudicationDto {
      assertThat(this.outcomes.first().hearing).isNull()
      assertThat(this.outcomes.first().outcome).isNotNull
      assertThat(this.outcomes.first().outcome!!.referralOutcome).isNotNull
      assertThat(this.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(this.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(this.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)

      return this
    }

    private fun ReportedAdjudicationDto.validateSecondItem(): ReportedAdjudicationDto {
      assertThat(this.outcomes[1].hearing).isNotNull
      assertThat(this.outcomes[1].hearing!!.outcome).isNotNull
      assertThat(this.outcomes[1].hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_INAD)
      assertThat(this.outcomes[1].outcome).isNotNull
      assertThat(this.outcomes[1].outcome!!.outcome).isNotNull
      assertThat(this.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)

      return this
    }
  }

  @Nested
  inner class HasReferralOutcome {

    @Test
    fun `returns true when last item is referral outcome `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.REFER_POLICE,
            adjudicator = "",
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE).also { o -> o.createDateTime = LocalDateTime.now() },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.SCHEDULE_HEARING).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) },
          )
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome(1)).isEqualTo(true)
    }

    @Test
    fun `returns false when no history `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.clear()
          it.clearOutcomes()
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome(1)).isEqualTo(false)
    }

    @Test
    fun `returns false when no last item has no outcome `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome(1)).isEqualTo(false)
    }

    @Test
    fun `returns false when no last item has no referral outcome `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE),
          )
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome(1)).isEqualTo(false)
    }
  }

  @Nested
  inner class ReadonlyAdjudication {

    @CsvSource("RETURNED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `adjudication awaiting review or returned is editable by originating agency`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isFalse
    }

    @CsvSource("RETURNED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `adjudication awaiting review or returned is readonly for override agency`(status: ReportedAdjudicationStatus) {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isTrue
    }

    @Test
    fun `unscheduled report is editable by override agency`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isFalse
    }

    @Test
    fun `unscheduled report is readonly for originating agency`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isTrue
    }

    @Test
    fun `scheduled report is readonly for override agency`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isTrue
    }

    @Test
    fun `scheduled report is editable by originating agency`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isFalse
    }

    @CsvSource("CHARGE_PROVED", "QUASHED", "REFER_POLICE", "REFER_INAD", "NOT_PROCEED", "PROSECUTION", "DISMISSED", "ADJOURNED")
    @ParameterizedTest
    fun `for other states it is always readonly for originating agency`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isTrue
    }

    @CsvSource("CHARGE_PROVED", "QUASHED", "REFER_POLICE", "REFER_INAD", "NOT_PROCEED", "PROSECUTION", "DISMISSED", "ADJOURNED")
    @ParameterizedTest
    fun `for other states it is always editable for override agency`(status: ReportedAdjudicationStatus) {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isFalse
    }

    @EnumSource(ReportedAdjudicationStatus::class)
    @ParameterizedTest
    fun `report is not transferable and readonly flag should be null `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isNull()
    }

    @Test
    fun `no active caseload returns readonly is true`() {
      whenever(authenticationFacade.activeCaseload).thenReturn(null)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(1)
      assertThat(response.transferableReadonly).isTrue
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

  @Test
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

    assertThatThrownBy {
      reportedAdjudicationService.lastOutcomeHasReferralOutcome(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
