package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome

@Transactional
@Service
class MigrationFixService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair() {
    reportedAdjudicationRepository.fixMigrationRecords().forEach {
        record ->
      if (record.getOutcomes().isNotEmpty() && record.getOutcomes().map { it.code }.any { o ->
          listOf(OutcomeCode.CHARGE_PROVED, OutcomeCode.NOT_PROCEED, OutcomeCode.DISMISSED).contains(o)
        }
      ) {
        log.info("updating status for ${record.chargeNumber}")
        record.status = record.latestOutcome()!!.code.status
      } else {
        log.info("setting invalid ${record.chargeNumber}")
        record.status = ReportedAdjudicationStatus.INVALID_OUTCOME
      }
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
