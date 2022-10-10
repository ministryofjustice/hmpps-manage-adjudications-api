package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "incident_details")
data class IncidentDetails(
  override val id: Long? = null,
  var locationId: Long,
  var dateTimeOfIncident: LocalDateTime,
  var dateTimeOfDiscovery: LocalDateTime? = null,
  var handoverDeadline: LocalDateTime,
) : BaseEntity()
