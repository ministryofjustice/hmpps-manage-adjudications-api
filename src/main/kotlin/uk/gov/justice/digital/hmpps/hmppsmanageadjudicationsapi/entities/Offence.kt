package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "offence")
data class Offence(
  override val id: Long? = null,
  var offenceCode: Int,
  @field:Length(max = 7)
  var victimPrisonersNumber: String? = null,
  @field:Length(max = 30)
  var victimStaffUsername: String? = null,
  @field:Length(max = 100)
  var victimOtherPersonsName: String? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "offence_fk_id")
  var protectedCharacteristics: MutableList<DraftProtectedCharacteristics> = mutableListOf(),
) : BaseEntity()
