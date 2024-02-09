package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.IncidentRoleRuleLookup
import java.time.LocalDate
import java.time.LocalDateTime

class ReportedAdjudicationServiceTest : ReportedAdjudicationTestBase() {
  private val telemetryClient: TelemetryClient = mock()
  private val reportedAdjudicationService =
    ReportedAdjudicationService(
      reportedAdjudicationRepository,
      offenceCodeLookupService,
      authenticationFacade,
      telemetryClient,
    )

  @Nested
  inner class ReportedAdjudicationDetails {

    @Test
    fun `can action from history is false`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.canActionFromHistory).isFalse
    }

    @Test
    fun `outcome entered in nomis flag is set `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `caution and damages owed should be in punishments`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
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
      assertThat(reportedAdjudicationService.getReportedAdjudicationDetails("1").punishments.size).isEqualTo(2)
    }

    @Test
    fun `adjudication is not part of active case load throws exception `() {
      whenever(authenticationFacade.activeCaseload).thenReturn("OTHER")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails("1")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override agency is not found throws exception `() {
      whenever(authenticationFacade.activeCaseload).thenReturn("BXI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.overrideAgencyId = "XXX"
        },
      )

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails("1")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override agency is not found throws exception when no case load or override set `() {
      whenever(authenticationFacade.activeCaseload).thenReturn(null)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      assertThatThrownBy {
        reportedAdjudicationService.getReportedAdjudicationDetails("1")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `override caseload allows access`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("BXI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.overrideAgencyId = "BXI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")
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
      val offenceDetails = mutableListOf(
        ReportedOffence(
          offenceCode = 1002,
        ),
      )
      val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
        it.hearings[0].dateTimeOfHearing = LocalDateTime.now().plusWeeks(1)

        val newFirstHearing = Hearing(
          dateTimeOfHearing = LocalDateTime.now().plusDays(1),
          oicHearingType = OicHearingType.GOV_ADULT,
          locationId = 1,
          agencyId = "MDI",
          oicHearingId = 1,
          chargeNumber = "1235",
        )

        val thirdHearing = Hearing(
          dateTimeOfHearing = LocalDateTime.now().plusWeeks(3),
          oicHearingType = OicHearingType.INAD_YOI,
          locationId = 1,
          agencyId = "MDI",
          oicHearingId = 1,
          chargeNumber = "1235",
        )

        it.hearings.add(newFirstHearing)
        it.hearings.add(thirdHearing)
      }
      reportedAdjudication.createdByUserId = "A_SMITH" // Add audit information
      reportedAdjudication.createDateTime = REPORTED_DATE_TIME

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.isYouthOffender = isYouthOffender
          it.offenceDetails = offenceDetails
          it.status = ReportedAdjudicationStatus.REJECTED
          it.statusReason = "Status Reason"
          it.statusDetails = "Status Reason String"
          it.reviewUserId = "A_REVIEWER"
        },
      )

      val reportedAdjudicationDto = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(reportedAdjudicationDto)
        .extracting(
          "chargeNumber",
          "prisonerNumber",
          "createdByUserId",
          "createdDateTime",
          "isYouthOffender",
        )
        .contains("1235", "A12345", "A_SMITH", REPORTED_DATE_TIME, isYouthOffender)

      assertThat(reportedAdjudicationDto.incidentDetails)
        .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
        .contains(2L, DATE_TIME_OF_INCIDENT, DATE_TIME_REPORTED_ADJUDICATION_EXPIRES)

      assertThat(reportedAdjudicationDto.incidentRole)
        .extracting("roleCode", "offenceRule", "associatedPrisonersNumber")
        .contains("25a", IncidentRoleRuleLookup.getOffenceRuleDetails("25a", isYouthOffender), "B23456")

      assertThat(reportedAdjudicationDto)
        .extracting("status", "reviewedByUserId", "statusReason", "statusDetails")
        .contains(ReportedAdjudicationStatus.REJECTED, "A_REVIEWER", "Status Reason", "Status Reason String")

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
          1002,
          offenceCodeLookupService.getOffenceCode(1002, isYouthOffender).paragraph,
          offenceCodeLookupService.getOffenceCode(1002, isYouthOffender).paragraphDescription.getParagraphDescription(
            Gender.MALE,
          ),
          null,
          null,
          null,
        )

      assertThat(reportedAdjudicationDto.incidentStatement)
        .extracting("statement", "completed")
        .contains(INCIDENT_STATEMENT, true)

      // test order is correct
      assertThat(reportedAdjudicationDto.hearings.size).isEqualTo(3)
      assertThat(reportedAdjudicationDto.hearings[0].oicHearingType).isEqualTo(OicHearingType.GOV_ADULT)
      assertThat(reportedAdjudicationDto.hearings[1].oicHearingType).isEqualTo(OicHearingType.GOV)
      assertThat(reportedAdjudicationDto.hearings[2].oicHearingType).isEqualTo(OicHearingType.INAD_YOI)

      verify(reportedAdjudicationRepository, never()).findByPrisonerNumberAndChargeNumberStartsWith(any(), any())
    }

    @Test
    fun `get reported adjudication details with flag to indicate consecutive report is available to view `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.addPunishment(
            Punishment(
              type = PunishmentType.ADDITIONAL_DAYS,
              consecutiveChargeNumber = "999",
              schedule = mutableListOf(
                PunishmentSchedule(days = 10),
              ),
            ),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.ADDITIONAL_DAYS,
              schedule = mutableListOf(
                PunishmentSchedule(days = 10),
              ),
            ),
          )
        },
      )

      val dto = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(dto.punishments.first { it.consecutiveChargeNumber == "999" }.consecutiveReportAvailable).isTrue
      assertThat(dto.punishments.first { it.consecutiveChargeNumber == null }.consecutiveReportAvailable).isNull()
    }

    @Test
    fun `contains linked charges and filters out own charge`() {
      val report = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.migratedSplitRecord = true
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndChargeNumberStartsWith(any(), any())).thenReturn(
        listOf(
          report,
          entityBuilder.reportedAdjudication(chargeNumber = "9872-2"),
          entityBuilder.reportedAdjudication(chargeNumber = "9872-1"),
        ),
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails(report.chargeNumber)

      verify(reportedAdjudicationRepository, atLeastOnce()).findByPrisonerNumberAndChargeNumberStartsWith("A12345", "${report.chargeNumber}-")

      assertThat(response.linkedChargeNumbers.size).isEqualTo(2)
      assertThat(response.linkedChargeNumbers.first()).isEqualTo("9872-1")
      assertThat(response.linkedChargeNumbers.last()).isEqualTo("9872-2")
    }
  }

  @Nested
  inner class ReportedAdjudicationSetReportedAdjudicationStatus {

    @ParameterizedTest
    @EnumSource(ReportedAdjudicationStatus::class)
    fun `setting status for a reported adjudication throws an illegal state exception for invalid transitions`(
      from: ReportedAdjudicationStatus,
    ) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = from
        },
      )
      ReportedAdjudicationStatus.values().filter { it != ReportedAdjudicationStatus.ACCEPTED }.filter { !from.nextStates().contains(it) }.forEach {
        Assertions.assertThrows(IllegalStateException::class.java) {
          reportedAdjudicationService.setStatus("1", it)
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
      "AWAITING_REVIEW, UNSCHEDULED",
      "AWAITING_REVIEW, REJECTED",
      "AWAITING_REVIEW, RETURNED",
      "AWAITING_REVIEW, AWAITING_REVIEW",
      "RETURNED, AWAITING_REVIEW",
    )
    fun `setting status for a reported adjudication for valid transitions`(
      from: ReportedAdjudicationStatus,
      to: ReportedAdjudicationStatus,
    ) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
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
      reportedAdjudicationService.setStatus("1", to)
      verify(reportedAdjudicationRepository).save(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          it.status = to
          it.reviewUserId = if (to == ReportedAdjudicationStatus.AWAITING_REVIEW) null else "ITAG_USER"
          it.lastModifiedAgencyId = it.originatingAgencyId
        },
      )

      verify(telemetryClient).trackEvent(
        ReportedAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "chargeNumber" to entityBuilder.reportedAdjudication().chargeNumber.toString(),
          "agencyId" to entityBuilder.reportedAdjudication().originatingAgencyId,
          "status" to to.name,
          "reason" to null,
        ),
        null,
      )
    }

    @Test
    fun `returns correct status information`() {
      val existingReportedAdjudication = existingReportedAdjudication(true, true, true)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
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
      returnedReportedAdjudication.lastModifiedAgencyId = returnedReportedAdjudication.originatingAgencyId
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        returnedReportedAdjudication,
      )

      val actualReturnedReportedAdjudication = reportedAdjudicationService.setStatus(
        "1",
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
          "chargeNumber" to existingReportedAdjudication.chargeNumber,
          "agencyId" to existingReportedAdjudication.originatingAgencyId,
          "status" to ReportedAdjudicationStatus.REJECTED.name,
          "reason" to "Status Reason",
        ),
        null,
      )
    }

    private fun existingReportedAdjudication(
      committedOnOwn: Boolean,
      isYouthOffender: Boolean,
      withVictims: Boolean,
    ): ReportedAdjudication {
      var incidentRoleCode: String? = null
      var incidentRoleAssociatedPrisonersNumber: String? = null
      var incidentRoleAssociatedPrisonersName: String? = null
      if (!committedOnOwn) {
        incidentRoleCode = INCIDENT_ROLE_CODE
        incidentRoleAssociatedPrisonersNumber = INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER
        incidentRoleAssociatedPrisonersName = INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME
      }

      val offenceDetails = if (withVictims) {
        mutableListOf(
          ReportedOffence(
            offenceCode = 1002,
            victimPrisonersNumber = "A1234AA",
            victimStaffUsername = "ABC12D",
            victimOtherPersonsName = "A name",
          ),
        )
      } else {
        mutableListOf(
          ReportedOffence(offenceCode = 1002),
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
    private val reportedAdjudication = entityBuilder.reportedAdjudication(chargeNumber = "1")
      .also {
        it.createdByUserId = "A_SMITH"
        it.createDateTime = LocalDateTime.now()
      }

    private val reportedAdjudicationDisIssued = entityBuilder.reportedAdjudication(chargeNumber = "1")
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

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      val response = reportedAdjudicationService.setIssued("1", now)

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

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationDisIssued)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudicationDisIssued)

      val response = reportedAdjudicationService.setIssued("1", now)

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

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        reportedAdjudicationService.setIssued("1", now)
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
        Hearing(locationId = 1, agencyId = "", chargeNumber = "1", oicHearingType = OicHearingType.GOV_ADULT, dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingId = 1L),
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

    private val reportedAdjudicationReferGovNotProceed = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""

      it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_GOV, adjudicator = "")

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_GOV).also {
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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
          chargeNumber = "1",
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

    private val reportedAdjudicationWithMigratedAndUIDate = entityBuilder.reportedAdjudication().also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
      it.hearings.clear()
      it.clearOutcomes()

      it.hearings.add(
        Hearing(
          locationId = 1,
          agencyId = "",
          chargeNumber = "1",
          oicHearingType = OicHearingType.INAD_ADULT,
          dateTimeOfHearing = LocalDateTime.now().plusDays(2),
          oicHearingId = 1L,
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
        ),
      )

      it.addOutcome(
        Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()),
      )

      it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED).also { it.createDateTime = LocalDateTime.now().plusDays(1) })
    }

    @Test
    fun `no data`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("0")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.clear()
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("0")

      assertThat(result.outcomes.isEmpty()).isTrue
    }

    @Test
    fun `single hearing`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("10")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("10")

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police no hearing`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferPolice)

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
    }

    @Test
    fun `outcome history DTO - Not proceed no hearing`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("2")

      assertThat(result.outcomes.size).isEqualTo(1)
      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes.first().outcome!!.outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, hearing scheduled`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationNoProsecution)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("4").validateFirstItem()

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNull()
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer gov, not proceed`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferGovNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("4")

      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_GOV)
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferInad)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("5").validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(2)
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationInadHearing)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("6").validateFirstItem().validateSecondItem()

      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNull()
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - Refer police, No prosecution, schedule hearing, refer to INAD, hearing scheduled, refer to police`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationInadReferPolice)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("7").validateFirstItem().validateSecondItem()

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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationProsecution)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("8").validateFirstItem().validateSecondItem()

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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationProsecutionAllHearings)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("9")

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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferPoliceNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("11")
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().hearing).isNull()
      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.first().outcome!!.referralOutcome).isNotNull
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - hearing refers to INAD who chooses NOT_PROCEED `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferInadNotProceed)

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("12")
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationAdjourned)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("13").validateFirstItem()
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - refer to police, no prosecution, hearing scheduled, refer to inad, hearing scheduled and adjourned`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationReferPoliceReferInadAdjourned)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("14").validateFirstItem().validateSecondItem()
      assertThat(result.outcomes.size).isEqualTo(3)

      assertThat(result.outcomes.last().hearing).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome).isNotNull
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(result.outcomes.last().outcome).isNull()
    }

    @Test
    fun `outcome history DTO - hearing completed - dismissed `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationCompletedHearingDismissed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("15")
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationCompletedHearingNotProceed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("16")
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationCompletedHearingChargeProved)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("17")
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationCompletedHearingAfterAdjourn)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("18").validateFirstItem().validateSecondItem()
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationCompletedHearingNotProceedQuashed)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("19")
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.QUASHED)
    }

    @Test
    fun `outcome history DTO - orders migrated and UI data correctly`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudicationWithMigratedAndUIDate)
      val result = reportedAdjudicationService.getReportedAdjudicationDetails("19")
      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `outcome history DTO - multiple same outcomes `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
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
              chargeNumber = "1",
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
              chargeNumber = "1",
            ),
          )
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("20")
      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.first().outcome!!.outcome.details).isEqualTo("refer 1")
      assertThat(result.outcomes.last().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome!!.outcome.details).isEqualTo("refer 2")
    }

    @Test
    fun `original nomis hearing outcome status - 1 hearing `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.NOMIS,
            adjudicator = "",
          )
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomes.size).isEqualTo(1)

      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `original nomis hearing outcome status - multiple hearing `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.NOMIS,
            adjudicator = "",
          )
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 1, oicHearingType = OicHearingType.GOV, chargeNumber = "1"),
          )
          it.hearings.last().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome).isNull()
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomes.last().outcome).isNull()
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `subsequent nomis hearing outcome status - original outcome created via adjudications`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
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
            Hearing(dateTimeOfHearing = LocalDateTime.now(), locationId = 1, agencyId = "", oicHearingId = 1, oicHearingType = OicHearingType.GOV, chargeNumber = "1"),
          )
          it.hearings.last().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = "")
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomes.size).isEqualTo(2)

      assertThat(result.outcomes.first().outcome).isNotNull
      assertThat(result.outcomes.first().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(result.outcomes.last().outcome).isNull()
      assertThat(result.outcomes.last().hearing!!.outcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
      assertThat(result.outcomeEnteredInNomis).isTrue
    }

    @Test
    fun `outcome history DTO - REFER_GOV SCHEDULE_HEARING as outcome`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.GOV_ADULT, locationId = 1, agencyId = "", chargeNumber = ""),
          )
          it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD).also { o -> o.createDateTime = LocalDateTime.now() })
          it.addOutcome(Outcome(code = OutcomeCode.REFER_GOV).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) })
          it.addOutcome(Outcome(code = OutcomeCode.SCHEDULE_HEARING).also { o -> o.createDateTime = LocalDateTime.now().plusDays(2) })
        },
      )

      val result = reportedAdjudicationService.getReportedAdjudicationDetails("1")

      assertThat(result.outcomes.size).isEqualTo(3)
      assertThat(result.outcomes.first().outcome!!.outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.outcomes.first().outcome!!.referralOutcome!!.code).isEqualTo(OutcomeCode.REFER_GOV)
      assertThat(result.outcomes[1].outcome!!.referralOutcome).isNull()
      assertThat(result.outcomes[1].hearing).isNull()
      assertThat(result.outcomes[1].outcome!!.outcome.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(result.outcomes.last().outcome).isNull()
      assertThat(result.outcomes.last().hearing).isNotNull
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
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
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

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")).isEqualTo(true)
    }

    @Test
    fun `returns false when no history `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.clear()
          it.clearOutcomes()
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")).isEqualTo(false)
    }

    @Test
    fun `returns false when no last item has no outcome `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")).isEqualTo(false)
    }

    @Test
    fun `returns false when no last item has no referral outcome `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE),
          )
        },
      )

      assertThat(reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")).isEqualTo(false)
    }
  }

  @Nested
  inner class ReadonlyAdjudication {

    @CsvSource("RETURNED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `is actionable for originating agency`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isTrue
    }

    @CsvSource("RETURNED", "AWAITING_REVIEW")
    @ParameterizedTest
    fun `is not actionable for override agency`(status: ReportedAdjudicationStatus) {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isFalse
    }

    @Test
    fun `scheduled is not actionable for override agency if the hearing belongs to the originating agency`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().agencyId = "MDI"
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isFalse
    }

    @Test
    fun `scheduled is actionable for originating agency if the hearing belongs to the originating agency`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().agencyId = "MDI"
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isTrue
    }

    @Test
    fun `scheduled is actionable for override agency if the hearing belongs to the override agency`() {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().agencyId = "LEI"
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isTrue
    }

    @Test
    fun `scheduled is not actionable for originating agency if the hearing belongs to the override agency`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.SCHEDULED
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().agencyId = "LEI"
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isFalse
    }

    @CsvSource("CHARGE_PROVED", "QUASHED", "REFER_POLICE", "REFER_INAD", "NOT_PROCEED", "PROSECUTION", "DISMISSED", "ADJOURNED", "UNSCHEDULED", "REFER_GOV")
    @ParameterizedTest
    fun `for other states it is always not actionable for originating agency`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
          it.hearings.first().agencyId = "LEI"
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isFalse
    }

    @CsvSource("CHARGE_PROVED", "QUASHED", "REFER_POLICE", "REFER_INAD", "NOT_PROCEED", "PROSECUTION", "DISMISSED", "ADJOURNED", "UNSCHEDULED", "REFER_GOV")
    @ParameterizedTest
    fun `for other states it is always actionable for override agency`(status: ReportedAdjudicationStatus) {
      whenever(authenticationFacade.activeCaseload).thenReturn("LEI")
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isTrue
    }

    @EnumSource(ReportedAdjudicationStatus::class)
    @ParameterizedTest
    fun `report is not transferable and actionable flag should be null `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isNull()
    }

    @CsvSource("ACCEPTED", "REJECTED")
    @ParameterizedTest
    fun `always returns not actionable`(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = status
          it.overrideAgencyId = "LEI"
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      val response = reportedAdjudicationService.getReportedAdjudicationDetails("1")
      assertThat(response.transferableActionsAllowed).isFalse
    }
  }

  @Nested
  inner class SetCreatedOnBehalfOf {
    private val now = LocalDateTime.now()
    private val reportedAdjudication = entityBuilder.reportedAdjudication(chargeNumber = "1")
      .also {
        it.createdByUserId = "A_SMITH"
        it.createDateTime = LocalDateTime.now()
      }

    @Test
    fun `sets created on behalf of`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      val response = reportedAdjudicationService.setCreatedOnBehalfOf("1", "officer", "some reason")

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.createdOnBehalfOfOfficer).isEqualTo("officer")
      assertThat(argumentCaptor.value.createdOnBehalfOfReason).isEqualTo("some reason")
      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class CanRemoveFlags {

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.createdByUserId = ""
          it.createDateTime = LocalDateTime.now()
          it.clearOutcomes()
          it.hearings.first().hearingOutcome = HearingOutcome(
            code = HearingOutcomeCode.COMPLETE,
            adjudicator = "",
          )
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.clearPunishments()
          it.addPunishment(
            Punishment(
              type = PunishmentType.ADDITIONAL_DAYS,
              schedule = mutableListOf(
                PunishmentSchedule(days = 1),
              ),
            ),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByPunishmentsConsecutiveChargeNumberAndPunishmentsTypeIn("12345", PunishmentType.additionalDays()))
        .thenReturn(listOf(entityBuilder.reportedAdjudication()))
    }

    @Test
    fun `outcome can not be removed as it has a linked ADA`() {
      val dto = reportedAdjudicationService.getReportedAdjudicationDetails(chargeNumber = "12345")

      assertThat(dto.outcomes.first().outcome!!.outcome.canRemove).isFalse
    }

    @Test
    fun `punishment can not be removed if consecutive to another report`() {
      val dto = reportedAdjudicationService.getReportedAdjudicationDetails(chargeNumber = "12345")

      assertThat(dto.punishments.first().canRemove).isFalse
    }
  }

  @Nested
  inner class CalculateInvalidStatuses {

    @Test
    fun `sets invalid suspended`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.EARNINGS,
              actualCreatedDate = LocalDate.now().atStartOfDay(),
              suspendedUntil = LocalDate.now(),
              schedule =
              mutableListOf(
                PunishmentSchedule(days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.INVALID_SUSPENDED)
    }

    @Test
    fun `does not sets invalid suspended as older than 6 months`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.EARNINGS,
              actualCreatedDate = LocalDate.now().atStartOfDay().minusMonths(7),
              suspendedUntil = LocalDate.now(),
              schedule =
              mutableListOf(
                PunishmentSchedule(days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
    }

    @Test
    fun `sets invalid ADA`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.PROSECUTION, actualCreatedDate = LocalDateTime.now()),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.ADDITIONAL_DAYS,
              schedule =
              mutableListOf(
                PunishmentSchedule(days = 1),
              ),
            ),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.INVALID_ADA)
    }

    @Test
    fun `sets invalid outcome `() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED, actualCreatedDate = LocalDateTime.now()),
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now().plusDays(1)),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.INVALID_OUTCOME)
    }

    @Test
    fun `sets invalid outcome if prosecuted an punishments exc ADA `() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.addOutcome(
            Outcome(code = OutcomeCode.PROSECUTION, actualCreatedDate = LocalDateTime.now()),
          )
          it.addPunishment(
            Punishment(
              type = PunishmentType.DAMAGES_OWED,
              schedule =
              mutableListOf(
                PunishmentSchedule(days = 1),
              ),
            ),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.INVALID_OUTCOME)
    }
  }

  @Nested
  inner class CalculateStatuses {

    @Test
    fun `status is adjourned`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "")
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.ADJOURNED)

      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()))
          it.addOutcome(Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = LocalDateTime.now().plusDays(1)))

          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.hearings.add(
            Hearing(
              dateTimeOfHearing = LocalDateTime.now().plusDays(1),
              oicHearingType = OicHearingType.INAD_ADULT,
              agencyId = "",
              locationId = 1,
              chargeNumber = "",
              hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
            ),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.ADJOURNED)
    }

    @Test
    fun `status is unscheduled no hearings`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.hearings.clear()
          it.clearOutcomes()
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)
    }

    @Test
    fun `status is scheduled with hearings`() {
      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)

      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "")
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.INAD_ADULT, agencyId = "", locationId = 1, chargeNumber = ""),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)

      assertThat(
        entityBuilder.reportedAdjudication().also {
          it.clearOutcomes()
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now()))
          it.addOutcome(Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = LocalDateTime.now().plusDays(1)))

          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.hearings.add(
            Hearing(dateTimeOfHearing = LocalDateTime.now().plusDays(1), oicHearingType = OicHearingType.INAD_ADULT, agencyId = "", locationId = 1, chargeNumber = ""),
          )
          it.calculateStatus()
        }.status,
      ).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)
    }
  }

  companion object {
    val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    const val INCIDENT_STATEMENT = "Example statement"
    const val INCIDENT_ROLE_CODE = "25a"
    const val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    const val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"
  }

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)

    assertThatThrownBy {
      reportedAdjudicationService.getReportedAdjudicationDetails("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      reportedAdjudicationService.setIssued("1", LocalDateTime.now())
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      reportedAdjudicationService.lastOutcomeHasReferralOutcome("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
