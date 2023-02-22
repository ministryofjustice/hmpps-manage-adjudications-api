package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

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
) : BaseEntity()

enum class OutcomeCode(val status: ReportedAdjudicationStatus) {
  REFER_POLICE(ReportedAdjudicationStatus.REFER_POLICE) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, PROSECUTION, SCHEDULE_HEARING)
    }
  },
  REFER_INAD(ReportedAdjudicationStatus.REFER_INAD) {
    override fun nextStates(): List<OutcomeCode> {
      return listOf(NOT_PROCEED, SCHEDULE_HEARING)
    }
  },
  NOT_PROCEED(ReportedAdjudicationStatus.NOT_PROCEED),

  PROSECUTION(ReportedAdjudicationStatus.PROSECUTION),
  SCHEDULE_HEARING(ReportedAdjudicationStatus.SCHEDULED);

  open fun nextStates(): List<OutcomeCode> = listOf()

  fun canTransitionTo(to: OutcomeCode): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }
}

enum class NotProceedReason {
  ANOTHER_WAY, RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, OTHER
}
