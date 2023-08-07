package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "reported_offence")
data class ReportedOffence(
  override val id: Long? = null,
  var offenceCode: Int,
  var victimPrisonersNumber: String? = null,
  var victimStaffUsername: String? = null,
  var victimOtherPersonsName: String? = null,
  var nomisOffenceCode: String? = null,
  var nomisOffenceDescription: String? = null,
) : BaseEntity()
