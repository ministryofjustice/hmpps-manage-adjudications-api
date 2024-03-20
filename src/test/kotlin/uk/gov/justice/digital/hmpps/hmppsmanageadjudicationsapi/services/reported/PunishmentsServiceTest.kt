package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentCommentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReasonForChange
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.ForbiddenException
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
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.update(
        chargeNumber = "1",
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = ""),

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.updatePunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = ""),

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.deletePunishmentComment(
        chargeNumber = "1",
        punishmentCommentId = 1,

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
              mutableListOf(PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now())),
            ),
          )
        },
      ),
    )

    assertThatThrownBy {
      punishmentsService.getSuspendedPunishments(
        prisonerNumber = "",
        chargeNumber = "1",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.getReportsWithAdditionalDays(
        chargeNumber = "1",
        prisonerNumber = "1",
        punishmentType = PunishmentType.PROSPECTIVE_DAYS,

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
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
          listOf(PunishmentRequest(type = punishmentType, days = 1)),
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
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, privilegeType = PrivilegeType.OTHER, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("description missing for type PRIVILEGE - sub type OTHER")
    }

    @Test
    fun `validation error - damages owed missing amount `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.DAMAGES_OWED, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("amount missing for type DAMAGES_OWED")
    }

    @Test
    fun `validation error - earnings missing stoppage percentage `() {
      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(PunishmentRequest(type = PunishmentType.EARNINGS, days = 1)),
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
    fun `creates a set of punishments `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          ),
          PunishmentRequest(
            type = PunishmentType.PROSPECTIVE_DAYS,
            days = 1,
          ),
          PunishmentRequest(
            type = PunishmentType.ADDITIONAL_DAYS,
            consecutiveChargeNumber = "999",
            days = 1,
          ),
          PunishmentRequest(
            type = PunishmentType.REMOVAL_WING,
            days = 1,
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
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().days).isEqualTo(1)

      assertThat(response).isNotNull
    }

    @Test
    fun `activated from punishment not found `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.create(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              days = 1,
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
              days = 1,
              activatedFrom = "2",
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              days = 1,
              activatedFrom = "2",
              damagesOwedAmount = 100.0,
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.REMOVAL_WING,
              days = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("CAUTION can only include DAMAGES_OWED")
    }

    @Test
    fun `clone suspended punishment `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "2").also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.CANTEEN,
              schedule = mutableListOf(
                PunishmentSchedule(days = 10),
              ),
            ),
          )
        },
      )
      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = "2",
          ),
        ),
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findByChargeNumber("2")
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().id).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedFromChargeNumber).isEqualTo("2")

      assertThat(response).isNotNull
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
    fun `activate manual suspended punishment`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.create(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = "2",
            consecutiveChargeNumber = "12345",
          ),
        ),
      )

      verify(reportedAdjudicationRepository, never()).findByChargeNumber("2")
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().id).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedFromChargeNumber).isEqualTo("2")
      assertThat(argumentCaptor.value.getPunishments().first().consecutiveToChargeNumber).isEqualTo("12345")

      assertThat(response).isNotNull
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
          listOf(PunishmentRequest(type = punishmentType, days = 1)),
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
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 1, type = PunishmentType.PRIVILEGE, days = 1)),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
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
              days = 1,
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 1, type = PunishmentType.EARNINGS, days = 1)),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
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
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.addPunishment(
            Punishment(
              type = type,
              schedule = mutableListOf(PunishmentSchedule(days = 10)),
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
              schedule = mutableListOf(PunishmentSchedule(days = 10)),
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
            PunishmentRequest(id = 1, type = PunishmentType.EXTRA_WORK, days = 10, suspendedUntil = LocalDate.now()),
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
              schedule = mutableListOf(PunishmentSchedule(days = 10)),
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
            PunishmentRequest(id = 1, type = type, days = 10, suspendedUntil = LocalDate.now()),
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(PunishmentRequest(id = 2, type = PunishmentType.REMOVAL_ACTIVITY, days = 1, suspendedUntil = LocalDate.now())),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment 2 is not associated with ReportedAdjudication")
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
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 2,
              type = PunishmentType.EXCLUSION_WORK,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 3,
              type = PunishmentType.ADDITIONAL_DAYS,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, days = 1).also {
                  it.createDateTime = LocalDateTime.now()
                },
              ),
            ),
            Punishment(
              id = 4,
              type = PunishmentType.REMOVAL_WING,
              schedule = mutableListOf(
                PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()).also {
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
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          ),
          PunishmentRequest(
            type = PunishmentType.PROSPECTIVE_DAYS,
            days = 1,
          ),
          PunishmentRequest(
            id = 3,
            type = PunishmentType.ADDITIONAL_DAYS,
            days = 3,
          ),
          PunishmentRequest(
            id = 4,
            type = PunishmentType.REMOVAL_WING,
            days = 1,
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
      assertThat(prospectiveDays.schedule.first().days).isEqualTo(1)
      assertThat(privilege.schedule.first().startDate).isEqualTo(LocalDate.now())
      assertThat(privilege.schedule.first().endDate).isEqualTo(LocalDate.now().plusDays(1))
      assertThat(privilege.otherPrivilege).isEqualTo("other")
      assertThat(privilege.privilegeType).isEqualTo(PrivilegeType.OTHER)
    }

    @Test
    fun `activated from punishment not found `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(id = 1, type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(entityBuilder.reportedAdjudication())

      assertThatThrownBy {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 10,
              type = PunishmentType.PROSPECTIVE_DAYS,
              days = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("suspended punishment not found")
    }

    @Test
    fun `activated from punishment has already been activated `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication.also {
          it.addPunishment(
            Punishment(id = 1, activatedFromChargeNumber = "2", type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(
            Punishment(id = 3, type = PunishmentType.PROSPECTIVE_DAYS, activatedByChargeNumber = "1", schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      assertDoesNotThrow {
        punishmentsService.update(
          chargeNumber = "1",
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              days = 1,
              activatedFrom = "2",
            ),
          ),
        )
      }
    }

    @Test
    fun `clone suspended punishment `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "2").also {
          it.addPunishment(
            Punishment(
              id = 1,
              type = PunishmentType.CONFINEMENT,
              schedule = mutableListOf(
                PunishmentSchedule(days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.update(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.CONFINEMENT,
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = "2",
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(reportedAdjudicationRepository, atLeastOnce()).findByChargeNumber("2")

      assertThat(argumentCaptor.value.getPunishments().first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().id).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedFromChargeNumber).isEqualTo("2")

      assertThat(response).isNotNull
    }

    @Test
    fun `activate manual suspended punishment`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = punishmentsService.update(
        chargeNumber = "1",
        listOf(
          PunishmentRequest(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = "2",
            consecutiveChargeNumber = "12345",
          ),
        ),
      )

      verify(reportedAdjudicationRepository, never()).findByChargeNumber("2")
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first()).isNotNull
      assertThat(argumentCaptor.value.getPunishments().first().id).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedFromChargeNumber).isEqualTo("2")
      assertThat(argumentCaptor.value.getPunishments().first().consecutiveToChargeNumber).isEqualTo("12345")

      assertThat(response).isNotNull
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
              days = 1,
              activatedFrom = "2",
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              days = 1,
              activatedFrom = "2",
              damagesOwedAmount = 100.0,
            ),
            PunishmentRequest(
              id = 1,
              type = PunishmentType.REMOVAL_WING,
              days = 1,
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
              schedule = mutableListOf(PunishmentSchedule(days = 1)),
            ),
          )
        },
      )
      val response = punishmentsService.update(
        chargeNumber = "12345",
        punishments =
        listOf(
          PunishmentRequest(id = 1, type = PunishmentType.ADDITIONAL_DAYS, days = 1),
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
              schedule = mutableListOf(PunishmentSchedule(days = 1)),
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
  inner class GetSuspendedPunishments {

    private val reportedAdjudications = listOf(
      entityBuilder.reportedAdjudication(chargeNumber = "2").also {
        it.addPunishment(
          Punishment(
            type = PunishmentType.REMOVAL_WING,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "2").also {
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedByChargeNumber = "1234",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "3").also {
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedFromChargeNumber = "1234",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "4").also {
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "5").also {
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
    )

    @Test
    fun `get suspended punishments `() {
      whenever(reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(any(), any(), any())).thenReturn(reportedAdjudications)
      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndStatusInAndPunishmentsSuspendedUntilAfter(any(), any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "5").also {
            it.status = ReportedAdjudicationStatus.INVALID_OUTCOME
            it.addPunishment(
              Punishment(
                type = PunishmentType.REMOVAL_WING,
                suspendedUntil = LocalDate.now(),
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
                ),
              ),
            )
          },
        ),
      )
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
      val suspended = punishmentsService.getSuspendedPunishments("AE1234", chargeNumber = "1")

      val removalWing = suspended.first { it.punishment.type == PunishmentType.REMOVAL_WING }

      assertThat(suspended.size).isEqualTo(2)
      assertThat(suspended.any { it.corrupted }).isTrue
      assertThat(removalWing.chargeNumber).isEqualTo("2")
      assertThat(removalWing.punishment.type).isEqualTo(PunishmentType.REMOVAL_WING)
      assertThat(removalWing.punishment.schedule.days).isEqualTo(10)
      assertThat(removalWing.punishment.schedule.suspendedUntil).isEqualTo(LocalDate.now())
    }

    @CsvSource("INAD_ADULT", "INAD_YOI")
    @ParameterizedTest
    fun `get suspended punishments for reported adjudication from independent adjudicator `(oicHearingType: OicHearingType) {
      whenever(reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(any(), any(), any())).thenReturn(reportedAdjudications)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().oicHearingType = oicHearingType
        },
      )

      val suspended = punishmentsService.getSuspendedPunishments("AE1234", chargeNumber = "1")

      val removalWing = suspended.first { it.punishment.type == PunishmentType.REMOVAL_WING }
      val additionalDays = suspended.first { it.punishment.type == PunishmentType.ADDITIONAL_DAYS }
      val prospectiveDays = suspended.first { it.punishment.type == PunishmentType.PROSPECTIVE_DAYS }

      assertThat(removalWing).isNotNull
      assertThat(additionalDays).isNotNull
      assertThat(prospectiveDays).isNotNull

      assertThat(suspended.size).isEqualTo(3)
    }
  }

  @Nested
  inner class GetAdditionalDaysReports {

    private val reportedAdjudications = listOf(
      entityBuilder.reportedAdjudication(chargeNumber = "1").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "1").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "1").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now().plusDays(1)
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(chargeNumber = "1").also {
        it.hearings.first().dateTimeOfHearing = LocalDateTime.now().plusDays(1)
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            consecutiveToChargeNumber = "12345",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
    )

    @EnumSource(PunishmentType::class)
    @ParameterizedTest
    fun `throws exception if punishment type in request not additional days or prospective days`(punishmentType: PunishmentType) {
      if (listOf(PunishmentType.ADDITIONAL_DAYS, PunishmentType.PROSPECTIVE_DAYS).contains(punishmentType)) return

      assertThatThrownBy {
        punishmentsService.getReportsWithAdditionalDays(
          chargeNumber = "",
          prisonerNumber = "",
          punishmentType = punishmentType,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Punishment type must be ADDITIONAL_DAYS or PROSPECTIVE_DAYS")
    }

    @CsvSource("ADDITIONAL_DAYS", "PROSPECTIVE_DAYS")
    @ParameterizedTest
    fun `get additional days reports`(punishmentType: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(ReportedAdjudicationStatus.CHARGE_PROVED, "AE1234", punishmentType)).thenReturn(
        reportedAdjudications,
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
          it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        },
      )

      val additionalDaysReports = punishmentsService.getReportsWithAdditionalDays(
        chargeNumber = "12345",
        prisonerNumber = "AE1234",
        punishmentType = punishmentType,
      )

      assertThat(additionalDaysReports.size).isEqualTo(1)
      assertThat(additionalDaysReports.first().punishment.type).isEqualTo(punishmentType)
      assertThat(additionalDaysReports.first().chargeNumber).isEqualTo("1")
      assertThat(additionalDaysReports.first().punishment.schedule.days).isEqualTo(10)
      assertThat(additionalDaysReports.first().punishment.consecutiveChargeNumber).isEqualTo("12345")
      assertThat(additionalDaysReports.first().chargeProvedDate).isEqualTo(reportedAdjudications.first().hearings.first().dateTimeOfHearing.toLocalDate())
    }
  }

  @Nested
  inner class CreatePunishmentComment {

    @Test
    fun `Punishment comment created`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication()

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentsService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = "some text"),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.punishmentComments[0].comment).isEqualTo("some text")
    }

    @Test
    fun `punishment comment with reason for change`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentsService.createPunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(comment = "some text", reasonForChange = ReasonForChange.APPEAL),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.punishmentComments[0].reasonForChange).isEqualTo(ReasonForChange.APPEAL)
    }
  }

  @Nested
  inner class UpdatePunishmentComment {

    @Test
    fun `Punishment comment not found`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.updatePunishmentComment(
          chargeNumber = "1",
          punishmentComment = PunishmentCommentRequest(id = -1, comment = "new text"),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment comment id -1 is not found")
    }

    @Test
    fun `Only author can update comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.updatePunishmentComment(
          chargeNumber = "1",
          punishmentComment = PunishmentCommentRequest(id = 2, comment = "new text"),
        )
      }.isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("Only author can carry out action on punishment comment. attempt by ITAG_USER")
    }

    @Test
    fun `Update punishment comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("author")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentsService.updatePunishmentComment(
        chargeNumber = "1",
        punishmentComment = PunishmentCommentRequest(id = 2, comment = "new text"),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.punishmentComments[0].comment).isEqualTo("new text")
    }
  }

  @Nested
  inner class DeletePunishmentComment {

    @Test
    fun `Punishment comment not found`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.deletePunishmentComment(
          chargeNumber = "1",
          punishmentCommentId = -1,
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment comment id -1 is not found")
    }

    @Test
    fun `Only author can delete comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "old text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("ITAG_USER")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.deletePunishmentComment(
          chargeNumber = "1",
          punishmentCommentId = 2,
        )
      }.isInstanceOf(ForbiddenException::class.java)
        .hasMessageContaining("Only author can carry out action on punishment comment. attempt by ITAG_USER")
    }

    @Test
    fun `Delete punishment comment`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishmentComments.add(
          PunishmentComment(id = 2, comment = "some text").also { punishmentComment ->
            punishmentComment.createdByUserId = "author"
            punishmentComment.createDateTime = LocalDateTime.now()
          },
        )
      }

      whenever(authenticationFacade.currentUsername).thenReturn("author")
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(reportedAdjudication)).thenReturn(reportedAdjudication)
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      punishmentsService.deletePunishmentComment(
        chargeNumber = "1",
        punishmentCommentId = 2,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.punishmentComments.size).isEqualTo(0)
    }
  }

  @Nested
  inner class GetActivePunishments {

    @Test
    fun `get activated from with other privilege`() {
      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.PRIVILEGE,
                privilegeType = PrivilegeType.OTHER,
                activatedFromChargeNumber = "12345",
                otherPrivilege = "test",
                schedule = mutableListOf(
                  PunishmentSchedule(days = 5, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(5)),
                ),
              ),
            )
          },
        ),
      )

      val response = punishmentsService.getActivePunishments(offenderBookingId = 1)

      assertThat(response.first().amount).isNull()
      assertThat(response.first().stoppagePercentage).isNull()
      assertThat(response.first().punishmentType).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(response.first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(response.first().otherPrivilege).isNotNull
      assertThat(response.first().startDate).isEqualTo(LocalDate.now())
      assertThat(response.first().lastDay).isEqualTo(LocalDate.now().plusDays(5))
      assertThat(response.first().days).isNotNull
      assertThat(response.first().activatedFrom).isEqualTo("12345")
    }

    @Test
    fun `get with stoppage percentage`() {
      whenever(
        reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.EARNINGS,
                stoppagePercentage = 90,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 0, startDate = LocalDate.now(), endDate = LocalDate.now()),
                ),
              ),
            )
          },
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.EARNINGS,
                stoppagePercentage = 90,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 0, startDate = LocalDate.now().minusDays(2), endDate = LocalDate.now().minusDays(2)),
                ),
              ),
            )
          },
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.EARNINGS,
                suspendedUntil = LocalDate.now(),
                stoppagePercentage = 90,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 0, startDate = LocalDate.now().minusDays(2), endDate = LocalDate.now().minusDays(2)),
                ),
              ),
            )
          },
        ),
      )

      val response = punishmentsService.getActivePunishments(offenderBookingId = 1)

      assertThat(response.size).isEqualTo(1)
      assertThat(response.first().amount).isNull()
      assertThat(response.first().stoppagePercentage).isNotNull
    }
  }

  @Nested
  inner class TransferRelatedActivations {

    @Test
    fun `activate suspended punishments from a report in a previous prison should not apply the security check when updating record`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        },
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.originatingAgencyId = "RDI"
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.clearPunishments()
          it.addPunishment(
            Punishment(id = 1, type = PunishmentType.REMOVAL_ACTIVITY, schedule = mutableListOf(PunishmentSchedule(days = 10))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(entityBuilder.reportedAdjudication())

      assertDoesNotThrow {
        punishmentsService.create(
          chargeNumber = "2",
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1, activatedFrom = "1", id = 1, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))),
        )
      }
    }
  }

  companion object {

    fun getRequest(id: Long? = null, type: PunishmentType, startDate: LocalDate? = null, endDate: LocalDate? = null): PunishmentRequest =
      when (type) {
        PunishmentType.PRIVILEGE -> PunishmentRequest(id = id, type = type, privilegeType = PrivilegeType.ASSOCIATION, days = 1, startDate = startDate, endDate = endDate)
        PunishmentType.EARNINGS -> PunishmentRequest(id = id, type = type, stoppagePercentage = 10, days = 1, startDate = startDate, endDate = endDate)

        else -> PunishmentRequest(id = id, type = type, days = 1, startDate = startDate, endDate = endDate)
      }
  }
}
