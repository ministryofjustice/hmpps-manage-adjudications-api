package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

enum class WitnessCode {
  OFFICER,
  STAFF,
  OTHER_PERSON,
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
  var reporter: String
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
  var reporter: String
) : BaseEntity()
