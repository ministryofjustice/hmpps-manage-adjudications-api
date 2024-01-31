package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
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
      .filter { it.hearings.any { hearing -> hearing.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE } }
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record ->
        val policeReferIdx = record.hearings.indexOfLast { it.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE }
        val nextHearingAfter = record.hearings.getOrNull(policeReferIdx + 1)

        nextHearingAfter?.let {
          it.hearingOutcome?.let { hearingOutcome ->
            if (hearingOutcome.details == Finding.PROVED.name && hearingOutcome.code == HearingOutcomeCode.ADJOURN && record.hearings.getOrNull(policeReferIdx + 2) == null) {
              log.info("Repairing ${record.chargeNumber}")
              val policeReferOutcome = record.getOutcomes().sortedBy { outcome -> outcome.getCreatedDateTime() }.last { outcome -> outcome.code == OutcomeCode.REFER_POLICE }
              // first of all.  need to add in the referal outcome
              record.addOutcome(
                Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = policeReferOutcome.getCreatedDateTime()!!.plusMinutes(1)),
              )
              // second, amend adjourn to complete
              hearingOutcome.code = HearingOutcomeCode.COMPLETE
              // add a charge proved outcome.
              record.addOutcome(
                Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = policeReferOutcome.getCreatedDateTime()!!.plusMinutes(2)),
              )
              // set it to charge proved
              record.status = ReportedAdjudicationStatus.CHARGE_PROVED

              reportedAdjudicationRepository.save(record)
            }
          }
        }
      }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
