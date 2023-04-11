package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "incident_role")
data class IncidentRole(
  override val id: Long? = null,
  var roleCode: String?,
  var associatedPrisonersNumber: String?,
  var associatedPrisonersName: String?,
) : BaseEntity()
