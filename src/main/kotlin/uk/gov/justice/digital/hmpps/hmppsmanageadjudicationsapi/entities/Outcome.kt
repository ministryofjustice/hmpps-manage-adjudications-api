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
  REFER_POLICE(ReportedAdjudicationStatus.REFER_POLICE),
  REFER_INAD(ReportedAdjudicationStatus.REFER_INAD),
  NOT_PROCEED(ReportedAdjudicationStatus.NOT_PROCEED),
  SCHEDULE_HEARING(ReportedAdjudicationStatus.SCHEDULE_HEARING),
  PROSECUTION(ReportedAdjudicationStatus.PROSECUTION),
}

enum class NotProceedReason {
  RELEASED, WITNESS_RELEASED, WITNESS_NOT_ATTEND, UNFIT, FLAWED, EXPIRED_NOTICE, EXPIRED_HEARING, NOT_FAIR, PROSECUTED, OTHER
}
