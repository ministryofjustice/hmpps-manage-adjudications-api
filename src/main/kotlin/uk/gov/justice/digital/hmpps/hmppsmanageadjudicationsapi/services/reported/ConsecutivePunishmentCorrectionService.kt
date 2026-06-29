package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
