package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import java.time.LocalDateTime

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
        val policeReferIdx = record.hearings.sortedBy { it.dateTimeOfHearing }.indexOfLast { it.hearingOutcome?.code == HearingOutcomeCode.REFER_POLICE }
        val nextHearingAfter = record.hearings.sortedBy { it.dateTimeOfHearing }.getOrNull(policeReferIdx + 1)

        nextHearingAfter?.let {
          it.hearingOutcome?.let { hearingOutcome ->
            if (listOf(Finding.PROVED.name, Finding.NOT_PROCEED.name, Finding.D.name).contains(hearingOutcome.details) && hearingOutcome.code == HearingOutcomeCode.ADJOURN && record.hearings.sortedBy { h -> h.dateTimeOfHearing }.getOrNull(policeReferIdx + 2) == null) {

              log.info("Repairing ${record.chargeNumber}")
              var policeReferOutcome = record.getOutcomes().sortedBy { outcome -> outcome.getCreatedDateTime() }.lastOrNull { outcome -> outcome.code == OutcomeCode.REFER_POLICE }

              if (policeReferOutcome == null) {
                policeReferOutcome = Outcome(code = OutcomeCode.REFER_POLICE, actualCreatedDate = LocalDateTime.now())
                record.addOutcome(policeReferOutcome)
              }

              // first of all.  need to add in the referal outcome
              record.addOutcome(
                Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = policeReferOutcome.getCreatedDateTime()!!.plusMinutes(2)),
              )
              // second, amend adjourn to complete
              hearingOutcome.code = HearingOutcomeCode.COMPLETE
              // add a charge proved outcome.
              record.addOutcome(
                Outcome(
                  code = if (hearingOutcome.details == Finding.PROVED.name) OutcomeCode.CHARGE_PROVED else if (hearingOutcome.details == Finding.D.name) OutcomeCode.DISMISSED else OutcomeCode.NOT_PROCEED,
                  actualCreatedDate = policeReferOutcome.getCreatedDateTime()!!.plusMinutes(3),
                  reason = if (hearingOutcome.details == Finding.NOT_PROCEED.name) NotProceedReason.OTHER else null,
                ),
              )
              // set it to charge proved
              record.status = if (hearingOutcome.details == Finding.PROVED.name) {
                ReportedAdjudicationStatus.CHARGE_PROVED
              } else if (hearingOutcome.details == Finding.D.name) {
                ReportedAdjudicationStatus.DISMISSED
              } else {
                ReportedAdjudicationStatus.NOT_PROCEED
              }

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
