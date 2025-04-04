package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedWitnessDto
import java.time.LocalDateTime

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
  @field:Length(max = 32)
  var username: String? = null,
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
  @field:Length(max = 32)
  var username: String? = null,
  var dateAdded: LocalDateTime? = null,
  @field:Length(max = 240)
  var comment: String? = null,
) : BaseEntity() {
  fun toDto(): ReportedWitnessDto = ReportedWitnessDto(
    code = this.code,
    firstName = this.firstName,
    lastName = this.lastName,
    reporter = this.reporter,
  )
}
