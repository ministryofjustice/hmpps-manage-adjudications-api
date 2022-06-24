package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "offence")
data class Offence(
  override val id: Long? = null,
  var offenceCode: Int,
  var victimPrisonersNumber: String? = null,
  var victimStaffUsername: String? = null,
  var victimOtherPersonsName: String? = null,
) : BaseEntity()
