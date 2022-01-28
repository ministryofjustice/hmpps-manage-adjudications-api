package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "reported_adjudications")
data class ReportedAdjudication(
  override val id: Long? = null,
  var prisonerNumber: String,
  var bookingId: Long,
  var reportNumber: Long,
  var agencyId: String,
  var locationId: Long,
  var dateTimeOfIncident: LocalDateTime,
  var handoverDeadline: LocalDateTime,
  var incidentRoleCode: String?,
  var incidentRoleAssociatedPrisonersNumber: String?,
  var statement: String,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var offences: MutableList<ReportedOffence>? = null,
) : BaseEntity()
