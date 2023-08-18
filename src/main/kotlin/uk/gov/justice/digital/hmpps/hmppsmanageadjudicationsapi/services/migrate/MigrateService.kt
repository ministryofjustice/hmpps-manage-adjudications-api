package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

class ExistingRecordConflictException(message: String) : Exception(message)

class UnableToMigrateException(message: String) : Exception(message)

class SkipExistingRecordException : Exception("Skip existing record flag is true")

@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val migrateNewRecordService: MigrateNewRecordService,
  private val migrateExistingRecordService: MigrateExistingRecordService,
  private val featureFlagsConfig: FeatureFlagsConfig,
  private val resetExistingRecordService: ResetExistingRecordService,
) {

  fun reset() {
    log.info("starting migration reset")
    removeNewRecords()
    resetExistingRecords()
  }

  @Transactional
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
    reportedAdjudicationRepository.findByMigratedIsTrue().forEach { deleteRecord(it.id!!) }
    log.info("finishing migration reset")
  }

  @Transactional
  private fun deleteRecord(id: Long) {
    reportedAdjudicationRepository.deleteById(id)
  }

  private fun resetExistingRecords() {
    if (!featureFlagsConfig.skipExistingRecords) {
      reportedAdjudicationRepository.findAll()
        .forEach { resetExistingRecordService.reset(it.id!!) }
      log.info("finished reset existing")
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
