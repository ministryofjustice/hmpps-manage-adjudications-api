package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime

@Entity
@Table(name = "hearing")
data class Hearing(
  override val id: Long? = null,
  var locationId: Long,
  var dateTimeOfHearing: LocalDateTime,
  @field:Length(max = 6)
  var agencyId: String,
  var chargeNumber: String,
  var oicHearingId: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var oicHearingType: OicHearingType,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "outcome_id")
  var hearingOutcome: HearingOutcome? = null,
) : BaseEntity()
