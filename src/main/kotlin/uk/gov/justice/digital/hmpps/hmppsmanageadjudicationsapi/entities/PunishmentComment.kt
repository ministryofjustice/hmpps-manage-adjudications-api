package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length

@Entity
@Table(name = "punishment_comments")
data class PunishmentComment(
  override val id: Long? = null,
  @field:Length(max = 4000)
  var comment: String,
  @Enumerated(EnumType.STRING)
  var reasonForChange: ReasonForChange? = null,
  val nomisCreatedBy: String? = null,
) : BaseEntity()

enum class ReasonForChange {
  APPEAL, CORRECTION, OTHER, GOV_OR_DIRECTOR
}
