package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length

enum class Plea {
  GUILTY,
  NOT_GUILTY,
  REFUSED,
  UNFIT,
  NOT_ASKED,
}

@Entity
@Table(name = "hearing_outcome")
data class HearingOutcome(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @field:Length(max = 32)
  var adjudicator: String,
  @Enumerated(EnumType.STRING)
  var adjournReason: HearingOutcomeAdjournReason? = null,
  @Enumerated(EnumType.STRING)
  var code: HearingOutcomeCode,
  @Enumerated(EnumType.STRING)
  var plea: HearingOutcomePlea? = null,
  var nomisOutcome: Boolean = false,
  var migrated: Boolean = false,
) : BaseEntity()

enum class HearingOutcomeCode(val outcomeCode: OutcomeCode? = null) {
  COMPLETE,
  REFER_POLICE(OutcomeCode.REFER_POLICE),
  REFER_INAD(OutcomeCode.REFER_INAD),
  ADJOURN,
  NOMIS,
  REFER_GOV(OutcomeCode.REFER_GOV),
  ;

  fun validateReferral(): HearingOutcomeCode {
    this.outcomeCode ?: throw ValidationException("invalid referral type")
    return this
  }
}

enum class HearingOutcomeAdjournReason {
  LEGAL_ADVICE,
  LEGAL_REPRESENTATION,
  RO_ATTEND,
  HELP,
  UNFIT,
  WITNESS,
  WITNESS_SUPPORT,
  MCKENZIE,
  EVIDENCE,
  INVESTIGATION,
  OTHER,
}

enum class HearingOutcomePlea(val plea: Plea) {
  UNFIT(Plea.UNFIT),
  ABSTAIN(Plea.REFUSED),
  GUILTY(Plea.GUILTY),
  NOT_GUILTY(Plea.NOT_GUILTY),
  NOT_ASKED(Plea.NOT_ASKED),
}
