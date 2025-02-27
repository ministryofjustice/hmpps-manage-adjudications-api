package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.data.domain.PageRequest
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

  fun processRecords(chargeNumbers: List<String>): List<String> {
    val complete = mutableListOf<String>()
    val adjudications = reportedAdjudicationRepository.findByChargeNumberIn(chargeNumbers)
    adjudications.forEach {
      val prisoner = try {
        prisonerSearchService.getPrisonerDetail(it.prisonerNumber)!!
      } catch (e: Exception) {
        return@forEach
      }
      complete.add(it.chargeNumber)
      if (prisoner.prisonId == "OUT") {
        return@forEach
      }
      if (prisoner.prisonId != it.originatingAgencyId) {
        it.overrideAgencyId = prisoner.prisonId
      } else {
        it.overrideAgencyId = null
      }
    }
    return complete
  }

  fun completeProcessing(chargeNumbers: List<String>) {
    val records = transferMigrationChargeRepository.findByChargeNumberIn(chargeNumbers)
    records.forEach {
      it.status = TransferMigrationChargeStatus.COMPLETE
    }
  }

  fun getNextRecords(): List<String> {
    val records = transferMigrationChargeRepository.findByStatus(TransferMigrationChargeStatus.READY, PageRequest.of(0, 1000))
    records.forEach { it.status = TransferMigrationChargeStatus.PROCESSING }
    return records.map { it.chargeNumber }
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
