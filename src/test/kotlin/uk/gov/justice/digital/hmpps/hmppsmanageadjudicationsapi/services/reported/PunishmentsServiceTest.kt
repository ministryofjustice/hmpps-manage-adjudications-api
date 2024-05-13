package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import java.time.LocalDate
import java.time.LocalDateTime

class PunishmentsServiceTest : ReportedAdjudicationTestBase() {

  private val punishmentsService = PunishmentsService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    assertThatThrownBy {
      punishmentsService.create(
        chargeNumber = "1",
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, duration = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.update(
        chargeNumber = "1",
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, duration = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    whenever(reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(any(), any(), any())).thenReturn(
      listOf(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(
            Punishment(
              type = PunishmentType.REMOVAL_WING,
              suspendedUntil = LocalDate.now(),
              schedule =
              mutableListOf(PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now())),
            ),
          )
        },
      ),
    )
  }

  @Nested
  inner class CreatePunishments {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().oicHearingType = OicHearingType.INAD_ADULT
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `throws a runtime exception if punishments already exist - ie the user has pressed back and resubmitted`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(Punishment(type = PunishmentType.CONFINEMENT, schedule = mutableListOf(PunishmentSchedule(duration = 0))))
        },
      )
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.CONFINEMENT, duration = 1)),
        )
      }.isInstanceOf(RuntimeException::class.java)
        .hasMessageContaining("This charge already has punishments - back key detected")
    }

    @CsvSource("ADDITIONAL_DAYS", "PROSPECTIVE_DAYS")
    @ParameterizedTest
    fun `throws exception if not inad hearing `(punishmentType: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().oicHearingType = OicHearingType.GOV
        },
      )

      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = punishmentType, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Punishment $punishmentType is invalid as the punishment decision was not awarded by an independent adjudicator")
    }

    @CsvSource(
      "ADJOURNED", "REFER_POLICE", "REFER_INAD", "SCHEDULED", "UNSCHEDULED", "AWAITING_REVIEW", "PROSECUTION",
      "NOT_PROCEED", "DISMISSED", "REJECTED", "RETURNED", "REFER_GOV",
    )
    @ParameterizedTest
    fun `validation error - wrong status code - must be CHARGE_PROVED `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = status },
      )
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, privilegeType = PrivilegeType.OTHER, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("description missing for type PRIVILEGE - sub type OTHER")
    }

    @Test
    fun `validation error - damages owed missing amount `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.DAMAGES_OWED, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("amount missing for type DAMAGES_OWED")
    }

    @Test
    fun `validation error - payback punishment is missing notes`() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PAYBACK, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("paybackNotes missing for type PAYBACK")
    }

    @Test
    fun `validation error - payback punishment is missing end date`() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PAYBACK, duration = 1, paybackNotes = "testing")),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @Test
    fun `validation error - earnings missing stoppage percentage `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.EARNINGS, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("stoppage percentage missing for type EARNINGS")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing start date `(type: PunishmentType) {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(getRequest(type = type, endDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing start date for schedule")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing end date `(type: PunishmentType) {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(getRequest(type = type, startDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - suspended missing all schedule dates `(type: PunishmentType) {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(getRequest(type = type)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing all schedule data")
    }

    @Test
    fun `payback punishment should use charge proved hearing date as start date`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            type = PunishmentType.PAYBACK,
            paybackNotes = "notes",
            duration = 1,
            endDate = LocalDate.now().plusDays(1),
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isEqualTo(reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate())
      assertThat(argumentCaptor.value.getPunishments().first().paybackNotes).isEqualTo("notes")
    }

    @Test
    fun `creates a set of punishments `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            duration = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          ),
          PunishmentRequest(
            type = PunishmentType.PROSPECTIVE_DAYS,
            duration = 1,
          ),
          PunishmentRequest(
            type = PunishmentType.ADDITIONAL_DAYS,
            consecutiveChargeNumber = "999",
            duration = 1,
          ),
          PunishmentRequest(
            type = PunishmentType.REMOVAL_WING,
            duration = 1,
            suspendedUntil = LocalDate.now(),
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      val removalWing = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.REMOVAL_WING }
      val additionalDays = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.ADDITIONAL_DAYS }

      assertThat(removalWing.suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(additionalDays.consecutiveToChargeNumber).isEqualTo("999")

      assertThat(argumentCaptor.value.getPunishments().size).isEqualTo(4)
      assertThat(argumentCaptor.value.getPunishments().first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(argumentCaptor.value.getPunishments().first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(argumentCaptor.value.getPunishments().first().otherPrivilege).isEqualTo("other")
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate)
        .isEqualTo(LocalDate.now())
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate)
        .isEqualTo(LocalDate.now().plusDays(1))
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().duration).isEqualTo(1)

      assertThat(response).isNotNull
    }

    @Test
    fun `activated from charge is not found`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("2"))).thenReturn(emptyList())

      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("activated from charge 2 not found")
    }

    @Test
    fun `activated from punishment not found `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("2"))).thenReturn(listOf(entityBuilder.reportedAdjudication(chargeNumber = "2")))

      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("suspended punishment not found")
    }

    @Test
    fun `validation error - punishments contains a caution and non applicable caution punishments`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.CAUTION,
              duration = 1,
              activatedFrom = "2",
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              duration = 1,
              activatedFrom = "2",
              damagesOwedAmount = 100.0,
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.REMOVAL_WING,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("CAUTION can only include DAMAGES_OWED")
    }

    @Test
    fun `saves caution `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.CAUTION,
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      val caution = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.CAUTION }

      assertThat(caution).isNotNull
      assertThat(response).isNotNull
    }

    @Test
    fun `saves damages owed and caution `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.CAUTION,
          ),
          PunishmentRequest(
            id = 1,
            type = PunishmentType.DAMAGES_OWED,
            damagesOwedAmount = 100.0,
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      val caution = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.CAUTION }
      val damagesOwed = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.DAMAGES_OWED }

      assertThat(caution).isNotNull
      assertThat(damagesOwed).isNotNull
      assertThat(damagesOwed.amount).isEqualTo(100.0)

      assertThat(argumentCaptor.value.getPunishments().size).isEqualTo(2)
      assertThat(response).isNotNull
    }

    @Test
    fun `exception is raised if no punishment id is provided for activation`() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.OTHER,
              otherPrivilege = "other",
              duration = 1,
              startDate = LocalDate.now(),
              endDate = LocalDate.now().plusDays(1),
              activatedFrom = "2",
              consecutiveChargeNumber = "12345",
            ),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Suspended punishment activation missing punishment id to activate")
    }
  }

  @Nested
  inner class UpdatePunishments {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.clearPunishments()
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().oicHearingType = OicHearingType.INAD_ADULT
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @CsvSource("ADDITIONAL_DAYS", "PROSPECTIVE_DAYS")
    @ParameterizedTest
    fun `throws exception if not inad hearing `(punishmentType: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().oicHearingType = OicHearingType.GOV
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = punishmentType, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Punishment $punishmentType is invalid as the punishment decision was not awarded by an independent adjudicator")
    }

    @CsvSource(
      "ADJOURNED", "REFER_POLICE", "REFER_INAD", "SCHEDULED", "UNSCHEDULED", "AWAITING_REVIEW", "PROSECUTION",
      "NOT_PROCEED", "DISMISSED", "REJECTED", "RETURNED", "REFER_GOV",
    )
    @ParameterizedTest
    fun `validation error - wrong status code - must be CHARGE_PROVED `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = status },
      )
      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.ASSOCIATION,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 1, type = PunishmentType.PRIVILEGE, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.OTHER,
              otherPrivilege = "",
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.OTHER,
              duration = 1,
            ),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("description missing for type PRIVILEGE - sub type OTHER")
    }

    @Test
    fun `validation error - earnings missing stoppage percentage `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.EARNINGS,
              stoppagePercentage = 10,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 1, type = PunishmentType.EARNINGS, duration = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("stoppage percentage missing for type EARNINGS")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing start date `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = type,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(getRequest(id = 1, type = type, endDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing start date for schedule")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing end date `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = type,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            getRequest(id = 1, type = type, startDate = LocalDate.now()),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @CsvSource(
      "PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - suspended missing all schedule dates `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = type,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(getRequest(id = 1, type = type)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing all schedule data")
    }

    @CsvSource("PROSPECTIVE_DAYS", "ADDITIONAL_DAYS")
    @ParameterizedTest
    fun `throws validation exception when deleting a punishment linked to another report`(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "1").also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.addPunishment(
            Punishment(
              type = type,
              schedule = mutableListOf(PunishmentSchedule(duration = 10)),
            ),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn("1", listOf(type))).thenReturn(
        listOf(entityBuilder.reportedAdjudication(chargeNumber = "1234")),
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          punishments = emptyList(),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to modify: $type is linked to another report")
    }

    @CsvSource("PROSPECTIVE_DAYS", "ADDITIONAL_DAYS")
    @ParameterizedTest
    fun `throws validation exception when amending a punishment to a non additional days type that is linked to another report `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.addPunishment(
            Punishment(
              id = 1,
              type = type,
              schedule = mutableListOf(PunishmentSchedule(duration = 10)),
            ),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn("1", listOf(type))).thenReturn(
        listOf(entityBuilder.reportedAdjudication(chargeNumber = "1234")),
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          punishments = listOf(
            PunishmentRequest(id = 1, type = PunishmentType.EXTRA_WORK, duration = 10, suspendedUntil = LocalDate.now()),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to modify: $type is linked to another report")
    }

    @CsvSource("PROSPECTIVE_DAYS", "ADDITIONAL_DAYS")
    @ParameterizedTest
    fun `throws validation exception if amended to suspended and linked to consecutive report`(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().oicHearingType = OicHearingType.INAD_ADULT
          it.addPunishment(
            Punishment(
              id = 1,
              type = type,
              schedule = mutableListOf(PunishmentSchedule(duration = 10)),
            ),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn("1", listOf(type))).thenReturn(
        listOf(entityBuilder.reportedAdjudication(chargeNumber = "1234")),
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          punishments = listOf(
            PunishmentRequest(id = 1, type = type, duration = 10, suspendedUntil = LocalDate.now()),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to modify: $type is linked to another report")
    }

    @Test
    fun `throws exception if id for punishment is not located `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.CONFINEMENT,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 2, type = PunishmentType.REMOVAL_ACTIVITY, duration = 1, suspendedUntil = LocalDate.now())),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment 2 is not associated with ReportedAdjudication")
    }

    @Test
    fun `payback punishment should not overwrite the start date`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.PAYBACK,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, startDate = reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
          )
        },
      )

      punishmentsService.update(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.PAYBACK,
            paybackNotes = "notes",
            duration = 1,
            endDate = LocalDate.now().plusDays(2),
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      argumentCaptor.value.getPunishments().first().schedule.forEach {
        assertThat(it.startDate).isEqualTo(reportedAdjudication.getLatestHearing()!!.dateTimeOfHearing.toLocalDate())
      }
      assertThat(argumentCaptor.value.getPunishments().first().paybackNotes).isEqualTo("notes")
    }

    @Test
    fun `update punishments `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          mutableListOf(
            Punishment(
              id = 1,
              type = PunishmentType.CONFINEMENT,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 2,
              type = PunishmentType.EXCLUSION_WORK,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 3,
              type = PunishmentType.ADDITIONAL_DAYS,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 4,
              type = PunishmentType.REMOVAL_WING,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, duration = 1, suspendedUntil = LocalDate.now()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
          ).forEach { p -> it.addPunishment(p) }
        },
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = punishmentsService.update(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            duration = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          ),
          PunishmentRequest(
            type = PunishmentType.PROSPECTIVE_DAYS,
            duration = 1,
          ),
          PunishmentRequest(
            id = 3,
            type = PunishmentType.ADDITIONAL_DAYS,
            duration = 3,
          ),
          PunishmentRequest(
            id = 4,
            type = PunishmentType.REMOVAL_WING,
            duration = 1,
            suspendedUntil = LocalDate.now(),
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(response).isNotNull
      assertThat(argumentCaptor.value.getPunishments().size).isEqualTo(4)

      val privilege = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.PRIVILEGE }
      val additionalDays = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.ADDITIONAL_DAYS }
      val prospectiveDays = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.PROSPECTIVE_DAYS }
      val removalWing = argumentCaptor.value.getPunishments().first { it.type == PunishmentType.REMOVAL_WING }
      assertThat(argumentCaptor.value.getPunishments().firstOrNull { it.type == PunishmentType.EXCLUSION_WORK })

      assertThat(privilege.id).isNull()
      assertThat(additionalDays.id).isEqualTo(3)
      assertThat(additionalDays.schedule.size).isEqualTo(2)
      assertThat(prospectiveDays.schedule.size).isEqualTo(1)
      assertThat(removalWing.schedule.size).isEqualTo(1)
      assertThat(removalWing.suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(prospectiveDays.schedule.first().duration).isEqualTo(1)
      assertThat(privilege.schedule.first().startDate).isEqualTo(LocalDate.now())
      assertThat(privilege.schedule.first().endDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(privilege.otherPrivilege).isEqualTo("other")
      assertThat(privilege.privilegeType).isEqualTo(PrivilegeType.OTHER)
    }

    @Test
    fun `activated from charge is not found `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(id = 1, type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(duration = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("2"))).thenReturn(emptyList())

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 10,
              type = PunishmentType.PROSPECTIVE_DAYS,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("activated from charge 2 not found")
    }

    @Test
    fun `activated from punishment not found `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(id = 1, type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(duration = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("2"))).thenReturn(listOf(entityBuilder.reportedAdjudication(chargeNumber = "2")))

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 10,
              type = PunishmentType.PROSPECTIVE_DAYS,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("suspended punishment not found")
    }

    @Test
    fun `validation error - punishments contains a caution and non applicable caution punishments`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.CAUTION,
              duration = 1,
              activatedFrom = "2",
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              duration = 1,
              activatedFrom = "2",
              damagesOwedAmount = 100.0,
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.REMOVAL_WING,
              duration = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("CAUTION can only include DAMAGES_OWED")
    }

    @Test
    fun `changing a punishment from consecutive to concurrent removes the consecutive to charge`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(
        reportedAdjudication.also {
          it.clearPunishments()
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.ADDITIONAL_DAYS,
              consecutiveToChargeNumber = "1234",
              schedule = mutableListOf(PunishmentSchedule(duration = 1)),
            ),
          )
        },
      )
      val response = punishmentsService.update(
        chargeNumber = "12345",
        punishments =
        listOf(
          PunishmentRequest(id = 1, type = PunishmentType.ADDITIONAL_DAYS, duration = 1),
        ),
      )

      assertThat(response.punishments.first().consecutiveChargeNumber).isNull()
    }

    @Test
    fun `changing damages owed amount updates new amount`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(
        reportedAdjudication.also {
          it.clearPunishments()
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              amount = 100.0,
              schedule = mutableListOf(PunishmentSchedule(duration = 1)),
            ),
          )
        },
      )
      val response = punishmentsService.update(
        chargeNumber = "12345",
        punishments =
        listOf(
          PunishmentRequest(id = 1, type = PunishmentType.DAMAGES_OWED, damagesOwedAmount = 99.9),
        ),
      )

      assertThat(response.punishments.first().damagesOwedAmount).isEqualTo(99.9)
    }
  }

  @Nested
  inner class ActivatePunishmentsV2 {

    private val punishmentsServiceV2 = PunishmentsService(
      reportedAdjudicationRepository,
      offenceCodeLookupService,
      authenticationFacade,
    )

    private val currentCharge = entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
      it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      it.clearPunishments()
      it.hearings.first().oicHearingType = OicHearingType.INAD_ADULT
    }

    private val reportToActivateFrom = entityBuilder.reportedAdjudication(chargeNumber = "activated").also {
      it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          id = 1,
          type = PunishmentType.ADDITIONAL_DAYS,
          suspendedUntil = LocalDate.now(),
          schedule =
          mutableListOf(
            PunishmentSchedule(id = 1, duration = 10, suspendedUntil = LocalDate.now())
              .also { s -> s.createDateTime = LocalDateTime.now() },
          ),
        ),
      )
      it.addPunishment(
        Punishment(
          id = 2,
          type = PunishmentType.CONFINEMENT,
          suspendedUntil = LocalDate.now(),
          schedule =
          mutableListOf(
            PunishmentSchedule(id = 1, duration = 10, suspendedUntil = LocalDate.now())
              .also { s -> s.createDateTime = LocalDateTime.now() },
          ),
        ),
      )
    }

    @Test
    fun `activated suspended punishments are not persisted (cloned) and the parent record is updated instead`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(currentCharge)
      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("activated"))).thenReturn(
        listOf(reportToActivateFrom),
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(currentCharge)

      val response = punishmentsServiceV2.create(
        chargeNumber = "12345",
        punishments = listOf(
          PunishmentRequest(id = 1, type = PunishmentType.ADDITIONAL_DAYS, duration = 10, activatedFrom = "activated"),
          PunishmentRequest(
            id = 2,
            type = PunishmentType.CONFINEMENT,
            duration = 10,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(10),
            activatedFrom = "activated",
          ),
        ),
      )

      assertThat(currentCharge.getPunishments()).isEmpty()
      val ada = reportToActivateFrom.getPunishments().first { it.id == 1L }
      val cc = reportToActivateFrom.getPunishments().first { it.id == 2L }
      assertThat(ada.suspendedUntil).isNull()
      assertThat(cc.suspendedUntil).isNull()
      assertThat(ada.activatedByChargeNumber).isEqualTo(currentCharge.chargeNumber)
      assertThat(cc.activatedByChargeNumber).isEqualTo(currentCharge.chargeNumber)
      assertThat(cc.schedule.first { it.id == null }.startDate).isEqualTo(LocalDate.now())
      assertThat(ada.schedule.first { it.id == null }.startDate).isNull()
      assertThat(ada.schedule.first { it.id == null }.endDate).isNull()
      assertThat(ada.schedule.first { it.id == null }.suspendedUntil).isNull()
      assertThat(cc.schedule.first { it.id == null }.suspendedUntil).isNull()
      assertThat(cc.schedule.first { it.id == null }.endDate).isEqualTo(LocalDate.now().plusDays(10))

      assertThat(response.suspendedPunishmentEvents!!.size).isEqualTo(1)
      assertThat(response.suspendedPunishmentEvents!!.first()).isEqualTo(
        SuspendedPunishmentEvent(chargeNumber = reportToActivateFrom.chargeNumber, agencyId = reportToActivateFrom.originatingAgencyId, status = reportToActivateFrom.status),
      )
    }

    @Test
    fun `update activated suspended punishments are not persisted (cloned) and the parent record is updated instead`() {
      reportToActivateFrom.also {
        // ensure this is not updated again
        it.getPunishments().first { p -> p.id == 2L }.activatedByChargeNumber = "activated"
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(currentCharge)
      whenever(reportedAdjudicationRepository.findByChargeNumberIn(listOf("activated"))).thenReturn(
        listOf(reportToActivateFrom),
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(currentCharge)

      val response = punishmentsServiceV2.update(
        chargeNumber = "12345",
        punishments = listOf(
          PunishmentRequest(id = 1, type = PunishmentType.ADDITIONAL_DAYS, duration = 10, activatedFrom = "activated"),
          PunishmentRequest(
            id = 2,
            type = PunishmentType.CONFINEMENT,
            duration = 10,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(10),
            activatedFrom = "activated",
          ),
        ),
      )

      assertThat(currentCharge.getPunishments()).isEmpty()
      val ada = reportToActivateFrom.getPunishments().first { it.id == 1L }
      val cc = reportToActivateFrom.getPunishments().first { it.id == 2L }
      assertThat(cc.schedule.size).isEqualTo(1)
      assertThat(ada.suspendedUntil).isNull()
      assertThat(ada.activatedByChargeNumber).isEqualTo(currentCharge.chargeNumber)
      assertThat(ada.schedule.first { it.id == null }.startDate).isNull()
      assertThat(ada.schedule.first { it.id == null }.endDate).isNull()
      assertThat(ada.schedule.first { it.id == null }.suspendedUntil).isNull()

      assertThat(response.suspendedPunishmentEvents!!.size).isEqualTo(1)
      assertThat(response.suspendedPunishmentEvents!!.first()).isEqualTo(
        SuspendedPunishmentEvent(chargeNumber = reportToActivateFrom.chargeNumber, agencyId = reportToActivateFrom.originatingAgencyId, status = reportToActivateFrom.status),
      )
    }

    @Test
    fun `deactivated suspended punishments are not persisted (cloned) and the parent record is updated instead`() {
      // need to activate them.
      reportToActivateFrom.also {
        it.getPunishments().forEach {
          it.suspendedUntil = null
          it.activatedByChargeNumber = currentCharge.chargeNumber
          it.schedule.add(
            PunishmentSchedule(id = 2, startDate = LocalDate.now(), endDate = LocalDate.now(), duration = 10).also {
                s ->
              s.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        }
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(currentCharge)
      whenever(reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber("12345")).thenReturn(
        listOf(reportToActivateFrom),
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(currentCharge)

      val response = punishmentsServiceV2.update(
        chargeNumber = "12345",
        punishments = listOf(
          PunishmentRequest(type = PunishmentType.EXCLUSION_WORK, duration = 10, startDate = LocalDate.now(), endDate = LocalDate.now()),
        ),
      )
      assertThat(currentCharge.getPunishments().size).isEqualTo(1)
      val ada = reportToActivateFrom.getPunishments().first { it.id == 1L }
      val cc = reportToActivateFrom.getPunishments().first { it.id == 2L }
      assertThat(ada.suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(cc.suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(ada.activatedByChargeNumber).isNull()
      assertThat(cc.activatedByChargeNumber).isNull()
      assertThat(ada.schedule.latestSchedule().startDate).isNull()
      assertThat(ada.schedule.latestSchedule().endDate).isNull()
      assertThat(ada.schedule.latestSchedule().suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(cc.schedule.latestSchedule().startDate).isNull()
      assertThat(cc.schedule.latestSchedule().endDate).isNull()
      assertThat(cc.schedule.latestSchedule().suspendedUntil).isEqualTo(LocalDate.now())

      assertThat(response.suspendedPunishmentEvents!!.size).isEqualTo(1)
      assertThat(response.suspendedPunishmentEvents!!.first()).isEqualTo(
        SuspendedPunishmentEvent(chargeNumber = reportToActivateFrom.chargeNumber, agencyId = reportToActivateFrom.originatingAgencyId, status = reportToActivateFrom.status),
      )
    }
  }

  companion object {

    fun getRequest(id: Long? = null, type: PunishmentType, startDate: LocalDate? = null, endDate: LocalDate? = null): PunishmentRequest =
      when (type) {
        PunishmentType.PRIVILEGE -> PunishmentRequest(id = id, type = type, privilegeType = PrivilegeType.ASSOCIATION, duration = 1, startDate = startDate, endDate = endDate)
        PunishmentType.EARNINGS -> PunishmentRequest(id = id, type = type, stoppagePercentage = 10, duration = 1, startDate = startDate, endDate = endDate)

        else -> PunishmentRequest(id = id, type = type, duration = 1, startDate = startDate, endDate = endDate)
      }
  }
}
