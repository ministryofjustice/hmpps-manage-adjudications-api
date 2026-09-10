package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDateTime

class ConsecutivePunishmentCorrectionServiceTest {

  private val reportedAdjudicationRepository: ReportedAdjudicationRepository = mock()
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()
  private val entityManager: EntityManager = mock()
  private val transactionManager: PlatformTransactionManager = mock()
  private val transactionStatus: TransactionStatus = mock()
  private val entityBuilder = EntityBuilder()

  private val service = ConsecutivePunishmentCorrectionService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    entityManager,
    transactionManager,
  )

  init {
    whenever(transactionManager.getTransaction(any<TransactionDefinition>())).thenReturn(transactionStatus)
  }

  @Test
  fun `does nothing when there are no looped consecutive punishments`() {
    whenever(reportedAdjudicationRepository.findPrisonerNumbersWithLoopedConsecutivePunishments()).thenReturn(emptyList())

    val result = service.clearLoopedConsecutivePunishments()

    assertThat(result).isEmpty()
    verify(reportedAdjudicationRepository, never()).findByPrisonerNumber(any())
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

    whenever(reportedAdjudicationRepository.findPrisonerNumbersWithLoopedConsecutivePunishments()).thenReturn(
      listOf(report.prisonerNumber),
    )
    whenever(
      reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClearForPrisoner(report.prisonerNumber),
    ).thenReturn(listOf(1))
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(report.prisonerNumber)).thenReturn(listOf(report))

    val result = service.clearLoopedConsecutivePunishments()

    assertThat(result).hasSize(1)
    assertThat(report.getPunishments().first { it.id == 1L }.consecutiveToChargeNumber).isNull()
    assertThat(report.getPunishments().first { it.id == 2L }.consecutiveToChargeNumber).isEqualTo("A-3")
  }

  @Test
  fun `reconnects a live punishment to the first live ancestor through quashed charges`() {
    val root = additionalDaysReport("A-1", OutcomeCode.CHARGE_PROVED)
    val quashed = additionalDaysReport("A-2", OutcomeCode.QUASHED, consecutiveTo = root.chargeNumber)
    val source = additionalDaysReport("A-3", OutcomeCode.CHARGE_PROVED, consecutiveTo = quashed.chargeNumber)

    whenever(
      reportedAdjudicationRepository.findPrisonerNumbersWithActiveConsecutivePunishments(
        listOf(PunishmentType.ADDITIONAL_DAYS.name, PunishmentType.PROSPECTIVE_DAYS.name),
      ),
    ).thenReturn(listOf(source.prisonerNumber))
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(source.prisonerNumber)).thenReturn(
      listOf(root, quashed, source),
    )

    val result = service.repairLinksThroughQuashedCharges()

    assertThat(result.map { it.chargeNumber }).containsExactly(source.chargeNumber)
    assertThat(source.getPunishments().single().consecutiveToChargeNumber).isEqualTo(root.chargeNumber)
    inOrder(reportedAdjudicationRepository) {
      verify(reportedAdjudicationRepository).lockConsecutivePunishmentOperationsForPrisoner(source.prisonerNumber)
      verify(reportedAdjudicationRepository).findByPrisonerNumber(source.prisonerNumber)
    }

    val secondResult = service.repairLinksThroughQuashedCharges()
    assertThat(secondResult).isEmpty()
  }

  @Test
  fun `flushes cleared loops before resolving the next repair phase`() {
    val loopedReport = additionalDaysReport("A-1", OutcomeCode.CHARGE_PROVED, consecutiveTo = "A-2", punishmentId = 1)
    val repairCandidate = additionalDaysReport("A-3", OutcomeCode.CHARGE_PROVED)
    val prisonerNumber = loopedReport.prisonerNumber

    whenever(reportedAdjudicationRepository.findPrisonerNumbersWithLoopedConsecutivePunishments()).thenReturn(
      listOf(prisonerNumber),
    )
    whenever(
      reportedAdjudicationRepository.findPrisonerNumbersWithActiveConsecutivePunishments(
        PunishmentType.additionalDays().map { it.name },
      ),
    ).thenReturn(listOf(prisonerNumber))
    whenever(
      reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClearForPrisoner(prisonerNumber),
    ).thenReturn(listOf(1))
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber)).thenReturn(
      listOf(loopedReport, repairCandidate),
    )

    service.repairConsecutivePunishmentChains()

    inOrder(reportedAdjudicationRepository, entityManager) {
      verify(reportedAdjudicationRepository).findByPrisonerNumber(prisonerNumber)
      verify(reportedAdjudicationRepository).findLoopedConsecutivePunishmentIdsToClearForPrisoner(prisonerNumber)
      verify(entityManager).flush()
    }
  }

  @Test
  fun `does not repair a link when the resolved target already has another live dependent`() {
    val root = additionalDaysReport("A-1", OutcomeCode.CHARGE_PROVED)
    val existingDependent = additionalDaysReport("A-2", OutcomeCode.CHARGE_PROVED, consecutiveTo = root.chargeNumber)
    val quashed = additionalDaysReport("A-3", OutcomeCode.QUASHED, consecutiveTo = root.chargeNumber)
    val source = additionalDaysReport("A-4", OutcomeCode.CHARGE_PROVED, consecutiveTo = quashed.chargeNumber)

    whenever(
      reportedAdjudicationRepository.findPrisonerNumbersWithActiveConsecutivePunishments(
        PunishmentType.additionalDays().map { it.name },
      ),
    ).thenReturn(listOf(source.prisonerNumber))
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(source.prisonerNumber)).thenReturn(
      listOf(root, existingDependent, quashed, source),
    )

    val result = service.repairLinksThroughQuashedCharges()

    assertThat(result).isEmpty()
    assertThat(source.getPunishments().single().consecutiveToChargeNumber).isEqualTo(quashed.chargeNumber)
  }

  @Test
  fun `commits each prisoner repair before locking the next prisoner`() {
    whenever(
      reportedAdjudicationRepository.findPrisonerNumbersWithActiveConsecutivePunishments(
        PunishmentType.additionalDays().map { it.name },
      ),
    ).thenReturn(listOf("B1234CD", "A1234BC"))
    whenever(reportedAdjudicationRepository.findByPrisonerNumber(any())).thenReturn(emptyList())

    service.repairLinksThroughQuashedCharges()

    inOrder(transactionManager, reportedAdjudicationRepository) {
      verify(transactionManager).getTransaction(any<TransactionDefinition>())
      verify(reportedAdjudicationRepository).lockConsecutivePunishmentOperationsForPrisoner("A1234BC")
      verify(reportedAdjudicationRepository).findByPrisonerNumber("A1234BC")
      verify(transactionManager).commit(transactionStatus)
      verify(transactionManager).getTransaction(any<TransactionDefinition>())
      verify(reportedAdjudicationRepository).lockConsecutivePunishmentOperationsForPrisoner("B1234CD")
      verify(reportedAdjudicationRepository).findByPrisonerNumber("B1234CD")
      verify(transactionManager).commit(transactionStatus)
    }
  }

  private fun additionalDaysReport(
    chargeNumber: String,
    outcomeCode: OutcomeCode,
    consecutiveTo: String? = null,
    punishmentId: Long? = null,
  ): ReportedAdjudication = entityBuilder.reportedAdjudication(
    chargeNumber = chargeNumber,
    dateTime = LocalDateTime.of(2023, 1, 1, 10, 0),
  ).also { report ->
    report.status = outcomeCode.status
    report.addOutcome(
      Outcome(code = outcomeCode).also { it.createDateTime = LocalDateTime.of(2023, 1, 2, 10, 0) },
    )
    report.addPunishment(
      Punishment(
        id = punishmentId,
        type = PunishmentType.ADDITIONAL_DAYS,
        consecutiveToChargeNumber = consecutiveTo,
        schedule = mutableListOf(PunishmentSchedule(duration = 5)),
      ),
    )
  }
}
