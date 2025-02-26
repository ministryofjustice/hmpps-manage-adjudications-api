package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.springframework.data.repository.CrudRepository
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationChargeStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.TransferMigrationCharges

interface TransferMigrationChargesRepository : CrudRepository<TransferMigrationCharges, Long> {

  fun findFirstByStatus(status: TransferMigrationChargeStatus): TransferMigrationCharges?
  fun findByChargeNumber(chargeNumber: String): TransferMigrationCharges?
}
