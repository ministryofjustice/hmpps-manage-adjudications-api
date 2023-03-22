package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

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
