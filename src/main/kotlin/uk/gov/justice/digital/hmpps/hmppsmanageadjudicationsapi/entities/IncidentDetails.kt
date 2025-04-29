package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "incident_details")
data class IncidentDetails(
  override val id: Long? = null,
  var locationId: Long? = null,
  @Transient
  val locationName: String? = null,
  var locationUuid: UUID,
  var dateTimeOfIncident: LocalDateTime,
  var dateTimeOfDiscovery: LocalDateTime,
  var handoverDeadline: LocalDateTime,
) : BaseEntity()
