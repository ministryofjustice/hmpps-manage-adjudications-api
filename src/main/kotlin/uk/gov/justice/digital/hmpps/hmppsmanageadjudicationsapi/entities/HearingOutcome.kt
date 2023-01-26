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
  @Enumerated(EnumType.STRING)
  var reason: HearingOutcomeReason? = null,
  @Enumerated(EnumType.STRING)
  var code: HearingOutcomeCode,
) : BaseEntity()

enum class HearingOutcomeCode {
  COMPLETE,
  REFER_POLICE,
  REFER_INAD,
  ADJOURN,
}

enum class HearingOutcomeReason {
  TEST
}
