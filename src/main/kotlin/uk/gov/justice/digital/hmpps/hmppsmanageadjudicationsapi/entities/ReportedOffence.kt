package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "reported_offence")
data class ReportedOffence(
  override val id: Long? = null,
  var offenceCode: Int,
  var victimPrisonersNumber: String? = null,
) : BaseEntity()
