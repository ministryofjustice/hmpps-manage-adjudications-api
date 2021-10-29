package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.springframework.data.jpa.domain.support.AuditingEntityListener
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "draft_adjudications")
@EntityListeners(AuditingEntityListener::class)
data class DraftAdjudication(
  override val id: Long? = null,
  val prisonerNumber: String,
  @OneToOne(optional = true, cascade = [CascadeType.ALL])
  @JoinColumn(name = "incident_details_id")
  val incidentDetails: IncidentDetails? = null,
  @OneToOne(optional = true, cascade = [CascadeType.ALL])
  @JoinColumn(name = "incident_statement_id")
  private var incidentStatement: IncidentStatement? = null,
) : BaseEntity() {

  fun addIncidentStatement(statement: String) {
    this.incidentStatement = IncidentStatement(statement)
  }

  fun getIncidentStatement(): IncidentStatement? = incidentStatement
}
