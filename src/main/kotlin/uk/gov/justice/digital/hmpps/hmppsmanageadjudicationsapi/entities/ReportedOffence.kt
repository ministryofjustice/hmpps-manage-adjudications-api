package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "reported_offence")
data class ReportedOffence(
  override val id: Long? = null,
  var offenceCode: Int,
  @field:Length(max = 7)
  var victimPrisonersNumber: String? = null,
  @field:Length(max = 30)
  var victimStaffUsername: String? = null,
  @field:Length(max = 100)
  var victimOtherPersonsName: String? = null,
  var nomisOffenceCode: String? = null,
  @field:Length(max = 350)
  var nomisOffenceDescription: String? = null,
  var actualOffenceCode: Int? = null,
) : BaseEntity()
