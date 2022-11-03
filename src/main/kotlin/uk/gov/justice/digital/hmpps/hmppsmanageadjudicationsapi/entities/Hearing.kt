package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "hearing")
data class Hearing(
  override val id: Long? = null,
  var locationId: Long,
  var dateTimeOfHearing: LocalDateTime,
  @field:Length(max = 6)
  var agencyId: String,
  var reportNumber: Long,
  var oicHearingId: Long,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var oicHearingType: OicHearingType,
) : BaseEntity()
