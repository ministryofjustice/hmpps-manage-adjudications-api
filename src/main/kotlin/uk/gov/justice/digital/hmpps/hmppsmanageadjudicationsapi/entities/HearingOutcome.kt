package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "hearing_outcome")
data class HearingOutcome(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @field:Length(max = 32)
  var adjudicator: String,
  @field:Length(max = 32)
  var reason: String? = null,
  @Enumerated(EnumType.STRING)
  var code: HearingOutcomeCode? = null,
) : BaseEntity()

enum class HearingOutcomeCode {
  COMPLETE,
  REFER_POLICE,
  REFER_INAD,
  ADJOURN,
}
