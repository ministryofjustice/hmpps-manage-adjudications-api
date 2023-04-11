package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "punishment_schedule")
data class PunishmentSchedule(
  override val id: Long? = null,
  var days: Int,
  var startDate: LocalDate? = null,
  var endDate: LocalDate? = null,
  var suspendedUntil: LocalDate? = null,
) : BaseEntity()
