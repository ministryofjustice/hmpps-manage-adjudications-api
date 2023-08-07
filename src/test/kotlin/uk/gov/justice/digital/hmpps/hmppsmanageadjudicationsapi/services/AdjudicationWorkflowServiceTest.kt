package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Assertions.assertThrows
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
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.OffenceDetailsRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacySyncService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationServiceTest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftOffenceService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.ValidationChecks
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Optional

class AdjudicationWorkflowServiceTest : ReportedAdjudicationTestBase() {

  private val legacySyncService: LegacySyncService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val draftAdjudicationRepository: DraftAdjudicationRepository = mock()
  private val draftOffenceService: DraftOffenceService = mock()

  private val adjudicationWorkflowService = AdjudicationWorkflowService(
    draftAdjudicationRepository,
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    legacySyncService,
    authenticationFacade,
    telemetryClient,
    draftOffenceService,
  )

  @Nested
  inner class CreateDraftFromReported {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    private val expectedSavedDraftAdjudication = DraftAdjudication(
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      chargeNumber = "1235",
      reportByUserId = "A_SMITH",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2L,
        dateTimeOfIncident = DATE_TIME_OF_INCIDENT,
        dateTimeOfDiscovery = DATE_TIME_OF_INCIDENT.plusDays(1),
        handoverDeadline = DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
      ),
      incidentRole = IncidentRole(
        roleCode = "25a",
        associatedPrisonersNumber = "B23456",
        associatedPrisonersName = "Associated Prisoner",
      ),
      offenceDetails = mutableListOf(
        Offence(
          // offence with all data set
          offenceCode = 1002,
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "A Person",
        ),
      ),
      incidentStatement = IncidentStatement(
        completed = true,
        statement = INCIDENT_STATEMENT,
      ),
      isYouthOffender = false,
      damages = mutableListOf(
        Damage(code = DamageCode.CLEANING, details = "details", reporter = "Fred"),
      ),
      evidence = mutableListOf(
        Evidence(code = EvidenceCode.PHOTO, identifier = "identifier", details = "details", reporter = "Fred"),
      ),
      witnesses = mutableListOf(
        Witness(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred"),
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
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      whenever(draftAdjudicationRepository.save(any())).thenReturn(savedDraftAdjudication)
    }

    @Test
    fun `adds the relevant draft data to the repository`() {
      adjudicationWorkflowService.createDraftFromReportedAdjudication("123")

      verify(draftAdjudicationRepository).save(expectedSavedDraftAdjudication)
    }

    @Test
    fun `returns the correct data`() {
      val createdDraft = adjudicationWorkflowService.createDraftFromReportedAdjudication("123")

      assertThat(createdDraft)
        .extracting("prisonerNumber", "id", "adjudicationNumber", "startedByUserId", "gender")
        .contains("A12345", 1L, 1235L, "A_SMITH", Gender.MALE)
      assertThat(createdDraft.incidentDetails)
        .extracting("dateTimeOfIncident", "handoverDeadline", "locationId")
        .contains(
          DATE_TIME_OF_INCIDENT,
          DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
          2L,
        )
      assertThat(createdDraft.incidentRole)
        .extracting(
          "roleCode",
          "offenceRule.paragraphNumber",
          "offenceRule.paragraphDescription",
          "associatedPrisonersNumber",
        )
        .contains("25a", "25(a)", "Attempts to commit any of the foregoing offences:", "B23456")
      assertThat(createdDraft.offenceDetails)
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
          offenceCodeLookupService.getOffenceCode(1002, false).paragraph,
          offenceCodeLookupService.getOffenceCode(1002, false).paragraphDescription.getParagraphDescription(Gender.MALE),
          "A1234AA",
          "ABC12D",
          "A Person",
        )
      assertThat(createdDraft.incidentStatement)
        .extracting("completed", "statement")
        .contains(true, INCIDENT_STATEMENT)
      assertThat(createdDraft.damages)
        .extracting("code", "details", "reporter")
        .contains(
          Tuple(
            DamageCode.CLEANING,
            "details",
            "Fred",
          ),
        )
      assertThat(createdDraft.evidence)
        .extracting("code", "details", "reporter", "identifier")
        .contains(
          Tuple(
            EvidenceCode.PHOTO,
            "details",
            "Fred",
            "identifier",
          ),
        )
      assertThat(createdDraft.witnesses)
        .extracting("code", "firstName", "lastName", "reporter")
        .contains(
          Tuple(
            WitnessCode.OFFICER,
            "prison",
            "officer",
            "Fred",
          ),
        )
    }
  }

  @Nested
  inner class WithAValidDraftAdjudication {
    private val INCIDENT_TIME = LocalDateTime.now(clock)

    @BeforeEach
    fun beforeEach() {
      val draft = DraftAdjudication(
        id = 1,
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = incidentDetails(1L, INCIDENT_TIME),
        incidentRole = incidentRoleWithAllValuesSet(),
        offenceDetails = mutableListOf(
          Offence(offenceCode = 1002, victimOtherPersonsName = "person", victimPrisonersNumber = "prisoner", victimStaffUsername = "staff"),
        ),
        incidentStatement = IncidentStatement(statement = "test"),
        isYouthOffender = false,
      )
      draft.createDateTime = now

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(draft),
      )

      whenever(legacySyncService.requestAdjudicationCreationData()).thenReturn(123456L)
      whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
        val passedInAdjudication = it.arguments[0] as ReportedAdjudication
        passedInAdjudication.createdByUserId = "A_SMITH"
        passedInAdjudication.createDateTime = REPORTED_DATE_TIME
        passedInAdjudication.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        passedInAdjudication
      }
    }

    @Test
    fun `stores a new completed adjudication record`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())

      assertThat(reportedAdjudicationArgumentCaptor.value)
        .extracting("prisonerNumber", "chargeNumber", "originatingAgencyId", "gender")
        .contains("A12345", "123456", "MDI", Gender.MALE)

      assertThat(reportedAdjudicationArgumentCaptor.value)
        .extracting(
          "locationId",
          "dateTimeOfIncident",
          "handoverDeadline",
          "incidentRoleCode",
          "incidentRoleAssociatedPrisonersNumber",
          "incidentRoleAssociatedPrisonersName",
          "statement",
        )
        .contains(
          1L,
          INCIDENT_TIME,
          DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
          INCIDENT_ROLE_CODE,
          INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
          INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME,
          "test",
        )

      assertThat(reportedAdjudicationArgumentCaptor.value.offenceDetails)
        .extracting(
          "offenceCode",
          "victimPrisonersNumber",
          "victimStaffUsername",
          "victimOtherPersonsName",
        )
        .contains(
          Tuple(
            1002,
            "prisoner",
            "staff",
            "person",
          ),
        )

      verify(telemetryClient).trackEvent(
        DraftAdjudicationService.TELEMETRY_EVENT,
        mapOf(
          "adjudicationNumber" to "1",
          "agencyId" to "MDI",
          "reportNumber" to "123456",
        ),
        null,
      )
    }

    @Test
    fun `makes a call to prison api to get creation data`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      verify(legacySyncService).requestAdjudicationCreationData()
    }

    @Test
    fun `deletes the draft adjudication once complete`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      val argumentCaptor: ArgumentCaptor<DraftAdjudication> = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).delete(argumentCaptor.capture())

      assertThat(argumentCaptor.value)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")
    }
  }

  @Nested
  inner class CompleteDraftAdjudication {
    @Test
    fun `throws an entity not found if the draft adjudication for the supplied id does not exists`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(Optional.empty())

      assertThatThrownBy {
        adjudicationWorkflowService.completeDraftAdjudication(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("DraftAdjudication not found for 1")
    }

    @ParameterizedTest
    @EnumSource(ValidationChecks::class)
    fun `illegal state exception test`(validationCheck: ValidationChecks) {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            agencyId = "MDI",
            incidentDetails = incidentDetails(2L, now),
          ).also {
            when (validationCheck) {
              ValidationChecks.INCIDENT_ROLE -> {
                it.isYouthOffender = false
              }

              ValidationChecks.INCIDENT_ROLE_ASSOCIATED_PRISONER -> {
                it.isYouthOffender = false
                it.incidentRole = IncidentRole(
                  null,
                  "25b",
                  null,
                  null,
                )
              }

              ValidationChecks.OFFENCE_DETAILS -> {
                it.incidentRole = incidentRoleWithAllValuesSet()
                it.isYouthOffender = false
              }

              ValidationChecks.INCIDENT_STATEMENT -> {
                it.isYouthOffender = false
                it.incidentRole = incidentRoleWithAllValuesSet()
                it.offenceDetails = mutableListOf(Offence(offenceCode = 1002))
              }

              else -> {}
            }
          },
        ),
      )

      assertThatThrownBy {
        adjudicationWorkflowService.completeDraftAdjudication(1)
      }.isInstanceOf(IllegalStateException::class.java)
        .hasMessageContaining(validationCheck.errorMessage)
    }
  }

  @Nested
  inner class CompleteAPreviouslyCompletedAdjudicationCheckStateChange {
    private val INCIDENT_TIME = LocalDateTime.now(clock)
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = INCIDENT_TIME)

    @BeforeEach
    fun beforeEach() {
      whenever(authenticationFacade.currentUsername).thenReturn("Fred")

      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            chargeNumber = "123",
            agencyId = "MDI",
            incidentDetails = incidentDetails(2L, INCIDENT_TIME),
            incidentRole = incidentRoleWithNoValuesSet(),
            offenceDetails = mutableListOf(Offence(offenceCode = 1002)),
            incidentStatement = IncidentStatement(statement = "test"),
            isYouthOffender = true,
          ),
        ),
      )
      whenever(legacySyncService.requestAdjudicationCreationData()).thenReturn(123)
      whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
        val passedInAdjudication = it.arguments[0] as ReportedAdjudication
        passedInAdjudication.createdByUserId = "A_SMITH"
        passedInAdjudication.createDateTime = REPORTED_DATE_TIME
        passedInAdjudication.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        passedInAdjudication
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
    }

    @ParameterizedTest
    @CsvSource(
      "ACCEPTED",
      "REJECTED",
    )
    fun `cannot complete when the reported adjudication is in the wrong state`(from: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = from },
      )
      assertThrows(IllegalStateException::class.java) {
        adjudicationWorkflowService.completeDraftAdjudication(1)
      }
    }

    @ParameterizedTest
    @CsvSource(
      "AWAITING_REVIEW",
      "RETURNED",
    )
    fun `completes when the reported adjudication is in a correct state`(from: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = from
        },
      )
      adjudicationWorkflowService.completeDraftAdjudication(1)
      val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())
      assertThat(reportedAdjudicationArgumentCaptor.value).extracting("status")
        .isEqualTo(ReportedAdjudicationStatus.AWAITING_REVIEW)
    }
  }

  @Nested
  inner class CompleteAPreviouslyCompletedAdjudication {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = LocalDateTime.now(clock).minusDays(2))

    @BeforeEach
    fun beforeEach() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            chargeNumber = "123",
            reportByUserId = "A_SMITH",
            agencyId = "MDI",
            incidentDetails = DraftAdjudicationServiceTest.incidentDetails(1L, clock),
            incidentRole = DraftAdjudicationServiceTest.incidentRoleWithAllValuesSet(),
            offenceDetails = mutableListOf(Offence(offenceCode = 1002, victimStaffUsername = "staff", victimPrisonersNumber = "prisoner", victimOtherPersonsName = "person")),
            incidentStatement = IncidentStatement(statement = "test"),
            isYouthOffender = false,
            damages = mutableListOf(
              Damage(code = DamageCode.REDECORATION, details = "details", reporter = "Fred"),
            ),
            evidence = mutableListOf(
              Evidence(code = EvidenceCode.BAGGED_AND_TAGGED, details = "details", reporter = "Fred"),
            ),
            witnesses = mutableListOf(
              Witness(code = WitnessCode.OFFICER, firstName = "prison", lastName = "officer", reporter = "Fred"),
            ),
          ),
        ),
      )
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.damages = mutableListOf(
            ReportedDamage(code = DamageCode.CLEANING, details = "details", reporter = "Rod"),
          )
          it.evidence = mutableListOf(
            ReportedEvidence(code = EvidenceCode.PHOTO, details = "details", reporter = "Rod"),
          )
          it.witnesses = mutableListOf(
            ReportedWitness(code = WitnessCode.STAFF, firstName = "staff", lastName = "member", reporter = "Rod"),
          )
        },
      )
      whenever(legacySyncService.requestAdjudicationCreationData()).thenReturn(123)
      whenever(reportedAdjudicationRepository.save(any())).thenAnswer {
        val passedInAdjudication = it.arguments[0] as ReportedAdjudication
        passedInAdjudication.createdByUserId = "A_SMITH"
        passedInAdjudication.createDateTime = REPORTED_DATE_TIME
        passedInAdjudication
      }

      whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    }

    @Test
    fun `throws an entity not found exception if the reported adjudication for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)

      assertThatThrownBy {
        adjudicationWorkflowService.completeDraftAdjudication(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("ReportedAdjudication not found for 1")
    }

    @Test
    fun `updates the completed adjudication record`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      verify(reportedAdjudicationRepository, times(2)).findByChargeNumber("123")

      val reportedAdjudicationArgumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(reportedAdjudicationArgumentCaptor.capture())

      assertThat(reportedAdjudicationArgumentCaptor.value)
        .extracting("prisonerNumber", "chargeNumber", "originatingAgencyId")
        .contains("A12345", "1235", "MDI")

      assertThat(reportedAdjudicationArgumentCaptor.value)
        .extracting(
          "locationId",
          "dateTimeOfIncident",
          "handoverDeadline",
          "incidentRoleCode",
          "incidentRoleAssociatedPrisonersNumber",
          "incidentRoleAssociatedPrisonersName",
          "statement",
        )
        .contains(
          1L,
          LocalDateTime.now(clock),
          DATE_TIME_REPORTED_ADJUDICATION_EXPIRES,
          INCIDENT_ROLE_CODE,
          INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER,
          INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME,
          "test",
        )

      assertThat(reportedAdjudicationArgumentCaptor.value.offenceDetails)
        .extracting(
          "offenceCode",
          "victimPrisonersNumber",
          "victimStaffUsername",
          "victimOtherPersonsName",
        )
        .contains(
          Tuple(
            1002,
            "prisoner",
            "staff",
            "person",
          ),
        )
      assertThat(reportedAdjudicationArgumentCaptor.value.damages.size).isEqualTo(2)
      assertThat(reportedAdjudicationArgumentCaptor.value.damages)
        .extracting(
          "code",
          "details",
          "reporter",
        )
        .contains(
          Tuple(
            DamageCode.CLEANING,
            "details",
            "Rod",
          ),
          Tuple(
            DamageCode.REDECORATION,
            "details",
            "Fred",
          ),
        )
      assertThat(reportedAdjudicationArgumentCaptor.value.evidence)
        .extracting(
          "code",
          "details",
          "reporter",
        )
        .contains(
          Tuple(
            EvidenceCode.BAGGED_AND_TAGGED,
            "details",
            "Fred",
          ),
          Tuple(
            EvidenceCode.PHOTO,
            "details",
            "Rod",
          ),
        )
      assertThat(reportedAdjudicationArgumentCaptor.value.witnesses)
        .extracting(
          "code",
          "firstName",
          "lastName",
          "reporter",
        )
        .contains(
          Tuple(
            WitnessCode.OFFICER,
            "prison",
            "officer",
            "Fred",
          ),
          Tuple(
            WitnessCode.STAFF,
            "staff",
            "member",
            "Rod",
          ),
        )
    }

    @Test
    fun `does not call prison api to get creation data`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      verify(legacySyncService, never()).requestAdjudicationCreationData()
    }

    @Test
    fun `deletes the draft adjudication once complete`() {
      adjudicationWorkflowService.completeDraftAdjudication(1)

      val argumentCaptor: ArgumentCaptor<DraftAdjudication> = ArgumentCaptor.forClass(DraftAdjudication::class.java)
      verify(draftAdjudicationRepository).delete(argumentCaptor.capture())

      assertThat(argumentCaptor.value)
        .extracting("id", "prisonerNumber")
        .contains(1L, "A12345")
    }
  }

  @Nested
  inner class AloSetOffenceAndComplete {

    @Test
    fun `set offences and complete draft`() {
      whenever(draftAdjudicationRepository.findById(any())).thenReturn(
        Optional.of(
          DraftAdjudication(
            id = 1,
            prisonerNumber = "A12345",
            gender = Gender.MALE,
            chargeNumber = "123",
            reportByUserId = "A_SMITH",
            agencyId = "MDI",
            incidentDetails = DraftAdjudicationServiceTest.incidentDetails(1L, clock),
            incidentRole = DraftAdjudicationServiceTest.incidentRoleWithAllValuesSet(),
            offenceDetails = mutableListOf(
              Offence(offenceCode = 1002),
            ),
            incidentStatement = IncidentStatement(statement = "test"),
            isYouthOffender = false,
          ),
        ),
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )

      adjudicationWorkflowService.setOffenceDetailsAndCompleteDraft(
        id = 1,
        offenceDetails = OffenceDetailsRequestItem(offenceCode = 1),
      )
      // note: using existing code for all of this, just test interactions occur
      verify(draftOffenceService, atLeastOnce()).setOffenceDetails(any(), any())
      verify(draftAdjudicationRepository, atLeastOnce()).delete(any())
      verify(reportedAdjudicationRepository, atLeastOnce()).save(any())
    }
  }

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)

    assertThatThrownBy {
      adjudicationWorkflowService.createDraftFromReportedAdjudication("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  companion object {
    private val now = LocalDateTime.now()
    private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())
    private val entityBuilder: EntityBuilder = EntityBuilder()
    private val DATE_TIME_OF_INCIDENT = LocalDateTime.of(2010, 10, 12, 10, 0)
    private const val INCIDENT_STATEMENT = "Example statement"
    private val DATE_TIME_REPORTED_ADJUDICATION_EXPIRES = LocalDateTime.of(2010, 10, 14, 10, 0)
    private val REPORTED_DATE_TIME = DATE_TIME_OF_INCIDENT.plusDays(1)
    private const val INCIDENT_ROLE_CODE = "25a"
    private const val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER = "B23456"
    private const val INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME = "Associated Prisoner"
    private val DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE = LocalDateTime.of(2010, 10, 14, 10, 0)

    fun incidentRoleWithAllValuesSet(): IncidentRole =
      IncidentRole(null, INCIDENT_ROLE_CODE, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NUMBER, INCIDENT_ROLE_ASSOCIATED_PRISONERS_NAME)

    fun incidentRoleWithNoValuesSet(): IncidentRole =
      IncidentRole(null, null, null, null)

    fun incidentDetails(locationId: Long, clock: Clock) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = LocalDateTime.now(clock),
      dateTimeOfDiscovery = LocalDateTime.now(clock).plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
    )

    fun incidentDetails(locationId: Long, now: LocalDateTime) = IncidentDetails(
      locationId = locationId,
      dateTimeOfIncident = now,
      dateTimeOfDiscovery = now.plusDays(1),
      handoverDeadline = DATE_TIME_DRAFT_ADJUDICATION_HANDOVER_DEADLINE,
    )
  }
}
