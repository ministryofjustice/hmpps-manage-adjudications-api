package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "draft_adjudications")
data class DraftAdjudication(
  override val id: Long? = null,
  val prisonerNumber: String,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_details_id")
  val incidentDetails: IncidentDetails? = null,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_statement_id")
  var incidentStatement: IncidentStatement? = null,
) : BaseEntity()
