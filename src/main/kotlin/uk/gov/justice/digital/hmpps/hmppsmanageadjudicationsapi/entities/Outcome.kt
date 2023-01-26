package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "outcome")
data class Outcome(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @field:Length(max = 32)
  var reason: String? = null,
  @Enumerated(EnumType.STRING)
  var code: OutcomeCode,
) : BaseEntity()

enum class OutcomeCode {
  REFER_POLICE,
  NOT_PROCEED,
}
