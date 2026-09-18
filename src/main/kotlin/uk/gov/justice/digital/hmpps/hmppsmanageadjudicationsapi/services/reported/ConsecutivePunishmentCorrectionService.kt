package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

@Service
class ConsecutivePunishmentCorrectionService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val offenceCodeLookupService: OffenceCodeLookupService,
  private val entityManager: EntityManager,
  transactionManager: PlatformTransactionManager,
) {
  private val transactionTemplate = TransactionTemplate(transactionManager).apply {
    propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
  }

  fun repairConsecutivePunishmentChains(): List<ReportedAdjudicationDto> = processPrisoners(
    prisonerNumbers = (
      reportedAdjudicationRepository.findPrisonerNumbersWithLoopedConsecutivePunishments() +
        findPrisonerNumbersWithRepairCandidates()
      ).distinct().sorted(),
    clearLoops = true,
    repairLinks = true,
  )

  fun clearLoopedConsecutivePunishments(): List<ReportedAdjudicationDto> = processPrisoners(
    prisonerNumbers = reportedAdjudicationRepository.findPrisonerNumbersWithLoopedConsecutivePunishments(),
    clearLoops = true,
    repairLinks = false,
  )

  /**
   * Repairs the legacy shape that caused Adjustments to undercount a consecutive chain:
   * an active charge points to a quashed charge in the middle of the chain. The active punishment
   * is reconnected to the first live ancestor (or made the root when the quashed charge was the
   * root). Ambiguous or inconsistent chains are deliberately left unchanged for manual review.
   */
  fun repairLinksThroughQuashedCharges(): List<ReportedAdjudicationDto> = processPrisoners(
    prisonerNumbers = findPrisonerNumbersWithRepairCandidates(),
    clearLoops = false,
    repairLinks = true,
  )

  /**
   * Each prisoner's correction runs in its own short transaction. This prevents the estate-wide
   * discovery query from turning into an estate-wide lock while retaining the same per-prisoner
   * mutex used by interactive consecutive-punishment writes.
   */
  private fun processPrisoners(
    prisonerNumbers: List<String>,
    clearLoops: Boolean,
    repairLinks: Boolean,
  ): List<ReportedAdjudicationDto> = prisonerNumbers.distinct().sorted().flatMap { prisonerNumber ->
    transactionTemplate.execute {
      reportedAdjudicationRepository.lockConsecutivePunishmentOperationsForPrisoner(prisonerNumber)
      val reports = reportedAdjudicationRepository.findByPrisonerNumber(prisonerNumber)

      val clearedLoops = if (clearLoops) {
        clearLoopedConsecutivePunishments(
          reports = reports,
          idsToClear = reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClearForPrisoner(
            prisonerNumber,
          ),
        )
      } else {
        emptyList()
      }

      if (clearedLoops.isNotEmpty()) entityManager.flush()

      val repairedLinks = if (repairLinks) repairLinksThroughQuashedCharges(reports) else emptyList()

      (clearedLoops + repairedLinks)
        .associateBy { it.chargeNumber }
        .values
        .toList()
    }.orEmpty()
  }

  private fun clearLoopedConsecutivePunishments(
    reports: List<ReportedAdjudication>,
    idsToClear: List<Long>,
  ): List<ReportedAdjudicationDto> {
    if (idsToClear.isEmpty()) return emptyList()

    val ids = idsToClear.toSet()
    return reports.mapNotNull { report ->
      val punishmentsToClear = report.getPunishments().filter { it.id in ids }
      if (punishmentsToClear.isEmpty()) return@mapNotNull null

      punishmentsToClear.forEach {
        log.info("clearing looped consecutive punishment ${it.id} on charge ${report.chargeNumber}")
        it.consecutiveToChargeNumber = null
      }
      report.toDto(offenceCodeLookupService)
    }
  }

  private fun repairLinksThroughQuashedCharges(
    reports: List<ReportedAdjudication>,
  ): List<ReportedAdjudicationDto> {
    val reportsByChargeNumber = reports.associateBy { it.chargeNumber }
    val repairedReports = linkedMapOf<String, ReportedAdjudication>()

    val repairPlans = reports
      .filter { it.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED }
      .flatMap { source ->
        source.getPunishments()
          .filter {
            PunishmentType.additionalDays().contains(it.type) &&
              it.getSuspendedUntil() == null &&
              it.consecutiveToChargeNumber != null
          }
          .mapNotNull punishments@{ punishment ->
            val currentTarget = requireNotNull(punishment.consecutiveToChargeNumber)
            val targetReport = reportsByChargeNumber[currentTarget]
            if (targetReport?.latestOutcome()?.code != OutcomeCode.QUASHED) return@punishments null

            val resolvedTarget = resolveLiveAncestor(source, punishment, targetReport, reportsByChargeNumber)
            if (resolvedTarget == null) {
              log.warn(
                "unable to safely repair consecutive punishment ${punishment.id} on charge ${source.chargeNumber} " +
                  "through quashed target $currentTarget",
              )
              return@punishments null
            }

            RepairPlan(source, punishment, currentTarget, resolvedTarget.chargeNumber)
          }
      }

    val plannedDependentsByTarget = repairPlans
      .mapNotNull { plan ->
        plan.resolvedTarget?.let { ConsecutiveTarget(it, plan.punishment.type) to plan.source.chargeNumber }
      }
      .groupBy({ it.first }, { it.second })

    repairPlans.forEach { plan ->
      val targetWouldHaveMultipleDependents = plan.resolvedTarget?.let { targetChargeNumber ->
        plannedDependentsByTarget.getValue(ConsecutiveTarget(targetChargeNumber, plan.punishment.type)).size > 1 ||
          hasOtherLiveDependent(
            reports = reports,
            targetChargeNumber = targetChargeNumber,
            punishmentType = plan.punishment.type,
            sourceChargeNumber = plan.source.chargeNumber,
          )
      } == true
      if (targetWouldHaveMultipleDependents) {
        log.warn(
          "unable to safely repair consecutive punishment ${plan.punishment.id} on charge " +
            "${plan.source.chargeNumber}: target ${plan.resolvedTarget} would have multiple live dependents",
        )
        return@forEach
      }

      if (plan.resolvedTarget != plan.currentTarget) {
        log.info(
          "repairing consecutive punishment ${plan.punishment.id} on charge ${plan.source.chargeNumber}: " +
            "${plan.currentTarget} -> ${plan.resolvedTarget ?: "root"}",
        )
        plan.punishment.consecutiveToChargeNumber = plan.resolvedTarget
        repairedReports[plan.source.chargeNumber] = plan.source
      }
    }

    return repairedReports.values.map { it.toDto(offenceCodeLookupService) }
  }

  private fun resolveLiveAncestor(
    source: ReportedAdjudication,
    sourcePunishment: Punishment,
    firstQuashedTarget: ReportedAdjudication,
    reportsByChargeNumber: Map<String, ReportedAdjudication>,
  ): ResolvedTarget? {
    val sourceHearingDate = source.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() ?: return null
    val visited = mutableSetOf(source.chargeNumber)
    var target = firstQuashedTarget

    while (true) {
      if (!visited.add(target.chargeNumber) ||
        target.prisonerNumber != source.prisonerNumber ||
        target.getLatestHearing()?.dateTimeOfHearing?.toLocalDate() != sourceHearingDate
      ) {
        return null
      }

      val matchingPunishments = target.getPunishments().filter {
        it.type == sourcePunishment.type && it.getSuspendedUntil() == null
      }
      if (matchingPunishments.size != 1) return null

      if (target.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED) {
        return ResolvedTarget(target.chargeNumber)
      }
      if (target.latestOutcome()?.code != OutcomeCode.QUASHED) return null

      val nextChargeNumber = matchingPunishments.single().consecutiveToChargeNumber
        ?: return ResolvedTarget(null)
      target = reportsByChargeNumber[nextChargeNumber] ?: return null
    }
  }

  private fun hasOtherLiveDependent(
    reports: List<ReportedAdjudication>,
    targetChargeNumber: String,
    punishmentType: PunishmentType,
    sourceChargeNumber: String,
  ): Boolean = reports.any { report ->
    report.chargeNumber != sourceChargeNumber &&
      report.latestOutcome()?.code == OutcomeCode.CHARGE_PROVED &&
      report.getPunishments().any {
        it.type == punishmentType &&
          it.getSuspendedUntil() == null &&
          it.consecutiveToChargeNumber == targetChargeNumber
      }
  }

  private fun findPrisonerNumbersWithRepairCandidates(): List<String> = reportedAdjudicationRepository.findPrisonerNumbersWithActiveConsecutivePunishments(
    PunishmentType.additionalDays().map { it.name },
  )

  private data class ResolvedTarget(val chargeNumber: String?)

  private data class ConsecutiveTarget(
    val chargeNumber: String,
    val punishmentType: PunishmentType,
  )

  private data class RepairPlan(
    val source: ReportedAdjudication,
    val punishment: Punishment,
    val currentTarget: String,
    val resolvedTarget: String?,
  )

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
