package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.OutcomeService.Companion.latestOutcome

@Transactional
@Service
class MigrationFixService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair() {
    listOf("4039003", "4039024").forEach {
      reportedAdjudicationRepository.findByChargeNumber(it)?.let { r ->
        missingNextStep(r, HearingOutcomeCode.REFER_POLICE)
      }
    }

    reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(ReportedAdjudicationStatus.REFER_POLICE).filter {
      it.hearings.size > 1 && it.getOutcomes().none { o -> o.code == OutcomeCode.SCHEDULE_HEARING }
    }.forEach {
      missingNextStep(it, HearingOutcomeCode.REFER_POLICE)
    }
  }

  private fun missingNextStep(reportedAdjudication: ReportedAdjudication, hearingOutcomeCode: HearingOutcomeCode) {
    val referPoliceHearingIdx = reportedAdjudication.hearings.sortedBy { it.dateTimeOfHearing }.indexOfFirst {
      it.hearingOutcome?.code == hearingOutcomeCode
    }
    if (referPoliceHearingIdx == -1) return

    if (referPoliceHearingIdx < reportedAdjudication.hearings.size - 1) {
      val referPoliceOutcome = reportedAdjudication.getOutcomes().sortedBy { it.getCreatedDateTime() }.first {
        it.code == hearingOutcomeCode.outcomeCode!!
      }
      reportedAdjudication.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = referPoliceOutcome.getCreatedDateTime()!!.plusMinutes(1)),
      )
      // fix the status.
      val latestHearingOutcome = reportedAdjudication.getLatestHearing()!!.hearingOutcome

      if (latestHearingOutcome == null) {
        reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED
      } else {
        if (latestHearingOutcome.code == HearingOutcomeCode.ADJOURN) {
          reportedAdjudication.status = ReportedAdjudicationStatus.ADJOURNED
        } else {
          reportedAdjudication.status = reportedAdjudication.latestOutcome()!!.code.status
        }
      }

      log.info("adding missing next step for ${reportedAdjudication.chargeNumber}, status is now ${reportedAdjudication.status}")

      reportedAdjudicationRepository.save(reportedAdjudication)
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
