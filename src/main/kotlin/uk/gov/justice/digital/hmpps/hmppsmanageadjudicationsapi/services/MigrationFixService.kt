package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class MigrationFixService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair() {
    fixMissingAdjourns()
  }

  private fun fixMissingAdjourns() {
    reportedAdjudicationRepository.getReportsMissingAdjourn().filter { it.hearings.size > 1 }.forEach {
        record ->
      val firstHearing = record.hearings.minByOrNull { it.dateTimeOfHearing }!!
      if (firstHearing.hearingOutcome == null) {
        log.info("fixing missing adjourns for ${record.chargeNumber}")
        firstHearing.hearingOutcome = HearingOutcome(
          code = HearingOutcomeCode.ADJOURN,
          adjudicator = "",
          reason = HearingOutcomeAdjournReason.OTHER,
          details = "",
        )
        reportedAdjudicationRepository.save(record)
      }
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
