package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table

@Deprecated("this table is purely to support nomis migration and is readonly for adjudications UI")
@Entity
@Table(name = "additional_victim")
data class AdditionalVictim(
  override val id: Long? = null,
  var victimPrisonersNumber: String? = null,
  var victimStaffUsername: String? = null,
) : BaseEntity()
