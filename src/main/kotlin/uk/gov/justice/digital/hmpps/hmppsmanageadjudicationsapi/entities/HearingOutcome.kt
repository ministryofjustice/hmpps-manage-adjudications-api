package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length

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
  var plea: HearingOutcomePlea? = null,
) : BaseEntity()

enum class HearingOutcomeCode(val outcomeCode: OutcomeCode? = null) {
  COMPLETE, REFER_POLICE(OutcomeCode.REFER_POLICE), REFER_INAD(OutcomeCode.REFER_INAD), ADJOURN;
  fun validateReferral(): HearingOutcomeCode {
    this.outcomeCode ?: throw ValidationException("invalid referral type")
    return this
  }
}

enum class HearingOutcomeAdjournReason {
  LEGAL_ADVICE, LEGAL_REPRESENTATION, RO_ATTEND, HELP, UNFIT, WITNESS, WITNESS_SUPPORT, MCKENZIE, EVIDENCE, INVESTIGATION, OTHER
}

enum class HearingOutcomePlea {
  UNFIT, ABSTAIN, GUILTY, NOT_GUILTY, NOT_ASKED
}
