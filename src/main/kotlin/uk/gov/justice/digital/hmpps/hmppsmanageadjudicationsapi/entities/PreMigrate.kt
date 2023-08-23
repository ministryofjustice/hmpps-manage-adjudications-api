package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

@Entity
@Table(name = "hearing_pre_migrate")
data class HearingPreMigrate(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  var locationId: Long,
  var dateTimeOfHearing: LocalDateTime,
  @Column(nullable = false)
  var oicHearingType: OicHearingType,
)

@Entity
@Table(name = "hearing_outcome_pre_migrate")
data class HearingOutcomePreMigrate(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @field:Length(max = 32)
  var adjudicator: String,
  @Enumerated(EnumType.STRING)
  var code: HearingOutcomeCode,
  @Enumerated(EnumType.STRING)
  var plea: HearingOutcomePlea? = null,
)

@Entity
@Table(name = "punishment_pre_migrate")
data class PunishmentPreMigrate(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,
  @Enumerated(EnumType.STRING)
  var type: PunishmentType,
  @Enumerated(EnumType.STRING)
  var privilegeType: PrivilegeType? = null,
  @field:Length(max = 32)
  var otherPrivilege: String? = null,
  var stoppagePercentage: Int? = null,
  @field:Length(max = 16)
  var consecutiveChargeNumber: String? = null,
)
