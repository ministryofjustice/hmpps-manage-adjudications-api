package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Length
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.DisIssueHistoryDto
import java.time.LocalDateTime

@Entity
@Table(name = "dis_issue_history")
data class DisIssueHistory(
  override val id: Long? = null,
  @field:Length(max = 32)
  var issuingOfficer: String,
  var dateTimeOfIssue: LocalDateTime,
) : BaseEntity() {
  fun toDto(): DisIssueHistoryDto = DisIssueHistoryDto(
      issuingOfficer = this.issuingOfficer,
      dateTimeOfIssue = this.dateTimeOfIssue,
    )
}
