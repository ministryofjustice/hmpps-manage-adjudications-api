package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import java.time.LocalDateTime

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
  @Enumerated(EnumType.STRING)
  var quashedReason: QuashedReason? = null,
  var oicHearingId: Long? = null,
  var deleted: Boolean? = null,
  var actualCreatedDate: LocalDateTime? = null,
  var migrated: Boolean = false,
) : BaseEntity()

enum class OutcomeCode(val status: ReportedAdjudicationStatus, val finding: Finding? = null) {
  REFER_POLICE(ReportedAdjudicationStatus.REFER_POLICE, Finding.REF_POLICE) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, PROSECUTION, SCHEDULE_HEARING)
    }
  },
  REFER_INAD(ReportedAdjudicationStatus.REFER_INAD) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, SCHEDULE_HEARING, REFER_GOV)
    }
  },
  REFER_GOV(ReportedAdjudicationStatus.REFER_GOV) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, SCHEDULE_HEARING)
    }
  },
  NOT_PROCEED(ReportedAdjudicationStatus.NOT_PROCEED, Finding.NOT_PROCEED),
  DISMISSED(ReportedAdjudicationStatus.DISMISSED, Finding.D),
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
    fun referrals() = listOf(REFER_POLICE, REFER_INAD, REFER_GOV)
    fun completedHearings() = listOf(CHARGE_PROVED, DISMISSED, NOT_PROCEED)
  }
}

enum class NotProceedReason {
  ANOTHER_WAY, RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, OTHER
}

enum class QuashedReason {
  FLAWED_CASE, JUDICIAL_REVIEW, APPEAL_UPHELD, OTHER
}
