package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import org.hibernate.validator.constraints.Length
import java.lang.IllegalStateException
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "reported_adjudications")
data class ReportedAdjudication(
  override val id: Long? = null,
  var prisonerNumber: String,
  var bookingId: Long,
  var reportNumber: Long,
  var agencyId: String,
  var locationId: Long,
  var dateTimeOfIncident: LocalDateTime,
  var handoverDeadline: LocalDateTime,
  var incidentRoleCode: String?,
  var incidentRoleAssociatedPrisonersNumber: String?,
  @Length(max = 4000)
  var statement: String,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var offenceDetails: MutableList<ReportedOffence>? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: ReportedAdjudicationStatus,
  @Length(max = 64)
  var statusReason: String? = null,
  @Length(max = 1000)
  var statusDetails: String? = null,
) : BaseEntity() {
  fun transition(to: ReportedAdjudicationStatus, statusReason: String? = null, statusDetails: String? = null) {
    if (this.status.canTransitionTo(to)) {
      this.status = to
      this.statusReason = statusReason
      this.statusDetails = statusDetails
    } else {
      throw IllegalStateException("ReportedAdjudication ${this.reportNumber} cannot transition from ${this.status} to $status")
    }
  }
}

enum class ReportedAdjudicationStatus {
  ACCEPTED,
  REJECTED,
  AWAITING_REVIEW {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(ACCEPTED, REJECTED, RETURNED, AWAITING_REVIEW)
    }
  },
  RETURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(AWAITING_REVIEW)
    }
  };
  open fun nextStates(): List<ReportedAdjudicationStatus> = listOf()
  fun canTransitionFrom(from: ReportedAdjudicationStatus): Boolean {
    val to = this
    return from.nextStates().contains(to)
  }
  fun canTransitionTo(to: ReportedAdjudicationStatus): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }
}
