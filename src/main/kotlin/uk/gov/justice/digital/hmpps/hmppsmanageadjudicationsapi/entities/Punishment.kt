package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import java.time.LocalDate

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
  @field:Length(max = 16)
  var activatedFromChargeNumber: String? = null,
  var suspendedUntil: LocalDate? = null,
  var amount: Double? = null,
  var sanctionSeq: Long? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_fk_id")
  var schedule: MutableList<PunishmentSchedule>,
  var deleted: Boolean? = null,
  @field:Length(max = 16)
  var consecutiveChargeNumber: String? = null,
  var migrated: Boolean = false,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_pre_migrate_id")
  var punishmentPreMigrate: PunishmentPreMigrate? = null,
  @field:Length(max = 32)
  var nomisStatus: String? = null,

) : BaseEntity()

enum class PunishmentType {
  PRIVILEGE, EARNINGS, CONFINEMENT, REMOVAL_ACTIVITY, EXCLUSION_WORK, EXTRA_WORK, REMOVAL_WING, ADDITIONAL_DAYS, PROSPECTIVE_DAYS, CAUTION, DAMAGES_OWED;

  companion object {
    fun additionalDays() = listOf(ADDITIONAL_DAYS, PROSPECTIVE_DAYS)
    fun damagesAndCaution() = listOf(CAUTION, DAMAGES_OWED)
  }
}
enum class PrivilegeType {
  CANTEEN, FACILITIES, MONEY, TV, ASSOCIATION, OTHER
}
