package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import java.time.LocalDate
import java.time.LocalDateTime

class PunishmentsServiceTest : ReportedAdjudicationTestBase() {

  private val prisonApiGateway: PrisonApiGateway = mock()

  private val punishmentsService = PunishmentsService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
    prisonApiGateway,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    assertThatThrownBy {
      punishmentsService.create(
        adjudicationNumber = 1,
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.update(
        adjudicationNumber = 1,
        listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreatePunishments {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.outcomes.add(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @CsvSource(
      "ADJOURNED", "REFER_POLICE", "REFER_INAD", "SCHEDULED", "UNSCHEDULED", "AWAITING_REVIEW", "PROSECUTION",
      "NOT_PROCEED", "DISMISSED", "REJECTED", "RETURNED",
    )
    @ParameterizedTest
    fun `validation error - wrong status code - must be CHARGE_PROVED `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = status },
      )
      assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, privilegeType = PrivilegeType.OTHER, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("description missing for type PRIVILEGE - sub type OTHER")
    }

    @Test
    fun `validation error - earnings missing stoppage percentage `() {
      assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.EARNINGS, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("stoppage percentage missing for type EARNINGS")
    }

    @CsvSource(
      " PRIVILEGE",
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
          adjudicationNumber = 1,
          listOf(getRequest(type = type, endDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing start date for schedule")
    }

    @CsvSource(
      " PRIVILEGE",
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
          adjudicationNumber = 1,
          listOf(getRequest(type = type, startDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @CsvSource(
      " PRIVILEGE",
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
          adjudicationNumber = 1,
          listOf(getRequest(type = type)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing all schedule data")
    }

    @Test
    fun `creates a set of punishments `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = punishmentsService.create(
        adjudicationNumber = 1,
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
      verify(prisonApiGateway, atLeastOnce()).createSanctions(any(), any())

      val removalWing = argumentCaptor.value.punishments.first { it.type == PunishmentType.REMOVAL_WING }
      assertThat(removalWing.suspendedUntil).isEqualTo(LocalDate.now())

      assertThat(argumentCaptor.value.punishments.size).isEqualTo(4)
      assertThat(argumentCaptor.value.punishments.first()).isNotNull
      assertThat(argumentCaptor.value.punishments.first().type).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(argumentCaptor.value.punishments.first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(argumentCaptor.value.punishments.first().otherPrivilege).isEqualTo("other")
      assertThat(argumentCaptor.value.punishments.first().schedule.first()).isNotNull
      assertThat(argumentCaptor.value.punishments.first().schedule.first().startDate)
        .isEqualTo(LocalDate.now())
      assertThat(argumentCaptor.value.punishments.first().schedule.first().endDate)
        .isEqualTo(LocalDate.now().plusDays(1))
      assertThat(argumentCaptor.value.punishments.first().schedule.first().days).isEqualTo(1)

      assertThat(response).isNotNull
    }

    @Test
    fun `activated from punishment not found `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              days = 1,
              activatedFrom = 2,
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("suspended punishment not found")
    }

    @Test
    fun `clone suspended punishment `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(
        entityBuilder.reportedAdjudication(2).also {
          it.punishments.add(
            Punishment(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              schedule = mutableListOf(
                PunishmentSchedule(days = 10),
              ),
            ),
          )
        },
      )
      val response = punishmentsService.create(
        adjudicationNumber = 1,
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = 2,
          ),
        ),
      )

      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(
        entityBuilder.reportedAdjudication(reportNumber = 2).also {
          it.punishments.add(
            Punishment(
              id = 1,
              type = PunishmentType.PRIVILEGE,
              privilegeType = PrivilegeType.OTHER,
              otherPrivilege = "other",
              schedule = mutableListOf(
                PunishmentSchedule(days = 1, suspendedUntil = LocalDate.now()),
              ),
            ),
          )
        },
      )

      verify(reportedAdjudicationRepository, atLeastOnce()).findByReportNumber(2)
      verify(prisonApiGateway, atLeastOnce()).createSanctions(any(), any())
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.punishments.first()).isNotNull
      assertThat(argumentCaptor.value.punishments.first().id).isNull()
      assertThat(argumentCaptor.value.punishments.first().activatedFrom).isEqualTo(2)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class UpdatePunishments {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.clear()
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.outcomes.add(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
    }

    @CsvSource(
      "ADJOURNED", "REFER_POLICE", "REFER_INAD", "SCHEDULED", "UNSCHEDULED", "AWAITING_REVIEW", "PROSECUTION",
      "NOT_PROCEED", "DISMISSED", "REJECTED", "RETURNED",
    )
    @ParameterizedTest
    fun `validation error - wrong status code - must be CHARGE_PROVED `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = status },
      )
      assertThatThrownBy {
        punishmentsService.update(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(PunishmentRequest(id = 1, type = PunishmentType.PRIVILEGE, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
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
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(PunishmentRequest(id = 1, type = PunishmentType.EARNINGS, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("stoppage percentage missing for type EARNINGS")
    }

    @CsvSource(
      " PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing start date `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(getRequest(id = 1, type = type, endDate = LocalDate.now())),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing start date for schedule")
    }

    @CsvSource(
      " PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - not suspended missing end date `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(
            getRequest(id = 1, type = type, startDate = LocalDate.now()),
          ),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @CsvSource(
      " PRIVILEGE",
      "EARNINGS",
      "CONFINEMENT",
      "REMOVAL_ACTIVITY",
      "EXCLUSION_WORK",
      "EXTRA_WORK",
      "REMOVAL_WING",
    )
    @ParameterizedTest
    fun `validation error - suspended missing all schedule dates `(type: PunishmentType) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(getRequest(id = 1, type = type)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing all schedule data")
    }

    @Test
    fun `throws exception if id for punishment is not located `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
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
          adjudicationNumber = 1,
          listOf(PunishmentRequest(id = 2, type = PunishmentType.REMOVAL_ACTIVITY, days = 1, suspendedUntil = LocalDate.now())),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Punishment 2 is not associated with ReportedAdjudication")
    }

    @Test
    fun `update punishments `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.punishments.addAll(
            mutableListOf(
              Punishment(
                id = 1,
                type = PunishmentType.CONFINEMENT,
                schedule = mutableListOf(
                  PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
                ),
              ),
              Punishment(
                id = 2,
                type = PunishmentType.EXCLUSION_WORK,
                schedule = mutableListOf(
                  PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
                ),
              ),
              Punishment(
                id = 3,
                type = PunishmentType.ADDITIONAL_DAYS,
                schedule = mutableListOf(
                  PunishmentSchedule(id = 1, days = 1),
                ),
              ),
              Punishment(
                id = 4,
                type = PunishmentType.REMOVAL_WING,
                schedule = mutableListOf(
                  PunishmentSchedule(id = 1, days = 1, suspendedUntil = LocalDate.now()),
                ),
              ),
            ),
          )
        },
      )

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = punishmentsService.update(
        adjudicationNumber = 1,
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
            days = 2,
          ),
          PunishmentRequest(
            id = 4,
            type = PunishmentType.REMOVAL_WING,
            days = 1,
            suspendedUntil = LocalDate.now(),
          ),
        ),
      )

      verify(prisonApiGateway, atLeastOnce()).updateSanctions(any(), any())
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(response).isNotNull
      assertThat(argumentCaptor.value.punishments.size).isEqualTo(4)

      val privilege = argumentCaptor.value.punishments.first { it.type == PunishmentType.PRIVILEGE }
      val additionalDays = argumentCaptor.value.punishments.first { it.type == PunishmentType.ADDITIONAL_DAYS }
      val prospectiveDays = argumentCaptor.value.punishments.first { it.type == PunishmentType.PROSPECTIVE_DAYS }
      val removalWing = argumentCaptor.value.punishments.first { it.type == PunishmentType.REMOVAL_WING }
      assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.EXCLUSION_WORK })

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
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
            Punishment(id = 1, type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(entityBuilder.reportedAdjudication())

      assertThatThrownBy {
        punishmentsService.update(
          adjudicationNumber = 1,
          listOf(
            PunishmentRequest(
              id = 1,
              type = PunishmentType.PROSPECTIVE_DAYS,
              days = 1,
              activatedFrom = 2,
            ),
          ),
        )
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("suspended punishment not found")
    }

    @Test
    fun `clone suspended punishment `() {
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(
        entityBuilder.reportedAdjudication(reportNumber = 2).also {
          it.punishments.add(
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
        adjudicationNumber = 1,
        listOf(
          PunishmentRequest(
            id = 1,
            type = PunishmentType.CONFINEMENT,
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
            activatedFrom = 2,
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(reportedAdjudicationRepository, atLeastOnce()).findByReportNumber(2)

      assertThat(argumentCaptor.value.punishments.first()).isNotNull
      assertThat(argumentCaptor.value.punishments.first().id).isNull()
      assertThat(argumentCaptor.value.punishments.first().activatedFrom).isEqualTo(2)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class GetSuspendedPunishments {

    private val reportedAdjudications = listOf(
      entityBuilder.reportedAdjudication(reportNumber = 1).also {
        it.punishments.add(
          Punishment(
            type = PunishmentType.REMOVAL_WING,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(reportNumber = 2).also {
        it.punishments.add(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedBy = 1234,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
      entityBuilder.reportedAdjudication(reportNumber = 3).also {
        it.punishments.add(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedFrom = 1234,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
    )

    @Test
    fun `get suspended punishments `() {
      whenever(reportedAdjudicationRepository.findByPrisonerNumberAndPunishmentsSuspendedUntilAfter(any(), any())).thenReturn(reportedAdjudications)
      val suspended = punishmentsService.getSuspendedPunishments("AE1234")

      assertThat(suspended.size).isEqualTo(1)
      assertThat(suspended.first().reportNumber).isEqualTo(1)
      assertThat(suspended.first().punishment.type).isEqualTo(PunishmentType.REMOVAL_WING)
      assertThat(suspended.first().punishment.schedule.days).isEqualTo(10)
      assertThat(suspended.first().punishment.schedule.suspendedUntil).isEqualTo(LocalDate.now())
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
