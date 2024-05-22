package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.PunishmentDto
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "punishment")
data class Punishment(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  var type: PunishmentType,
  @Enumerated(EnumType.STRING)
  var privilegeType: PrivilegeType? = null,
  @field:Length(max = 32)
  var otherPrivilege: String? = null,
  var stoppagePercentage: Int? = null,
  @field:Length(max = 16)
  var activatedByChargeNumber: String? = null,
  private var suspendedUntil: LocalDate? = null,
  var amount: Double? = null,
  var sanctionSeq: Long? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_fk_id")
  private var schedule: MutableList<PunishmentSchedule>,
  var deleted: Boolean? = null,
  @field:Length(max = 16)
  var consecutiveToChargeNumber: String? = null,
  @field:Length(max = 32)
  var nomisStatus: String? = null,
  var actualCreatedDate: LocalDateTime? = null,
  @field:Length(max = 4000)
  var paybackNotes: String? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "punishment_fk_id")
  var rehabilitativeActivities: MutableList<RehabilitativeActivity> = mutableListOf(),
  var rehabCompleted: Boolean? = null,
  @Enumerated(EnumType.STRING)
  var rehabNotCompletedOutcome: NotCompletedOutcome? = null,
) : BaseEntity() {
  fun isActiveSuspended(punishmentCutOff: LocalDate): Boolean =
    this.suspendedUntil?.isAfter(punishmentCutOff) == true && this.activatedByChargeNumber == null

  fun isActivePunishment(punishmentCutOff: LocalDate): Boolean =
    PunishmentType.damagesAndCaution().none { it == this.type } && this.suspendedUntil == null && (
      this.latestSchedule().endDate?.isAfter(punishmentCutOff) == true || PunishmentType.additionalDays().contains(this.type)
      )

  fun isActive(): Boolean =
    this.suspendedUntil == null && this.latestSchedule().endDate?.isAfter(LocalDate.now().minusDays(1)) == true

  fun latestSchedule() = this.schedule.maxBy { it.createDateTime!! }

  fun addSchedule(schedule: PunishmentSchedule) {
    this.suspendedUntil = schedule.suspendedUntil
    this.schedule.add(schedule)
  }

  fun removeSchedule(schedule: PunishmentSchedule) {
    this.schedule.remove(schedule)
    this.suspendedUntil = latestSchedule().suspendedUntil
  }

  fun getSchedule(): List<PunishmentSchedule> = this.schedule

  fun getSuspendedUntil(): LocalDate? = this.suspendedUntil

  fun isCorrupted(): Boolean =
    this.suspendedUntil != null && this.actualCreatedDate?.toLocalDate()?.isEqual(this.suspendedUntil) == true && this.actualCreatedDate?.toLocalDate()?.isAfter(LocalDate.now().minusMonths(6)) == true

  fun toDto(hasLinkedAda: Boolean, consecutiveReportsAvailable: List<String>?, activatedFrom: String? = null): PunishmentDto {
    val canRemove = !(PunishmentType.additionalDays().contains(this.type) && hasLinkedAda) && this.rehabCompleted == null
    return PunishmentDto(
      id = this.id,
      type = this.type,
      privilegeType = this.privilegeType,
      otherPrivilege = this.otherPrivilege,
      stoppagePercentage = this.stoppagePercentage,
      damagesOwedAmount = this.amount,
      activatedFrom = activatedFrom,
      activatedBy = this.activatedByChargeNumber,
      consecutiveChargeNumber = this.consecutiveToChargeNumber,
      canRemove = canRemove,
      canEdit = this.rehabilitativeActivities.isEmpty() && canRemove,
      consecutiveReportAvailable = isConsecutiveReportAvailable(this.consecutiveToChargeNumber, consecutiveReportsAvailable),
      schedule = this.schedule.maxBy { latest -> latest.createDateTime ?: LocalDateTime.now() }.toDto(),
      paybackNotes = this.paybackNotes,
      rehabilitativeActivities = this.rehabilitativeActivities.map { it.toDto() },
      rehabilitativeActivitiesCompleted = this.rehabCompleted,
      rehabilitativeActivitiesNotCompletedOutcome = this.rehabNotCompletedOutcome,
    )
  }

  private fun isConsecutiveReportAvailable(consecutiveChargeNumber: String?, consecutiveReportsAvailable: List<String>?): Boolean? {
    consecutiveChargeNumber ?: return null
    consecutiveReportsAvailable ?: return null
    return consecutiveReportsAvailable.any { it == consecutiveChargeNumber }
  }
}

enum class PunishmentType(val measurement: Measurement = Measurement.DAYS, val rehabilitativeActivitiesAllowed: Boolean = true) {
  PRIVILEGE,
  EARNINGS,
  CONFINEMENT,
  REMOVAL_ACTIVITY,
  EXCLUSION_WORK,
  EXTRA_WORK,
  REMOVAL_WING,
  ADDITIONAL_DAYS(rehabilitativeActivitiesAllowed = false),
  PROSPECTIVE_DAYS(rehabilitativeActivitiesAllowed = false),
  CAUTION(rehabilitativeActivitiesAllowed = false),
  DAMAGES_OWED(rehabilitativeActivitiesAllowed = false),
  PAYBACK(measurement = Measurement.HOURS, rehabilitativeActivitiesAllowed = false),
  ;

  companion object {
    fun additionalDays() = listOf(ADDITIONAL_DAYS, PROSPECTIVE_DAYS)
    fun damagesAndCaution() = listOf(CAUTION, DAMAGES_OWED)
  }
}
enum class PrivilegeType {
  CANTEEN,
  FACILITIES,
  MONEY,
  TV,
  ASSOCIATION,
  GYM,
  OTHER,
}
