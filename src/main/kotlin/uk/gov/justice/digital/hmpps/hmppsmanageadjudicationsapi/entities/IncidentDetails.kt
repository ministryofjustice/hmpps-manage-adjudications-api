package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "incident_details")
class IncidentDetails(
  val locationId: Long,
  val dateTimeOfIncident: LocalDateTime,
) : BaseEntity()
