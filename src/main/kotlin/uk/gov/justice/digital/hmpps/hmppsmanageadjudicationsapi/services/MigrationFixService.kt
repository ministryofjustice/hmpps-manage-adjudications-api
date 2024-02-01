package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class MigrationFixService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun repair() {
    fixRanbyOutstanding()
    fixRefChargeProvedMaybe()
    fixRefNotProvedMaybe()
    fixRefDismissedMaybe()
    fixRefAdjourndMaybe()
    fixStocken()
    // fixMissingAdjourns()
  }

  private fun fixMissingAdjourns() {
    listOf(
      // ReportedAdjudicationStatus.CHARGE_PROVED,
      ReportedAdjudicationStatus.NOT_PROCEED,
      ReportedAdjudicationStatus.DISMISSED,
      ReportedAdjudicationStatus.REFER_POLICE,
      ReportedAdjudicationStatus.REFER_INAD,
    ).forEach { status ->
      reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(status).filter {
        it.hearings.size > 1 && it.hearings.any { ho -> ho.hearingOutcome == null }
      }.forEach {
          record ->
        val firstHearing = record.hearings.minByOrNull { it.dateTimeOfHearing }!!
        if (firstHearing.hearingOutcome == null) {
          log.info("fixing missing adjourns for ${record.chargeNumber}")
          firstHearing.hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "", reason = HearingOutcomeAdjournReason.OTHER, details = "")
          reportedAdjudicationRepository.save(record)
        }
      }
    }
  }

  private fun fixRefChargeProvedMaybe() {
    reportedAdjudicationRepository.findByMigratedIsFalseAndStatusAndHearingsHearingOutcomeCodeIn(
      status = ReportedAdjudicationStatus.CHARGE_PROVED,
      codes = listOf(
        HearingOutcomeCode.REFER_POLICE,
        HearingOutcomeCode.REFER_INAD,
        HearingOutcomeCode.REFER_GOV,
      ),
    )
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record ->
        fix(record)
      }
  }

  private fun fixRefNotProvedMaybe() {
    reportedAdjudicationRepository.findByMigratedIsFalseAndStatusAndHearingsHearingOutcomeCodeIn(
      status = ReportedAdjudicationStatus.NOT_PROCEED,
      codes = listOf(
        HearingOutcomeCode.REFER_POLICE,
        HearingOutcomeCode.REFER_INAD,
        HearingOutcomeCode.REFER_GOV,
      ),
    )
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record ->
        fix(record)
      }
  }

  private fun fixRefDismissedMaybe() {
    reportedAdjudicationRepository.findByMigratedIsFalseAndStatusAndHearingsHearingOutcomeCodeIn(
      status = ReportedAdjudicationStatus.DISMISSED,
      codes = listOf(
        HearingOutcomeCode.REFER_POLICE,
        HearingOutcomeCode.REFER_INAD,
        HearingOutcomeCode.REFER_GOV,
      ),
    )
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record ->
        fix(record)
      }
  }

  private fun fixRefAdjourndMaybe() {
    reportedAdjudicationRepository.findByMigratedIsFalseAndStatus(status = ReportedAdjudicationStatus.ADJOURNED)
      .filter { it.chargeNumber != "3908780" }
      .filter {
        it.hearings.any { hearing ->
          listOf(
            HearingOutcomeCode.REFER_POLICE,
            HearingOutcomeCode.REFER_INAD,
            HearingOutcomeCode.REFER_GOV,
          ).contains(hearing.hearingOutcome?.code)
        }
      }
      .filter { it.getOutcomes().none { outcome -> outcome.code == OutcomeCode.SCHEDULE_HEARING } }
      .forEach { record -> fix(record) }
  }

  private fun fix(record: ReportedAdjudication) {
    val referCount = record.getOutcomes().count { listOf(OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV).contains(it.code) }
    if (referCount > 1) return

    val referIdx = record.hearings.sortedBy { it.dateTimeOfHearing }.indexOfLast { listOf(HearingOutcomeCode.REFER_POLICE, HearingOutcomeCode.REFER_INAD, HearingOutcomeCode.REFER_GOV).contains(it.hearingOutcome?.code) }
    val nextHearingAfter = record.hearings.sortedBy { it.dateTimeOfHearing }.getOrNull(referIdx + 1)
    nextHearingAfter?.let {
      val referOutcome = record.getOutcomes().sortedBy { outcome -> outcome.getCreatedDateTime() }.lastOrNull { outcome -> listOf(OutcomeCode.REFER_POLICE, OutcomeCode.REFER_INAD, OutcomeCode.REFER_GOV).contains(outcome.code) }
      referOutcome?.let {
        log.info("adding scheduled hearing for ${record.chargeNumber}")
        record.addOutcome(
          Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = it.getCreatedDateTime()!!.plusSeconds(10)),
        )
        reportedAdjudicationRepository.save(record)
      }
    }
  }

  private fun fixRanbyOutstanding() {
    // hard code this one - 3908780
    reportedAdjudicationRepository.findByChargeNumber(chargeNumber = "3908780")?.let { ranbyIssue ->

      if (ranbyIssue.status == ReportedAdjudicationStatus.CHARGE_PROVED) return
      val referPoliceOutcome = ranbyIssue.getOutcomes().first()

      // basically.  needs a schedule hearing outcome after the police refer.
      ranbyIssue.addOutcome(
        Outcome(
          code = OutcomeCode.SCHEDULE_HEARING,
          actualCreatedDate = referPoliceOutcome.getCreatedDateTime()!!.plusMinutes(1),
        ),
      )
      // needs a charge proved after that/
      ranbyIssue.addOutcome(
        Outcome(
          code = OutcomeCode.CHARGE_PROVED,
          actualCreatedDate = referPoliceOutcome.getCreatedDateTime()!!.plusMinutes(2),
        ),
      )
      // and needs to turn the final hearing to be completed.
      ranbyIssue.getLatestHearing()!!.hearingOutcome!!.code = HearingOutcomeCode.COMPLETE

      ranbyIssue.status = ReportedAdjudicationStatus.CHARGE_PROVED

      reportedAdjudicationRepository.save(ranbyIssue)
    }
  }

  private fun fixStocken() {
    reportedAdjudicationRepository.findByChargeNumber(chargeNumber = "4046986")?.let {
        stoken ->
      if (stoken.getOutcomes().any { it.code == OutcomeCode.SCHEDULE_HEARING }) return
      // simply add a schedule hearing in after refer gov outcome
      val referBackToGov = stoken.getOutcomes().first { it.code == OutcomeCode.REFER_GOV }
      stoken.addOutcome(
        Outcome(code = OutcomeCode.SCHEDULE_HEARING, actualCreatedDate = referBackToGov.getCreatedDateTime()!!.plusMinutes(1)),
      )
      reportedAdjudicationRepository.save(stoken)
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
