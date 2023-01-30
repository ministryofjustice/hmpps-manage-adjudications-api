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
  @Enumerated(EnumType.STRING)
  var finding: HearingOutcomeFinding? = null,
  @Enumerated(EnumType.STRING)
  var plea: HearingOutcomePlea? = null,
) : BaseEntity()

enum class HearingOutcomeCode {
  COMPLETE,
  REFER_POLICE,
  REFER_INAD,
  ADJOURN,
}

enum class HearingOutcomeReason {
  TEST, TEST2
}

enum class HearingOutcomeFinding {
  PROVED, DISMISSED, NOT_PROCEED_WITH
}

enum class HearingOutcomePlea {
  TEST, TEST2
}
