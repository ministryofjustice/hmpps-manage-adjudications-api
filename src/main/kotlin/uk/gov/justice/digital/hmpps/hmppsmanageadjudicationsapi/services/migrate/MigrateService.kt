package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

class ExistingRecordConflictException(message: String) : Exception(message)

class UnableToMigrateException(message: String) : Exception(message)

@Transactional
@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val migrateNewRecordService: MigrateNewRecordService,
  private val migrateExistingRecordService: MigrateExistingRecordService,
) {

  fun reset() {
    reportedAdjudicationRepository.deleteByMigratedIsTrue()
    reportedAdjudicationRepository.findAll().forEach { it.resetExistingRecord() }
  }

  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val reportedAdjudication = reportedAdjudicationRepository.findByChargeNumber(
      chargeNumber = adjudicationMigrateDto.oicIncidentId.toString(),
    )

    return if (reportedAdjudication != null && adjudicationMigrateDto.offenceSequence == 1L) {
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

  private fun ReportedAdjudication.resetExistingRecord() {
    this.hearings.removeIf { it.migrated }
    this.punishmentComments.removeIf { it.migrated }
    this.damages.removeIf { it.migrated }
    this.evidence.removeIf { it.migrated }
    this.witnesses.removeIf { it.migrated }

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
