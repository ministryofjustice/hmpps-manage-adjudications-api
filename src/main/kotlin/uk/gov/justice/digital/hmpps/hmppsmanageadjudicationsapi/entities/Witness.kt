package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

enum class WitnessCode {
  OFFICER,
  STAFF,
  OTHER_PERSON,
  VICTIM,
  PRISONER,
}

@Entity
@Table(name = "witness")
data class Witness(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: WitnessCode,
  @field:Length(max = 32)
  var firstName: String,
  @field:Length(max = 32)
  var lastName: String,
  @field:Length(max = 32)
  var reporter: String,
) : BaseEntity()

@Entity
@Table(name = "reported_witness")
data class ReportedWitness(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: WitnessCode,
  @field:Length(max = 32)
  var firstName: String,
  @field:Length(max = 32)
  var lastName: String,
  @field:Length(max = 32)
  var reporter: String,
) : BaseEntity()
