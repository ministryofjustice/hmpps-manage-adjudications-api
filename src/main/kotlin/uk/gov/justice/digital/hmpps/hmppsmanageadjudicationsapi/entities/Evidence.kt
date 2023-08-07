package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

enum class EvidenceCode {
  PHOTO,
  BODY_WORN_CAMERA,
  CCTV,
  BAGGED_AND_TAGGED,
  OTHER,
}

@Entity
@Table(name = "evidence")
data class Evidence(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: EvidenceCode,
  @field:Length(max = 32)
  var identifier: String? = null,
  @field:Length(max = 4000)
  var details: String,
  @field:Length(max = 32)
  var reporter: String,
) : BaseEntity()

@Entity
@Table(name = "reported_evidence")
data class ReportedEvidence(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var code: EvidenceCode,
  @field:Length(max = 32)
  var identifier: String? = null,
  @field:Length(max = 4000)
  var details: String,
  @field:Length(max = 32)
  var reporter: String,
) : BaseEntity()
