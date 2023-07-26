package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Deprecated("this table is purely to support nomis migration and is readonly for adjudications UI")
@Entity
@Table(name = "additional_associate")
data class AdditionalAssociate(
  override val id: Long? = null,
  var incidentRoleAssociatedPrisonersNumber: String?,
) : BaseEntity()
