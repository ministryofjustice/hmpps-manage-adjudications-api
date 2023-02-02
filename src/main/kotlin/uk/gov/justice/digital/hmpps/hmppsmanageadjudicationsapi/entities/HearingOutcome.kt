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
  var reason: HearingOutcomeAdjournReason? = null,
  @Enumerated(EnumType.STRING)
  var code: HearingOutcomeCode,
  @Enumerated(EnumType.STRING)
  var finding: HearingOutcomeFinding? = null,
  @Enumerated(EnumType.STRING)
  var plea: HearingOutcomePlea? = null,
) : BaseEntity()

enum class HearingOutcomeCode {
  COMPLETE, REFER_POLICE, REFER_INAD, ADJOURN,
}

enum class HearingOutcomeAdjournReason {
  LEGAL_ADVICE, LEGAL_REPRESENTATION, RO_ATTEND, HELP, UNFIT, WITNESS, WITNESS_SUPPORT, MCKENZIE, EVIDENCE, INVESTIGATION, OTHER
}

enum class HearingOutcomeFinding {
  PROVED, DISMISSED, NOT_PROCEED_WITH
}

enum class HearingOutcomePlea {
  UNFIT, ABSTAIN, GUILTY, NOT_GUILTY, NOT_ASKED
}
