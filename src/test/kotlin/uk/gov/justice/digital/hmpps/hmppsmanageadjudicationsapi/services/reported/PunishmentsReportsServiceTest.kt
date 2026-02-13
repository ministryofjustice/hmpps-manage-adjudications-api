package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.RehabilitativeActivity
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDate
import java.time.LocalDateTime

class PunishmentsReportsServiceTest : ReportedAdjudicationTestBase() {

  private val punishmentsReportQueryService = PunishmentsReportQueryService(
    reportedAdjudicationRepository,
  )

  private val punishmentsReportService = PunishmentsReportService(
    punishmentsReportQueryService,
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      punishmentsReportService.getSuspendedPunishments(
        prisonerNumber = "",
        chargeNumber = "1",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      punishmentsReportService.getReportsWithAdditionalDays(
        chargeNumber = "1",
        prisonerNumber = "1",
        punishmentType = PunishmentType.PROSPECTIVE_DAYS,

      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
    )

    @Test
    fun `get suspended punishments only returns the active ones from a charge`() {
      whenever(
        reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.status = ReportedAdjudicationStatus.CHARGE_PROVED
            it.addPunishment(
              Punishment(
                type = PunishmentType.REMOVAL_WING,
                suspendedUntil = LocalDate.now(),
                schedule = mutableListOf(
                  PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
                ),
              ),
            )
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now().minusDays(2),
                schedule = mutableListOf(
                  PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now().minusDays(2)),
                ),
              ),
            )
          },
        ),
      )
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())

      val suspended = punishmentsReportService.getSuspendedPunishments("AE1234", chargeNumber = "1")
      assertThat(suspended.size).isEqualTo(1)
    }

    @Test
    fun `get suspended punishments `() {
      whenever(
        reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(reportedAdjudications)
      whenever(
        reportedAdjudicationRepository.findByPrisonerNumberAndStatusInAndPunishmentsSuspendedUntilAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "5").also {
            it.status = ReportedAdjudicationStatus.INVALID_OUTCOME
            it.addPunishment(
              Punishment(
                type = PunishmentType.REMOVAL_WING,
                suspendedUntil = LocalDate.now(),
                schedule = mutableListOf(
                  PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
                ),
              ),
            )
          },
        ),
      )
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(entityBuilder.reportedAdjudication())
      val suspended = punishmentsReportService.getSuspendedPunishments("AE1234", chargeNumber = "1")

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
      whenever(
        reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(reportedAdjudications)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.first().oicHearingType = oicHearingType
        },
      )

      val suspended = punishmentsReportService.getSuspendedPunishments("AE1234", chargeNumber = "1")

      val removalWing = suspended.first { it.punishment.type == PunishmentType.REMOVAL_WING }
      val additionalDays = suspended.first { it.punishment.type == PunishmentType.ADDITIONAL_DAYS }
      val prospectiveDays = suspended.first { it.punishment.type == PunishmentType.PROSPECTIVE_DAYS }

      assertThat(removalWing).isNotNull
      assertThat(additionalDays).isNotNull
      assertThat(prospectiveDays).isNotNull

      assertThat(suspended.size).isEqualTo(3)
    }

    @Test
    fun `get suspended punishments filters out any with rehabilitative activities`() {
      whenever(
        reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.clearPunishments()
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now().plusDays(10),
                schedule = mutableListOf(PunishmentSchedule(duration = 10)),
              ),
            )
            it.addPunishment(
              Punishment(
                type = PunishmentType.REMOVAL_WING,
                suspendedUntil = LocalDate.now().plusDays(10),
                rehabilitativeActivities = mutableListOf(
                  RehabilitativeActivity(),
                ),
                schedule = mutableListOf(PunishmentSchedule(duration = 10)),
              ),
            )
          },
        ),
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(),
      )

      val suspended = punishmentsReportService.getSuspendedPunishments("AE1234", chargeNumber = "1")
      assertThat(suspended.size).isEqualTo(1)
      assertThat(suspended.first().punishment.type).isEqualTo(PunishmentType.CONFINEMENT)
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
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
              PunishmentSchedule(duration = 10, suspendedUntil = LocalDate.now()),
            ),
          ),
        )
      },
    )

    @EnumSource(PunishmentType::class)
    @ParameterizedTest
    fun `throws exception if punishment type in request not additional days or prospective days`(punishmentType: PunishmentType) {
      if (listOf(PunishmentType.ADDITIONAL_DAYS, PunishmentType.PROSPECTIVE_DAYS).contains(punishmentType)) return

      Assertions.assertThatThrownBy {
        punishmentsReportService.getReportsWithAdditionalDays(
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
      whenever(
        reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(
          ReportedAdjudicationStatus.CHARGE_PROVED,
          "AE1234",
          punishmentType,
        ),
      ).thenReturn(
        reportedAdjudications,
      )

      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(
        entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
          it.hearings.first().dateTimeOfHearing = LocalDateTime.now()
        },
      )

      val additionalDaysReports = punishmentsReportService.getReportsWithAdditionalDays(
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
  inner class GetActivePunishments {

    @Test
    fun `get activated from with other privilege`() {
      whenever(
        reportedAdjudicationRepository.findIdsForActivePunishmentsByBookingId(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(1L))

      whenever(
        reportedAdjudicationRepository.findByIdsWithPunishments(any()),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.PRIVILEGE,
                privilegeType = PrivilegeType.OTHER,
                otherPrivilege = "test",
                schedule = mutableListOf(
                  PunishmentSchedule(duration = 5, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(5)),
                ),
              ),
            )
          },
        ),
      )

      val response = punishmentsReportService.getActivePunishments(offenderBookingId = 1)

      assertThat(response.first().amount).isNull()
      assertThat(response.first().stoppagePercentage).isNull()
      assertThat(response.first().punishmentType).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(response.first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(response.first().otherPrivilege).isNotNull
      assertThat(response.first().startDate).isEqualTo(LocalDate.now())
      assertThat(response.first().lastDay).isEqualTo(LocalDate.now().plusDays(5))
      assertThat(response.first().duration).isNotNull
      assertThat(response.first().activatedFrom).isNull()
    }

    @Test
    fun `get with stoppage percentage`() {
      whenever(
        reportedAdjudicationRepository.findIdsForActivePunishmentsByBookingId(
          any(),
          any(),
          any(),
        ),
      ).thenReturn(listOf(1L))

      whenever(
        reportedAdjudicationRepository.findByIdsWithPunishments(any()),
      ).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication().also {
            it.addPunishment(
              Punishment(
                type = PunishmentType.EARNINGS,
                stoppagePercentage = 90,
                schedule = mutableListOf(
                  PunishmentSchedule(duration = 0, startDate = LocalDate.now(), endDate = LocalDate.now()),
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
                  PunishmentSchedule(
                    duration = 0,
                    startDate = LocalDate.now().minusDays(2),
                    endDate = LocalDate.now().minusDays(2),
                  ),
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
                  PunishmentSchedule(
                    duration = 0,
                    startDate = LocalDate.now().minusDays(2),
                    endDate = LocalDate.now().minusDays(2),
                  ),
                ),
              ),
            )
          },
        ),
      )

      val response = punishmentsReportService.getActivePunishments(offenderBookingId = 1)

      assertThat(response.size).isEqualTo(1)
      assertThat(response.first().amount).isNull()
      assertThat(response.first().stoppagePercentage).isNotNull
    }
  }
}
