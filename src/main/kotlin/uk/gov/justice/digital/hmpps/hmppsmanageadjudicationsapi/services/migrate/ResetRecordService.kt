package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.hibernate.ScrollMode
import org.hibernate.SessionFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Service
class ResetRecordService(
  private val sessionFactory: SessionFactory,
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun remove() {
    val session = sessionFactory.openStatelessSession()
    val scrollable = session.createNamedQuery("findMigratedRecordsToDelete", ReportedAdjudication::class.java).scroll(ScrollMode.FORWARD_ONLY)

    while (scrollable.next()) {
      reportedAdjudicationRepository.deleteById(scrollable.get().id!!)
    }

    session.close()
  }

  @Transactional
  fun reset() {
    reportedAdjudicationRepository.findByMigratedIsFalse().forEach {
      it.resetExistingRecord()
    }
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
