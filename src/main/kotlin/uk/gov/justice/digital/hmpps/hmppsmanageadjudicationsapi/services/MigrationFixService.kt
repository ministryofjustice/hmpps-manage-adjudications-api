package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class MigrationFixService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair() {
     repairReferPoliceAdjournShouldBeChargeProved()
  }

  private fun repairReferPoliceAdjournShouldBeChargeProved() {
    reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(status = ReportedAdjudicationStatus.ADJOURNED)
      .filter { it.hearings.any { hearing ->  hearing.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE } }
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record -> reportedAdjudicationRepository.save(record)
    }

  }
}