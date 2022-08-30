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
  var draftCreatedAt: LocalDateTime,
  var dateTimeOfIncident: LocalDateTime,
  var handoverDeadline: LocalDateTime,
  var isYouthOffender: Boolean,
  var incidentRoleCode: String?,
  var incidentRoleAssociatedPrisonersNumber: String?,
  var incidentRoleAssociatedPrisonersName: String?,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: Status,
  var statement: String,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var offenceDetails: MutableList<ReportedOffence>? = null,
  var reviewUserId: String? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var damages: MutableList<ReportedDamage>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var evidence: MutableList<ReportedEvidence>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var witnesses: MutableList<ReportedWitness>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var statusAudit: MutableList<ReportedAdjudicationStatusAudit>,
) : BaseEntity() {
  fun transition(to: ReportedAdjudicationStatusAudit, reviewUserId: String? = null) {

    if (this.status.canTransitionTo(to.status)) {
      this.statusAudit.add(to)
      this.status = to.status
      this.reviewUserId = reviewUserId
    } else {
      throw IllegalStateException("ReportedAdjudication ${this.reportNumber} cannot transition from ${this.status} to ${to.status}")
    }
  }
  fun getLatestStatus(): ReportedAdjudicationStatusAudit =
    statusAudit.sortedBy { it.createDateTime!! }.reversed().first()
}

@Entity
@Table(name = "reported_adjudication_status_audit")
data class ReportedAdjudicationStatusAudit(
  override val id: Long? = null,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: Status,
  @field:Length(max = 128)
  var statusReason: String? = null,
  @field:Length(max = 4000)
  var statusDetails: String? = null,
) : BaseEntity()

enum class Status {
  ACCEPTED,
  REJECTED,
  AWAITING_REVIEW {
    override fun nextStates(): List<Status> {
      return listOf(ACCEPTED, REJECTED, RETURNED, AWAITING_REVIEW)
    }
  },
  RETURNED {
    override fun nextStates(): List<Status> {
      return listOf(AWAITING_REVIEW)
    }
  };
  open fun nextStates(): List<Status> = listOf()
  fun canTransitionFrom(from: Status): Boolean {
    val to = this
    return from.nextStates().contains(to)
  }
  fun canTransitionTo(to: Status): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }
  fun isAccepted(): Boolean {
    return this == ACCEPTED
  }
}
