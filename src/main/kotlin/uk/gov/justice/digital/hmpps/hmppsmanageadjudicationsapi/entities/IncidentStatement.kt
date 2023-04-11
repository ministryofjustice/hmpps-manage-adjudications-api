package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "incident_statement")
data class IncidentStatement(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var statement: String? = null,
  var completed: Boolean? = false,
) : BaseEntity()
