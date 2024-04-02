package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.OutcomeDto
import java.time.LocalDateTime

@Entity
@Table(name = "outcome")
data class Outcome(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @Enumerated(EnumType.STRING)
  var notProceedReason: NotProceedReason? = null,
  @Enumerated(EnumType.STRING)
  var code: OutcomeCode,
  @Enumerated(EnumType.STRING)
  var quashedReason: QuashedReason? = null,
  var oicHearingId: Long? = null,
  var deleted: Boolean? = null,
  var actualCreatedDate: LocalDateTime? = null,
  @Enumerated(EnumType.STRING)
  var referGovReason: ReferGovReason? = null,
) : BaseEntity() {
  fun getCreatedDateTime(): LocalDateTime? = this.actualCreatedDate ?: this.createDateTime

  fun toDto(hasLinkedAda: Boolean): OutcomeDto =
    OutcomeDto(
      id = this.id,
      code = this.code,
      details = this.details,
      // added due to migration - not applicable for DPS app itself
      reason = this.notProceedReason ?: if (this.code == OutcomeCode.NOT_PROCEED) NotProceedReason.OTHER else null,
      quashedReason = this.quashedReason,
      referGovReason = this.referGovReason,
      canRemove = !hasLinkedAda,
    )
}

enum class OutcomeCode(val status: ReportedAdjudicationStatus) {
  REFER_POLICE(ReportedAdjudicationStatus.REFER_POLICE) {
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
  NOT_PROCEED(ReportedAdjudicationStatus.NOT_PROCEED),
  DISMISSED(ReportedAdjudicationStatus.DISMISSED),
  PROSECUTION(ReportedAdjudicationStatus.PROSECUTION),
  SCHEDULE_HEARING(ReportedAdjudicationStatus.SCHEDULED),
  CHARGE_PROVED(ReportedAdjudicationStatus.CHARGE_PROVED),
  QUASHED(ReportedAdjudicationStatus.QUASHED),
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
  ANOTHER_WAY,
  RELEASED,
  WITNESS_NOT_ATTEND,
  UNFIT,
  FLAWED,
  EXPIRED_NOTICE,
  EXPIRED_HEARING,
  NOT_FAIR,
  OTHER,
}

enum class QuashedReason {
  FLAWED_CASE,
  JUDICIAL_REVIEW,
  APPEAL_UPHELD,
  OTHER,
}

enum class ReferGovReason {
  REVIEW_FOR_REFER_POLICE,
  GOV_INQUIRY,
  OTHER,
}
