package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "incident_statement")
data class IncidentStatement(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var statement: String? = null,
  var completed: Boolean? = false
) : BaseEntity()
