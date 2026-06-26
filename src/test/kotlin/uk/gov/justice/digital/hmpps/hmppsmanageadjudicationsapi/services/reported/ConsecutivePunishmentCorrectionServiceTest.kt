package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder

class ConsecutivePunishmentCorrectionServiceTest {

  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()
  private val entityBuilder = EntityBuilder()

  private val service = ConsecutivePunishmentCorrectionService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
  )

  @Test
  fun `does nothing when there are no looped consecutive punishments`() {
    whenever(reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClear()).thenReturn(emptyList())

    val result = service.clearLoopedConsecutivePunishments()

    assertThat(result).isEmpty()
    verify(reportedAdjudicationRepository, never()).findByPunishmentIdIn(any())
  }

  @Test
  fun `only clears the punishment whose id is flagged, leaving others untouched`() {
    val report = entityBuilder.reportedAdjudication(chargeNumber = "A-1").also {
      it.addPunishment(
        Punishment(
          id = 1,
          type = PunishmentType.ADDITIONAL_DAYS,
          consecutiveToChargeNumber = "A-2",
          schedule = mutableListOf(PunishmentSchedule(duration = 1)),
        ),
      )
      it.addPunishment(
        Punishment(
          id = 2,
          type = PunishmentType.ADDITIONAL_DAYS,
          consecutiveToChargeNumber = "A-3",
          schedule = mutableListOf(PunishmentSchedule(duration = 1)),
        ),
      )
    }

    whenever(reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClear()).thenReturn(listOf(1))
    whenever(reportedAdjudicationRepository.findByPunishmentIdIn(listOf(1))).thenReturn(listOf(report))

    val result = service.clearLoopedConsecutivePunishments()

    assertThat(result).hasSize(1)
    assertThat(report.getPunishments().first { it.id == 1L }.consecutiveToChargeNumber).isNull()
    assertThat(report.getPunishments().first { it.id == 2L }.consecutiveToChargeNumber).isEqualTo("A-3")
  }
}
