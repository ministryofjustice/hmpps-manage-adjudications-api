package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentScheduleDto
import java.time.LocalDate

@Entity
@Table(name = "punishment_schedule")
data class PunishmentSchedule(
  override val id: Long? = null,
  var days: Int,
  var startDate: LocalDate? = null,
  var endDate: LocalDate? = null,
  var suspendedUntil: LocalDate? = null,
) : BaseEntity() {
  fun toDto(): PunishmentScheduleDto =
    PunishmentScheduleDto(
      days = this.days,
      startDate = this.startDate,
      endDate = this.endDate,
      suspendedUntil = this.suspendedUntil,
    )
}
