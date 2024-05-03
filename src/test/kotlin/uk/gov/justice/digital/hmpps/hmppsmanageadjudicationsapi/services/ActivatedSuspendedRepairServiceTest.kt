package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate

class ActivatedSuspendedRepairServiceTest : ReportedAdjudicationTestBase() {
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  private val activatedSuspendedRepairService = ActivatedSuspendedRepairService(reportedAdjudicationRepository)

  @Test
  fun `removes the clone and updates the parent record, capturing events for both charges`() {
    val child = entityBuilder.reportedAdjudication(chargeNumber = "child").also {
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          type = PunishmentType.ADDITIONAL_DAYS,
          activatedFromChargeNumber = "parent",
          schedule = mutableListOf(
            PunishmentSchedule(days = 10, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1)),
          ),
        ),
      )
      it.addPunishment(
        Punishment(
          type = PunishmentType.CONFINEMENT,
          activatedFromChargeNumber = "parent",
          schedule = mutableListOf(
            PunishmentSchedule(days = 10, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1)),
          ),
        ),
      )
    }

    val parent = entityBuilder.reportedAdjudication(chargeNumber = "parent").also {
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          type = PunishmentType.ADDITIONAL_DAYS,
          activatedByChargeNumber = "child",
          suspendedUntil = LocalDate.now(),
          schedule = mutableListOf(
            PunishmentSchedule(id = 1, days = 10, suspendedUntil = LocalDate.now()),
          ),
        ),
      )
      it.addPunishment(
        Punishment(
          type = PunishmentType.CONFINEMENT,
          activatedByChargeNumber = "child",
          suspendedUntil = LocalDate.now(),
          schedule = mutableListOf(
            PunishmentSchedule(id = 1, days = 10, suspendedUntil = LocalDate.now()),
          ),
        ),
      )
    }

    whenever(reportedAdjudicationRepository.findByPunishmentsActivatedFromChargeNumberIsNotNull()).thenReturn(listOf(child))
    whenever(reportedAdjudicationRepository.findByChargeNumber("parent")).thenReturn(parent)

    val response = activatedSuspendedRepairService.repair()

    assertThat(child.getPunishments()).isEmpty()
    assertThat(parent.getPunishments().all { it.suspendedUntil == null }).isTrue()
    assertThat(parent.getPunishments().all { it.activatedByChargeNumber == "child" }).isTrue()
    assertThat(parent.getPunishments().all { it.schedule.size == 2 }).isTrue()
    assertThat(parent.getPunishments().first().schedule.first { it.id == null }.suspendedUntil).isNull()
    assertThat(parent.getPunishments().first().schedule.first { it.id == null }.startDate).isEqualTo(LocalDate.now())
    assertThat(parent.getPunishments().first().schedule.first { it.id == null }.endDate).isEqualTo(LocalDate.now().plusDays(1))
    assertThat(parent.getPunishments().last().schedule.first { it.id == null }.suspendedUntil).isNull()
    assertThat(parent.getPunishments().last().schedule.first { it.id == null }.startDate).isEqualTo(LocalDate.now())
    assertThat(parent.getPunishments().last().schedule.first { it.id == null }.endDate).isEqualTo(LocalDate.now().plusDays(1))

    assertThat(response.size).isEqualTo(2)
    assertThat(response.first { it.chargeNumber == "parent" }).isNotNull
    assertThat(response.first { it.chargeNumber == "child" }).isNotNull
  }
}
