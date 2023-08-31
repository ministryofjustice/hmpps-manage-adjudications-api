package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime

enum class DamageCode {
  ELECTRICAL_REPAIR,
  PLUMBING_REPAIR,
  FURNITURE_OR_FABRIC_REPAIR,
  LOCK_REPAIR,
  REDECORATION,
  CLEANING,
  REPLACE_AN_ITEM,
}

@Entity
@Table(name = "damages")
data class Damage(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: DamageCode,
  @field:Length(max = 4000)
  var details: String,
  @field:Length(max = 32)
  var reporter: String,
) : BaseEntity()

@Entity
@Table(name = "reported_damages")
data class ReportedDamage(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: DamageCode,
  @field:Length(max = 4000)
  var details: String,
  @field:Length(max = 32)
  var reporter: String,
  var migrated: Boolean = false,
  var dateAdded: LocalDateTime? = null,
  var repairCost: Double? = null,
) : BaseEntity()
