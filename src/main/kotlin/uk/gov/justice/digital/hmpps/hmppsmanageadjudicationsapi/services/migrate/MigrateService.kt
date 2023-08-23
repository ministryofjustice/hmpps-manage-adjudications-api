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
  private val resetRecordService: ResetRecordService,
) {

  fun reset() {
    log.info("starting migration reset")
    resetRecordService.remove()
    if (!featureFlagsConfig.skipExistingRecords) {
      reportedAdjudicationRepository.findByMigratedIsFalse().forEach { resetRecordService.reset(it) }
    }
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

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
