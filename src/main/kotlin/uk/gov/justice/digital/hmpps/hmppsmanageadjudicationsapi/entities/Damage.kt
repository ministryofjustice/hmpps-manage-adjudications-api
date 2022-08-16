package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.Table

enum class DamageCode {
  ELECTRICAL_REPAIR,
  PLUMBING_REPAIR,
  FURNITURE_OR_FABRIC_REPAIR,
  LOCK_REPAIR,
  REDECORATION,
  CLEANING,
  REPLACE_AN_ITEM
}

@Entity
@Table(name = "damages")
data class Damage(
  override val id: Long? = null,
  @Length(max = 32)
  var code: DamageCode,
  @Length(max = 4000)
  var details: String
) : BaseEntity()

@Entity
@Table(name = "reported_damages")
data class ReportedDamage(
  override val id: Long? = null,
  @Length(max = 32)
  var code: DamageCode,
  @Length(max = 4000)
  var details: String
) : BaseEntity()
