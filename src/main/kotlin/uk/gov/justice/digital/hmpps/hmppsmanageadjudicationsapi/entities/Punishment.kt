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
  var suspendedUntil: LocalDate? = null,
  var amount: Double? = null,
  var sanctionSeq: Long? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_fk_id")
  var schedule: MutableList<PunishmentSchedule>,
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
) : BaseEntity() {
  fun isActiveSuspended(punishmentCutOff: LocalDate): Boolean =
    this.suspendedUntil?.isAfter(punishmentCutOff) == true && this.activatedByChargeNumber == null

  fun toDto(hasLinkedAda: Boolean, consecutiveReportsAvailable: List<String>?, activatedFrom: String? = null): PunishmentDto =
    PunishmentDto(
      id = this.id,
      type = this.type,
      privilegeType = this.privilegeType,
      otherPrivilege = this.otherPrivilege,
      stoppagePercentage = this.stoppagePercentage,
      damagesOwedAmount = this.amount,
      activatedFrom = activatedFrom,
      activatedBy = this.activatedByChargeNumber,
      consecutiveChargeNumber = this.consecutiveToChargeNumber,
      canRemove = !(PunishmentType.additionalDays().contains(this.type) && hasLinkedAda),
      consecutiveReportAvailable = isConsecutiveReportAvailable(this.consecutiveToChargeNumber, consecutiveReportsAvailable),
      schedule = this.schedule.maxBy { latest -> latest.createDateTime ?: LocalDateTime.now() }.toDto(),
      paybackNotes = this.paybackNotes,
      rehabilitativeActivities = this.rehabilitativeActivities.map { it.toDto() },
    )

  private fun isConsecutiveReportAvailable(consecutiveChargeNumber: String?, consecutiveReportsAvailable: List<String>?): Boolean? {
    consecutiveChargeNumber ?: return null
    consecutiveReportsAvailable ?: return null
    return consecutiveReportsAvailable.any { it == consecutiveChargeNumber }
  }
}

enum class PunishmentType(val measurement: Measurement = Measurement.DAYS) {
  PRIVILEGE,
  EARNINGS,
  CONFINEMENT,
  REMOVAL_ACTIVITY,
  EXCLUSION_WORK,
  EXTRA_WORK,
  REMOVAL_WING,
  ADDITIONAL_DAYS,
  PROSPECTIVE_DAYS,
  CAUTION,
  DAMAGES_OWED,
  PAYBACK(Measurement.HOURS),
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
