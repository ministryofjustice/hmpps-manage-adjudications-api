package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "associate")
data class Associate(
  override val id: Long? = null,
  var incidentRoleAssociatedPrisonersNumber: String?,
  var incidentRoleAssociatedPrisonersName: String?,
  ): BaseEntity()