package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table
import javax.validation.ValidationException

@Entity
@Table(name = "outcome")
data class Outcome(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @Enumerated(EnumType.STRING)
  var reason: NotProceedReason? = null,
  @Enumerated(EnumType.STRING)
  var code: OutcomeCode,
  var amount: Double? = null,
  var caution: Boolean? = null,
  @Enumerated(EnumType.STRING)
  var quashedReason: QuashedReason? = null,
  var oicHearingId: Long? = null,
) : BaseEntity()

enum class OutcomeCode(val status: ReportedAdjudicationStatus, val finding: Finding? = null) { // TODO map these.
  REFER_POLICE(ReportedAdjudicationStatus.REFER_POLICE, Finding.REF_POLICE) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, PROSECUTION, SCHEDULE_HEARING)
    }
  },
  REFER_INAD(ReportedAdjudicationStatus.REFER_INAD) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, SCHEDULE_HEARING)
    }
  },
  NOT_PROCEED(ReportedAdjudicationStatus.NOT_PROCEED, Finding.NOT_PROCEED),
  DISMISSED(ReportedAdjudicationStatus.DISMISSED, Finding.REFUSED), // TODO confirm with John
  PROSECUTION(ReportedAdjudicationStatus.PROSECUTION, Finding.PROSECUTED),
  SCHEDULE_HEARING(ReportedAdjudicationStatus.SCHEDULED),
  CHARGE_PROVED(ReportedAdjudicationStatus.CHARGE_PROVED, Finding.PROVED),
  QUASHED(ReportedAdjudicationStatus.QUASHED, Finding.QUASHED),
  ;

  fun validateReferral(): OutcomeCode {
    if (referrals().none { this == it }) throw ValidationException("invalid referral type")
    return this
  }

  open fun nextStates(): List<OutcomeCode> = listOf()

  fun canTransitionTo(to: OutcomeCode): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }

  companion object {
    fun referrals() = listOf(REFER_POLICE, REFER_INAD)
    fun completedHearings() = listOf(CHARGE_PROVED, DISMISSED, NOT_PROCEED)
  }
}

enum class NotProceedReason {
  ANOTHER_WAY, RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, OTHER
}

enum class QuashedReason {
  FLAWED_CASE, JUDICIAL_REVIEW, APPEAL_UPHELD, OTHER
}
