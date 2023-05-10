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
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest.Companion.mapPunishmentToSanction
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.PrisonApiGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
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

    assertThatThrownBy {
      punishmentsService.createPunishmentsFromChargeProvedIfApplicable(
        reportedAdjudication = entityBuilder.reportedAdjudication(),
        caution = true,
        amount = null,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    assertThatThrownBy {
      punishmentsService.amendPunishmentsFromChargeProvedIfApplicable(
        adjudicationNumber = 1,
        caution = true,
        amount = null,
        damagesOwed = null,
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class PunishmentsFromChargeProved {

    @CsvSource("true, 10.0", "false, 10.0", "true,", "false,")
    @ParameterizedTest
    fun `create punishments `(caution: Boolean, amount: Double?) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
      }

      whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(reportedAdjudication)
      whenever(prisonApiGateway.createSanction(any(), any())).thenReturn(1)

      punishmentsService.createPunishmentsFromChargeProvedIfApplicable(
        reportedAdjudication = reportedAdjudication,
        caution = caution,
        amount = amount,
      )

      when (caution) {
        true -> {
          verify(prisonApiGateway, atLeastOnce()).createSanction(
            reportedAdjudication.reportNumber,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.CAUTION,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
            ),
          )
          assertThat(reportedAdjudication.punishments.firstOrNull { it.type == PunishmentType.CAUTION }).isNotNull
          assertThat(reportedAdjudication.punishments.first { it.type == PunishmentType.CAUTION }.sanctionSeq).isEqualTo(1)
        }
        false -> {
          verify(prisonApiGateway, never()).createSanction(
            1,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.CAUTION,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
            ),
          )
          if (amount != null) assertThat(reportedAdjudication.punishments.firstOrNull { it.type == PunishmentType.CAUTION }).isNull()
        }
      }

      when (amount) {
        null -> {
          verify(prisonApiGateway, never()).createSanction(
            1,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.OTHER,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
              compensationAmount = 10.0,
            ),
          )
          if (caution) assertThat(reportedAdjudication.punishments.firstOrNull { it.type == PunishmentType.DAMAGES_OWED }).isNull()
        }
        else -> {
          verify(prisonApiGateway, atLeastOnce()).createSanction(
            reportedAdjudication.reportNumber,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.OTHER,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
              compensationAmount = 10.0,
            ),
          )
          assertThat(reportedAdjudication.punishments.firstOrNull { it.type == PunishmentType.DAMAGES_OWED }).isNotNull
          assertThat(reportedAdjudication.punishments.first { it.type == PunishmentType.DAMAGES_OWED }.sanctionSeq).isEqualTo(1)
        }
      }
    }

    @CsvSource("true,true", "true,false", "false,false", "false,true")
    @ParameterizedTest
    fun `amend punishments - caution `(cautionExists: Boolean, caution: Boolean) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        it.punishments.add(
          Punishment(
            type = PunishmentType.DAMAGES_OWED,
            amount = 10.0,
            schedule = mutableListOf(
              PunishmentSchedule(days = 0),
            ),
          ),
        )
        it.punishments.add(
          Punishment(
            type = PunishmentType.CONFINEMENT,
            schedule = mutableListOf(
              PunishmentSchedule(days = 0),
            ),
          ),
        )
        if (cautionExists) {
          it.punishments.add(
            Punishment(
              type = PunishmentType.CAUTION,
              sanctionSeq = 1,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
        }
      }

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(reportedAdjudication)
      whenever(prisonApiGateway.createSanction(any(), any())).thenReturn(2)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      punishmentsService.amendPunishmentsFromChargeProvedIfApplicable(
        adjudicationNumber = 1,
        caution = caution,
        amount = if (!cautionExists && caution) 10.0 else null,
        damagesOwed = if (!cautionExists && caution) true else null,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      if (cautionExists) {
        if (caution) {
          verify(prisonApiGateway, never()).deleteSanction(any(), any())
          verify(prisonApiGateway, never()).createSanction(any(), any())
          assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.CAUTION }.sanctionSeq).isEqualTo(1)
        } else {
          verify(prisonApiGateway, atLeastOnce()).deleteSanction(reportedAdjudication.reportNumber, 1)
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.CAUTION }).isNull()
        }
      } else {
        if (caution) {
          verify(prisonApiGateway, atLeast(2)).createSanction(any(), any())
          verify(prisonApiGateway, atLeastOnce()).deleteSanctions(reportedAdjudication.reportNumber)
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.CAUTION }).isNotNull
          assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.CAUTION }.sanctionSeq).isEqualTo(2)
          assertThat(argumentCaptor.value.punishments.size).isEqualTo(2)
        } else {
          verify(prisonApiGateway, never()).deleteSanction(any(), any())
          verify(prisonApiGateway, never()).createSanction(any(), any())
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.CAUTION }).isNull()
        }
      }
    }

    @CsvSource("true, 10.0, true", "true,,true", "false,10.0,true", "true,10.0,false", "true,,false")
    @ParameterizedTest
    fun `amend punishments - damages owed `(changeAmount: Boolean, amount: Double?, recordExists: Boolean) {
      val reportedAdjudication = entityBuilder.reportedAdjudication().also {
        it.createDateTime = LocalDateTime.now()
        it.createdByUserId = ""
        if (recordExists) {
          it.punishments.add(
            Punishment(
              type = PunishmentType.DAMAGES_OWED,
              sanctionSeq = 1,
              amount = if (changeAmount) 100.0 else amount,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
        }
      }

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(reportedAdjudication)
      whenever(prisonApiGateway.createSanction(any(), any())).thenReturn(2)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)

      punishmentsService.amendPunishmentsFromChargeProvedIfApplicable(
        adjudicationNumber = 1,
        caution = false,
        amount = amount,
        damagesOwed = true,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      if (recordExists) {
        if (changeAmount && amount != null) {
          verify(prisonApiGateway, atLeastOnce()).deleteSanction(reportedAdjudication.reportNumber, 1L)
          verify(prisonApiGateway, atLeastOnce()).createSanction(
            reportedAdjudication.reportNumber,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.OTHER,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
              compensationAmount = 10.0,
            ),
          )
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.DAMAGES_OWED }).isNotNull
          assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.DAMAGES_OWED }.sanctionSeq).isEqualTo(2)
        } else if (changeAmount) {
          verify(prisonApiGateway, atLeastOnce()).deleteSanction(reportedAdjudication.reportNumber, 1L)
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.DAMAGES_OWED }).isNull()
        } else {
          verify(prisonApiGateway, never()).deleteSanction(reportedAdjudication.reportNumber, 1L)
          verify(prisonApiGateway, never()).createSanction(any(), any())
        }
      } else {
        if (amount != null) {
          verify(prisonApiGateway, atLeastOnce()).createSanction(
            reportedAdjudication.reportNumber,
            OffenderOicSanctionRequest(
              oicSanctionCode = OicSanctionCode.OTHER,
              status = Status.IMMEDIATE,
              sanctionDays = 0,
              effectiveDate = LocalDate.now(),
              compensationAmount = 10.0,
            ),
          )
          assertThat(argumentCaptor.value.punishments.firstOrNull { it.type == PunishmentType.DAMAGES_OWED }).isNotNull
          assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.DAMAGES_OWED }.sanctionSeq).isEqualTo(2)
        } else {
          verify(prisonApiGateway, never()).deleteSanction(reportedAdjudication.reportNumber, 1L)
          verify(prisonApiGateway, never()).createSanction(any(), any())
        }
      }
    }

    @Test
    fun `set to caution, and preserve damages owed`() {
      val reportedAdjudication = entityBuilder.reportedAdjudication()
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = ""
        },
      )
      whenever(reportedAdjudicationRepository.findByReportNumber(1L)).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
            Punishment(
              id = 1,
              type = PunishmentType.DAMAGES_OWED,
              amount = 10.0,
              schedule = mutableListOf(
                PunishmentSchedule(days = 10),
              ),
            ),
          )
        },
      )
      punishmentsService.amendPunishmentsFromChargeProvedIfApplicable(
        adjudicationNumber = 1L,
        caution = true,
        damagesOwed = null,
        amount = null,
      )

      verify(prisonApiGateway, atLeast(2)).createSanction(any(), any())
      verify(prisonApiGateway, atLeastOnce()).deleteSanctions(any())
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.punishments.size).isEqualTo(2)
      assertThat(argumentCaptor.value.punishments.first().type).isEqualTo(PunishmentType.CAUTION)
      assertThat(argumentCaptor.value.punishments.last().type).isEqualTo(PunishmentType.DAMAGES_OWED)
      assertThat(argumentCaptor.value.punishments.last().amount).isEqualTo(10.0)
    }
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
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
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
    fun `validation error - caution is true `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.clearOutcomes()
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.punishments.add(
            Punishment(
              type = PunishmentType.CAUTION,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.create(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("outcome is a caution - no further punishments can be added")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      assertThatThrownBy {
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
          adjudicationNumber = 1,
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
          adjudicationNumber = 1,
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
              privilegeType = PrivilegeType.CANTEEN,
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
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
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
        punishmentsService.update(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - caution is true `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.clearOutcomes()
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.punishments.add(
            Punishment(
              type = PunishmentType.CAUTION,
              schedule = mutableListOf(
                PunishmentSchedule(days = 0),
              ),
            ),
          )
        },
      )
      assertThatThrownBy {
        punishmentsService.update(
          adjudicationNumber = 1,
          listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)),
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("outcome is a caution - no further punishments can be added")
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

    @CsvSource("true", "false")
    @ParameterizedTest
    fun `update punishments `(maintainDamagesOwed: Boolean) {
      whenever(prisonApiGateway.createSanction(any(), any())).thenReturn(22)
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          if (maintainDamagesOwed) {
            it.punishments.add(
              Punishment(
                type = PunishmentType.DAMAGES_OWED,
                amount = 100.0,
                sanctionSeq = 21,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 0),
                ),
              ),
            )
          }

          it.punishments.addAll(
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
      assertThat(argumentCaptor.value.punishments.size).isEqualTo(if (maintainDamagesOwed) 5 else 4)

      val privilege = argumentCaptor.value.punishments.first { it.type == PunishmentType.PRIVILEGE }
      val additionalDays = argumentCaptor.value.punishments.first { it.type == PunishmentType.ADDITIONAL_DAYS }
      val prospectiveDays = argumentCaptor.value.punishments.first { it.type == PunishmentType.PROSPECTIVE_DAYS }
      val removalWing = argumentCaptor.value.punishments.first { it.type == PunishmentType.REMOVAL_WING }
      if (maintainDamagesOwed) assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.DAMAGES_OWED }).isNotNull
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

      when (maintainDamagesOwed) {
        true -> {
          verify(prisonApiGateway, atLeastOnce()).createSanction(any(), any())
          assertThat(argumentCaptor.value.punishments.first { it.type == PunishmentType.DAMAGES_OWED }.sanctionSeq).isEqualTo(22)
        }
        false -> verify(prisonApiGateway, never()).createSanction(any(), any())
      }
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
              id = 10,
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
    fun `activated from punishment has already been activated `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication.also {
          it.punishments.add(
            Punishment(id = 1, activatedFrom = 2, type = PunishmentType.PROSPECTIVE_DAYS, schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.punishments.add(
            Punishment(id = 3, type = PunishmentType.PROSPECTIVE_DAYS, activatedBy = 1, schedule = mutableListOf(PunishmentSchedule(days = 1))),
          )
        },
      )

      assertDoesNotThrow {
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
      }
    }

    @Test
    fun `clone suspended punishment `() {
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

  @Nested
  inner class PunishmentSanctionMapper {

    @EnumSource(PunishmentType::class)
    @ParameterizedTest
    fun ` map all non privilege punishments to sanctions - suspended`(punishmentType: PunishmentType) {
      if (punishmentType == PunishmentType.PRIVILEGE) return

      val punishment = Punishment(
        type = punishmentType,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.status).isEqualTo(Status.SUSPENDED)
    }

    @EnumSource(PunishmentType::class)
    @ParameterizedTest
    fun ` map all non privilege punishments to sanctions - not suspended`(punishmentType: PunishmentType) {
      if (punishmentType == PunishmentType.PRIVILEGE) return

      val punishment = Punishment(
        type = punishmentType,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, startDate = LocalDate.now(), endDate = LocalDate.now()),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.effectiveDate).isEqualTo(LocalDate.now())
      assertThat(oicSanctionRequest.sanctionDays).isEqualTo(10)
      assertThat(oicSanctionRequest.status).isEqualTo(Status.IMMEDIATE)

      when (punishmentType) {
        PunishmentType.EARNINGS -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.STOP_PCT)
        PunishmentType.CONFINEMENT -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.CC)
        PunishmentType.REMOVAL_ACTIVITY -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.REMACT)
        PunishmentType.EXCLUSION_WORK -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.EXTRA_WORK)
        PunishmentType.EXTRA_WORK -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.EXTW)
        PunishmentType.REMOVAL_WING -> assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.REMWIN)
        else -> {}
      }
    }

    @EnumSource(PrivilegeType::class)
    @ParameterizedTest
    fun `map all privilege punishments to sanctions - suspended`(privilegeType: PrivilegeType) {
      val punishment = Punishment(
        type = PunishmentType.PRIVILEGE,
        privilegeType = privilegeType,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.status).isEqualTo(Status.SUSPENDED)
    }

    @EnumSource(PrivilegeType::class)
    @ParameterizedTest
    fun `map all privilege punishments to sanctions - not suspended`(privilegeType: PrivilegeType) {
      val punishment = Punishment(
        type = PunishmentType.PRIVILEGE,
        privilegeType = privilegeType,
        otherPrivilege = "nintendo switch",
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, startDate = LocalDate.now(), endDate = LocalDate.now()),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.effectiveDate).isEqualTo(LocalDate.now())
      assertThat(oicSanctionRequest.sanctionDays).isEqualTo(10)
      assertThat(oicSanctionRequest.status).isEqualTo(Status.IMMEDIATE)
      assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.FORFEIT)
      if (privilegeType != PrivilegeType.OTHER) assertThat(oicSanctionRequest.commentText).isEqualTo("Loss of $privilegeType")
      if (privilegeType == PrivilegeType.OTHER) assertThat(oicSanctionRequest.commentText).isEqualTo("Loss of nintendo switch")
    }

    @CsvSource("PROSPECTIVE_DAYS", "ADDITIONAL_DAYS")
    @ParameterizedTest
    fun `prospective and additional days - suspended `(punishmentType: PunishmentType) {
      val punishment = Punishment(
        type = punishmentType,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now()),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.effectiveDate).isEqualTo(LocalDate.now())
      assertThat(oicSanctionRequest.sanctionDays).isEqualTo(10)
      assertThat(oicSanctionRequest.status).isEqualTo(Status.SUSPENDED)
      assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.ADA)
    }

    @CsvSource("PROSPECTIVE_DAYS", "ADDITIONAL_DAYS")
    @ParameterizedTest
    fun `prospective and additional days - not suspended `(punishmentType: PunishmentType) {
      val punishment = Punishment(
        type = punishmentType,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10),
        ),
      )

      val oicSanctionRequest = punishment.mapPunishmentToSanction()

      assertThat(oicSanctionRequest.effectiveDate).isEqualTo(LocalDate.now())
      assertThat(oicSanctionRequest.sanctionDays).isEqualTo(10)
      if (punishmentType == PunishmentType.PROSPECTIVE_DAYS) {
        assertThat(oicSanctionRequest.status).isEqualTo(Status.PROSPECTIVE)
      } else {
        assertThat(oicSanctionRequest.status).isEqualTo(Status.IMMEDIATE)
      }
      assertThat(oicSanctionRequest.oicSanctionCode).isEqualTo(OicSanctionCode.ADA)
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
