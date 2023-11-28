package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "reported_offence")
data class ReportedOffence(
  override val id: Long? = null,
  var offenceCode: Int,
  var victimPrisonersNumber: String? = null,
  var victimStaffUsername: String? = null,
  var victimOtherPersonsName: String? = null,
  var nomisOffenceCode: String? = null,
  @field:Length(max = 350)
  var nomisOffenceDescription: String? = null,
  var actualOffenceCode: Int? = null,
) : BaseEntity()
