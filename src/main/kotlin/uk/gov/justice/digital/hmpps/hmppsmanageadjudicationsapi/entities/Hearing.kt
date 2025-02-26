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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.HearingDto
import java.time.LocalDateTime
import java.util.*

enum class OicHearingType {
  GOV_ADULT,
  GOV_YOI,
  INAD_YOI,
  GOV,
  INAD_ADULT,
  ;

  fun isValidState(isYoungOffender: Boolean) {
    when (isYoungOffender) {
      true -> if (listOf(GOV_ADULT, INAD_ADULT).contains(this)) throw IllegalStateException("oic hearing type is not applicable for rule set")
      false -> if (listOf(GOV_YOI, INAD_YOI).contains(this)) throw IllegalStateException("oic hearing type is not applicable for rule set")
    }
  }

  companion object {
    fun inadTypes() = listOf(INAD_ADULT, INAD_YOI)
    fun govTypes() = listOf(GOV_ADULT, GOV_YOI)
  }
}

@Entity
@Table(name = "hearing")
data class Hearing(
  override val id: Long? = null,
  var locationId: Long,
  var locationUuid: UUID? = null,
  var dateTimeOfHearing: LocalDateTime,
  @field:Length(max = 6)
  var agencyId: String,
  @field:Length(max = 16)
  var chargeNumber: String,
  var oicHearingId: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var oicHearingType: OicHearingType,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "outcome_id")
  var hearingOutcome: HearingOutcome? = null,
  @field:Length(max = 240)
  var representative: String? = null,
) : BaseEntity() {
  fun toDto(): HearingDto = HearingDto(
      id = this.id,
      locationId = this.locationId,
      locationUuid = this.locationUuid,
      dateTimeOfHearing = this.dateTimeOfHearing,
      oicHearingType = this.oicHearingType,
      outcome = this.hearingOutcome?.toDto(),
      agencyId = this.agencyId,
    )
}
