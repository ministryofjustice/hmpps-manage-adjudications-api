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
  @OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "punishment_fk_id")
  var schedule: MutableList<PunishmentSchedule>,
) : BaseEntity()

enum class PunishmentType {
  PRIVILEGE, EARNINGS, CONFINEMENT, REMOVAL_ACTIVITY, EXCLUSION_WORK, EXTRA_WORK, REMOVAL_WING, ADDITIONAL_DAYS, PROSPECTIVE_DAYS
}
enum class PrivilegeType {
  CANTEEN, FACILITIES, MONEY, TV, ASSOCIATION, OTHER
}
