package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.ChargeNumberMapping
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

class ExistingRecordConflictException(message: String) : RuntimeException(message)

class NomisDeletedHearingsOrOutcomesException(message: String) : RuntimeException(message)

class UnableToMigrateException(message: String) : RuntimeException(message)

class DuplicateCreationException(message: String) : RuntimeException(message)

class SkipExistingRecordException : Exception("Skip existing record flag is true")

@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val migrateNewRecordService: MigrateNewRecordService,
  private val migrateExistingRecordService: MigrateExistingRecordService,
  private val featureFlagsConfig: FeatureFlagsConfig,
) {

  @Transactional
  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val reportedAdjudication = reportedAdjudicationRepository.findByChargeNumberIn(
      chargeNumbers = listOf(
        adjudicationMigrateDto.oicIncidentId.toString(),
        "${adjudicationMigrateDto.oicIncidentId}-${adjudicationMigrateDto.offenceSequence}",
      ),
    )

    val existingRecord = reportedAdjudication.firstOrNull { !it.migrated }
    val duplicatedRecord = reportedAdjudication.firstOrNull { it.migrated }

    return if (existingRecord != null && adjudicationMigrateDto.offenceSequence == 1L) {
      if (featureFlagsConfig.skipExistingRecords) throw SkipExistingRecordException()
      if (existingRecord.status == ReportedAdjudicationStatus.REJECTED) {
        return MigrateResponse(
          chargeNumberMapping = ChargeNumberMapping(
            chargeNumber = existingRecord.chargeNumber,
            oicIncidentId = adjudicationMigrateDto.oicIncidentId,
            offenceSequence = adjudicationMigrateDto.offenceSequence,
          ),
        )
      }
      migrateExistingRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
        existingAdjudication = existingRecord,
      )
    } else {
      duplicatedRecord?.let {
        throw DuplicateCreationException("already processed this record")
      }
      migrateNewRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
      )
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
