package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Plea
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table
import javax.validation.ValidationException

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

enum class HearingOutcomePlea(val plea: Plea? = null) { // TODO map these
  UNFIT, ABSTAIN, GUILTY, NOT_GUILTY, NOT_ASKED
}
