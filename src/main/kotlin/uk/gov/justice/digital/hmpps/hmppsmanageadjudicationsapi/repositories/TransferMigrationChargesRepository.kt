package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.domain.Pageable
import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationChargeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationCharges

interface TransferMigrationChargesRepository : CrudRepository<TransferMigrationCharges, Long> {

  fun findByStatus(status: TransferMigrationChargeStatus, page: Pageable): List<TransferMigrationCharges>
  fun findByChargeNumber(chargeNumber: String): TransferMigrationCharges?
  fun findByChargeNumberIn(chargeNumbers: List<String>): List<TransferMigrationCharges>
}
