package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodeLookupService

/**
 * Corrects self-referential (looped) consecutive punishments, where two charges for the same
 * prisoner are mutually consecutive (punishment on charge A consecutive to charge B and the
 * punishment on charge B consecutive to charge A). Only the more recently created charge should
 * remain consecutive to the older one, so the consecutive link is cleared from the punishment
 * belonging to the earlier-created charge. These loops break downstream services such as
 * Calculate Release Dates.
 */
@Service
class ConsecutivePunishmentCorrectionService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val offenceCodeLookupService: OffenceCodeLookupService,
) {

  @Transactional
  fun repairConsecutivePunishmentChains(): List<ReportedAdjudicationDto> = (clearLoopedConsecutivePunishments() + repairLinksThroughQuashedCharges())
    .associateBy { it.chargeNumber }
    .values
    .toList()

  @Transactional
  fun clearLoopedConsecutivePunishments(): List<ReportedAdjudicationDto> {
    val idsToClear = reportedAdjudicationRepository.findLoopedConsecutivePunishmentIdsToClear()
    if (idsToClear.isEmpty()) return emptyList()

    return reportedAdjudicationRepository.findByPunishmentIdIn(idsToClear).map { report ->
      report.getPunishments().filter { idsToClear.contains(it.id) }.forEach {
        log.info("clearing looped consecutive punishment ${it.id} on charge ${report.chargeNumber}")
        it.consecutiveToChargeNumber = null
      }
      report.toDto(offenceCodeLookupService)
    }
  }

  /**
   * Repairs the legacy shape that caused Adjustments to undercount a consecutive chain:
   * an active charge points to a quashed charge in the middle of the chain. The active punishment
   * is reconnected to the first live ancestor (or made the root when the quashed charge was the
   * root). Ambiguous or inconsistent chains are deliberately left unchanged for manual review.
   */
  @Transactional
  fun repairLinksThroughQuashedCharges(): List<ReportedAdjudicationDto> {
    val repairedReports = linkedMapOf<String, ReportedAdjudication>()

    reportedAdjudicationRepository.findReportsWithActiveConsecutivePunishments(
      PunishmentType.additionalDays().map { it.name },
    )
      .map { it.chargeNumber }
      .distinct()
      .sorted()
      .mapNotNull(reportedAdjudicationRepository::findByChargeNumberForUpdate)
      .filter { it.latestOutcomeCode() == OutcomeCode.CHARGE_PROVED }
      .forEach { source ->
        source.getPunishments()
          .filter {
            PunishmentType.additionalDays().contains(it.type) &&
              it.getSuspendedUntil() == null &&
              it.consecutiveToChargeNumber != null
          }
          .forEach punishments@{ punishment ->
            val currentTarget = requireNotNull(punishment.consecutiveToChargeNumber)
            val targetReport = reportedAdjudicationRepository.findByChargeNumberForUpdate(currentTarget)
            if (targetReport?.latestOutcomeCode() != OutcomeCode.QUASHED) return@punishments

            resolveLiveAncestor(source, punishment, targetReport)?.let { resolvedTarget ->
              if (resolvedTarget.chargeNumber != currentTarget) {
                log.info(
                  "repairing consecutive punishment ${punishment.id} on charge ${source.chargeNumber}: " +
                    "$currentTarget -> ${resolvedTarget.chargeNumber ?: "root"}",
                )
                punishment.consecutiveToChargeNumber = resolvedTarget.chargeNumber
                repairedReports[source.chargeNumber] = source
              }
            } ?: log.warn(
              "unable to safely repair consecutive punishment ${punishment.id} on charge ${source.chargeNumber} " +
                "through quashed target $currentTarget",
            )
          }
      }

    return repairedReports.values.map { it.toDto(offenceCodeLookupService) }
  }

  private fun resolveLiveAncestor(
    source: ReportedAdjudication,
    sourcePunishment: Punishment,
    firstQuashedTarget: ReportedAdjudication,
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

      if (target.latestOutcomeCode() == OutcomeCode.CHARGE_PROVED) {
        return ResolvedTarget(target.chargeNumber)
      }
      if (target.latestOutcomeCode() != OutcomeCode.QUASHED) return null

      val nextChargeNumber = matchingPunishments.single().consecutiveToChargeNumber
        ?: return ResolvedTarget(null)
      target = reportedAdjudicationRepository.findByChargeNumberForUpdate(nextChargeNumber) ?: return null
    }
  }

  private fun ReportedAdjudication.latestOutcomeCode(): OutcomeCode? = getOutcomes()
    .maxWithOrNull(compareBy({ it.getCreatedDateTime() }, { it.id }))
    ?.code

  private data class ResolvedTarget(val chargeNumber: String?)

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
