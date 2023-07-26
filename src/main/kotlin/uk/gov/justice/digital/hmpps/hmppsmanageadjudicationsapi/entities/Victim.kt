package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "victim")
data class Victim(
  override val id: Long? = null,
  var victimPrisonersNumber: String? = null,
  var victimStaffUsername: String? = null,
  var victimOtherPersonsName: String? = null,
  ): BaseEntity()