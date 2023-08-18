package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class ResetExistingRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun reset(id: Long) {
    reportedAdjudicationRepository.findById(id).get().resetExistingRecord()
  }

  private fun ReportedAdjudication.resetExistingRecord() {
    this.hearings.removeIf { it.migrated }
    this.punishmentComments.removeIf { it.migrated }
    this.damages.removeIf { it.migrated }
    this.evidence.removeIf { it.migrated }
    this.witnesses.removeIf { it.migrated }

    this.hearings.filter { it.hearingOutcome?.nomisOutcome == true }.forEach {
      it.hearingOutcome!!.nomisOutcome = false
      it.hearingOutcome!!.adjudicator = ""
      it.hearingOutcome!!.code = HearingOutcomeCode.NOMIS
    }

    this.hearings.filter { it.hearingOutcome?.migrated == true }.forEach {
      it.hearingOutcome = null
    }

    this.getPunishments().forEach {
      if (it.migrated) this.removePunishment(it)
    }

    this.getOutcomes().forEach {
      if (it.migrated) this.removeOutcome(it)
    }

    this.offenceDetails.forEach {
      if (it.migrated) {
        it.offenceCode = it.actualOffenceCode!!
        it.migrated = false
        it.nomisOffenceCode = null
        it.nomisOffenceDescription = null
        it.actualOffenceCode = null
      }
    }

    this.statusBeforeMigration?.let {
      this.status = it
    }
  }
}
