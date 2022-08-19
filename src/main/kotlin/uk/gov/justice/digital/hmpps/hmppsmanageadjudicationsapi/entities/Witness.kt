package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.Table

enum class WitnessCode {
  PRISON_OFFICER,
  STAFF,
  OTHER,
}

@Entity
@Table(name = "witness")
data class Witness(
  override val id: Long? = null,
  @Length(max = 32)
  var code: WitnessCode,
  @Length(max = 32)
  var firstName: String,
  @Length(max = 32)
  var lastName: String,
  @Length(max = 32)
  var reporter: String
) : BaseEntity()

@Entity
@Table(name = "reported_witness")
data class ReportedWitness(
  override val id: Long? = null,
  @Length(max = 32)
  var code: WitnessCode,
  @Length(max = 32)
  var firstName: String,
  @Length(max = 32)
  var lastName: String,
  @Length(max = 32)
  var reporter: String
) : BaseEntity()
