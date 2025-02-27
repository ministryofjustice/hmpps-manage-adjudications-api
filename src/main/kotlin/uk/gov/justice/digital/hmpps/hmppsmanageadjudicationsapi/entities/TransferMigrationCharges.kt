package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import org.hibernate.validator.constraints.Length

@Entity
class TransferMigrationCharges(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @field:Length(max = 16)
  var chargeNumber: String,
  @Enumerated(EnumType.STRING)
  var status: TransferMigrationChargeStatus,
)

enum class TransferMigrationChargeStatus {
  READY,
  PROCESSING,
  COMPLETE,
}
