package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.RehabilitativeActivityDto
import java.time.LocalDate

@Entity
@Table(name = "rehabilitative_activity")
data class RehabilitativeActivity(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var details: String? = null,
  @field:Length(max = 32)
  var monitor: String? = null,
  var endDate: LocalDate? = null,
  var totalSessions: Int? = null,
  var completed: Boolean? = null,
) : BaseEntity() {
  fun toDto(): RehabilitativeActivityDto = RehabilitativeActivityDto(
    id = this.id,
    details = this.details,
    monitor = this.monitor,
    endDate = this.endDate,
    totalSessions = this.totalSessions,
    completed = this.completed,
  )
}
