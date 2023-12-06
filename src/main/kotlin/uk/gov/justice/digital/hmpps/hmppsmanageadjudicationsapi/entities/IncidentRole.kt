package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "incident_role")
data class IncidentRole(
  override val id: Long? = null,
  var roleCode: String?,
  @field:Length(max = 7)
  var associatedPrisonersNumber: String?,
  @field:Length(max = 100)
  var associatedPrisonersName: String?,
) : BaseEntity()
