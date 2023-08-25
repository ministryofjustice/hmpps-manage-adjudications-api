package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Service
class ResetRecordService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun remove(agency: String) {
    reportedAdjudicationRepository.findRecordsToDelete(agency).forEach {
      reportedAdjudicationRepository.deleteById(it)
    }
  }

  @Transactional
  fun reset(chargeNumber: String) {
    reportedAdjudicationRepository.findByChargeNumber(chargeNumber)!!.resetExistingRecord()
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

    this.hearings.forEach {
      if (it.hearingPreMigrate != null) {
        it.oicHearingType = it.hearingPreMigrate!!.oicHearingType
        it.dateTimeOfHearing = it.hearingPreMigrate!!.dateTimeOfHearing
        it.locationId = it.hearingPreMigrate!!.locationId
        it.hearingPreMigrate = null
      }
      if (it.hearingOutcome?.hearingOutcomePreMigrate != null) {
        it.hearingOutcome!!.code = it.hearingOutcome!!.hearingOutcomePreMigrate!!.code
        it.hearingOutcome!!.adjudicator = it.hearingOutcome!!.hearingOutcomePreMigrate!!.adjudicator
        it.hearingOutcome!!.hearingOutcomePreMigrate = null
      }
    }

    this.getPunishments().forEach {
      it.nomisStatus = null
      if (it.migrated) this.removePunishment(it)
      it.schedule.removeIf { ps -> ps.migrated }
      if (it.punishmentPreMigrate != null) {
        it.amount = it.punishmentPreMigrate!!.amount
        it.stoppagePercentage = it.punishmentPreMigrate!!.stoppagePercentage
        it.consecutiveChargeNumber = it.punishmentPreMigrate!!.consecutiveChargeNumber
        it.type = it.punishmentPreMigrate!!.type
        it.privilegeType = it.punishmentPreMigrate!!.privilegeType
        it.otherPrivilege = it.punishmentPreMigrate!!.otherPrivilege
        it.punishmentPreMigrate = null
      }
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
