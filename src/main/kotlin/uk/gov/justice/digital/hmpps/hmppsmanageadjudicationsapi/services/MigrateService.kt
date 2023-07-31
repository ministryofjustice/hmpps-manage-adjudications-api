package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.MigrateResponse
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val migrateNewRecordService: MigrateNewRecordService,
  private val migrateExistingRecordService: MigrateExistingRecordService,
) {

  fun reset() {
    reportedAdjudicationRepository.deleteByMigratedIsTrue()
  }

  fun accept(adjudicationMigrateDto: AdjudicationMigrateDto): MigrateResponse {
    val reportedAdjudication = reportedAdjudicationRepository.findByChargeNumber(
      chargeNumber = adjudicationMigrateDto.oicIncidentId.toString(),
    )

    return when (reportedAdjudication) {
      null -> migrateNewRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
      )
      else -> migrateExistingRecordService.accept(
        adjudicationMigrateDto = adjudicationMigrateDto,
        reportedAdjudication = reportedAdjudication,
      )
    }
  }
}
