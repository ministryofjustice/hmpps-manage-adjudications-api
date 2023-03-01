package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "dis_issue_history")
data class DisIssueHistory(
  override val id: Long? = null,
  var reportedAdjudicationId: Long,
  @field:Length(max = 32)
  var issuingOfficer: String,
  var dateTimeOfIssue: LocalDateTime
) : BaseEntity()
