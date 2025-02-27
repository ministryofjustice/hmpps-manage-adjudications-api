package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationChargeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationCharges
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.TransferMigrationChargesRepository

@Transactional
@Service
class TransferMigrationService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
  private val transferMigrationChargeRepository: TransferMigrationChargesRepository,
  private val prisonerSearchService: PrisonerSearchService,
) {

  fun processRecord(chargeNumber: String) {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber(chargeNumber)!!
    val prisoner = prisonerSearchService.getPrisonerDetail(adjudication.prisonerNumber)!!

    adjudication.overrideAgencyId = prisoner.prisonId
  }

  fun completeProcessing(chargeNumber: String) {
    val record = transferMigrationChargeRepository.findByChargeNumber(chargeNumber) ?: return
    record.status = TransferMigrationChargeStatus.COMPLETE
  }

  fun getNextRecord(): TransferMigrationCharges? {
    val record = transferMigrationChargeRepository.findFirstByStatus(TransferMigrationChargeStatus.READY)
    record?.let { it.status = TransferMigrationChargeStatus.PROCESSING }
    return record
  }

  fun setupChargeIds() {
    val chargeIds = reportedAdjudicationRepository.findChargeNumbersByStatus(listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name))
    transferMigrationChargeRepository.saveAll(
      chargeIds.map {
        TransferMigrationCharges(chargeNumber = it, status = TransferMigrationChargeStatus.READY)
      },
    )
  }
}
