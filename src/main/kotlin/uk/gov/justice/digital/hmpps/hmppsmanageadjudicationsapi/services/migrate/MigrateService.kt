package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

class ExistingRecordConflictException(message: String) : Exception(message)

class UnableToMigrateException(message: String) : Exception(message)

class SkipExistingRecordException : Exception("Skip existing record flag is true")

@Transactional
@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val migrateNewRecordService: MigrateNewRecordService,
  private val migrateExistingRecordService: MigrateExistingRecordService,
  private val featureFlagsConfig: FeatureFlagsConfig,
) {

  fun reset() {
    log.info("starting migration reset")
    removeNewRecords()
    resetExistingRecords()
  }

  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val reportedAdjudication = reportedAdjudicationRepository.findByChargeNumber(
      chargeNumber = adjudicationMigrateDto.oicIncidentId.toString(),
    )

    return if (reportedAdjudication != null && adjudicationMigrateDto.offenceSequence == 1L) {
      if (featureFlagsConfig.skipExistingRecords) throw SkipExistingRecordException()

      migrateExistingRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
        existingAdjudication = reportedAdjudication,
      )
    } else {
      migrateNewRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
      )
    }
  }

  private fun removeNewRecords() {
    reportedAdjudicationRepository.deleteByMigratedIsTrue()
    log.info("finishing migration reset")
  }

  private fun resetExistingRecords() {
    log.info("reset existing reset")
    if (!featureFlagsConfig.skipExistingRecords) {
      reportedAdjudicationRepository.findAll()
        .forEach { it.resetExistingRecord() }
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
