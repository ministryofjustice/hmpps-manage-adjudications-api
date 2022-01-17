package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.Entity
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
) : BaseEntity()
