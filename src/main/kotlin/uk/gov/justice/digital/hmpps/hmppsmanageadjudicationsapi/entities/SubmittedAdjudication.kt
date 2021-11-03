package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "submitted_draft_adjudications")
data class SubmittedAdjudication(
  val adjudicationNumber: Long,
  val dateTimeSent: LocalDateTime
) : BaseEntity()
