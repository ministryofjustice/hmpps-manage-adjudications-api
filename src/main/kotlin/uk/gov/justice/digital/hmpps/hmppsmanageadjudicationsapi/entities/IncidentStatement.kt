package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "incident_statement")
class IncidentStatement(
  val statement: String,
) : BaseEntity()
