package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "submitted_adjudication_history")
data class SubmittedAdjudicationHistory(
  val adjudicationNumber: Long,
  val agencyId: String,
  var dateTimeOfIncident: LocalDateTime,
  var dateTimeSent: LocalDateTime,
) : BaseEntity()
