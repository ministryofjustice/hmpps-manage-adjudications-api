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
  var activatedBy: Long? = null,
  var activatedFrom: Long? = null,
  var suspendedUntil: LocalDate? = null,
  var amount: Double? = null,
  var sanctionSeq: Long? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_fk_id")
  var schedule: MutableList<PunishmentSchedule>,
  var deleted: Boolean? = null,
  var consecutiveReportNumber: Long? = null,
) : BaseEntity()

enum class PunishmentType {
  PRIVILEGE, EARNINGS, CONFINEMENT, REMOVAL_ACTIVITY, EXCLUSION_WORK, EXTRA_WORK, REMOVAL_WING, ADDITIONAL_DAYS, PROSPECTIVE_DAYS, CAUTION, DAMAGES_OWED,
}
enum class PrivilegeType {
  CANTEEN, FACILITIES, MONEY, TV, ASSOCIATION, OTHER
}
