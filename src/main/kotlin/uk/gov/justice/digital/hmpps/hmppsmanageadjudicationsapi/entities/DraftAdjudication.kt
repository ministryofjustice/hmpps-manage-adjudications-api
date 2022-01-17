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
  val reportNumber: Long? = null,
  val reportByUserId: String? = null,
  val agencyId: String,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_details_id")
  val incidentDetails: IncidentDetails,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_role_id")
  var incidentRole: IncidentRole,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_statement_id")
  var incidentStatement: IncidentStatement? = null,
) : BaseEntity()
