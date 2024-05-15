package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import java.time.LocalDate

@Entity
@Table(name = "rehabilitative_activity")
data class RehabilitativeActivity(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String,
  @field:Length(max = 32)
  var monitor: String,
  var endDate: LocalDate,
  var totalSessions: Int? = null,
  var completed: Boolean? = null,
  @Enumerated(EnumType.STRING)
  var outcome: RehabilitativeActivityOutcome? = null,
) : BaseEntity()

enum class RehabilitativeActivityOutcome {
  ACTIVATED,
  PARTIAL_ACTIVATED,
  EXTENDED,
  NO_ACTION,
}
