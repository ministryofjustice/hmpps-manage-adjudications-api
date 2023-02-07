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
import javax.persistence.OneToOne
import javax.persistence.Table
import javax.validation.ValidationException

@Entity
@Table(name = "reported_adjudications")
data class ReportedAdjudication(
  override val id: Long? = null,
  var prisonerNumber: String,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var gender: Gender,
  var bookingId: Long,
  var reportNumber: Long,
  var agencyId: String,
  var locationId: Long,
  var dateTimeOfIncident: LocalDateTime,
  var dateTimeOfDiscovery: LocalDateTime,
  var handoverDeadline: LocalDateTime,
  var isYouthOffender: Boolean,
  var incidentRoleCode: String?,
  var incidentRoleAssociatedPrisonersNumber: String?,
  var incidentRoleAssociatedPrisonersName: String?,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var status: ReportedAdjudicationStatus,
  @field:Length(max = 128)
  var statusReason: String? = null,
  @field:Length(max = 4000)
  var statusDetails: String? = null,
  var statement: String,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var offenceDetails: MutableList<ReportedOffence>,
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
  var hearings: MutableList<Hearing>,
  var draftCreatedOn: LocalDateTime,
  var issuingOfficer: String? = null,
  var dateTimeOfIssue: LocalDateTime? = null,
  var dateTimeOfFirstHearing: LocalDateTime? = null,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "outcome_id")
  var outcome: Outcome? = null,

) :
  BaseEntity() {
  fun transition(to: ReportedAdjudicationStatus, reason: String? = null, details: String? = null, reviewUserId: String? = null) {

    if (this.status.canTransitionTo(to)) {
      this.status = to
      this.statusReason = reason
      this.statusDetails = details
      this.reviewUserId = reviewUserId
    } else {
      throw IllegalStateException("ReportedAdjudication ${this.reportNumber} cannot transition from ${this.status} to $to")
    }
  }
}

enum class ReportedAdjudicationStatus {
  @Deprecated("this is no longer used and remains for historic purposes")
  ACCEPTED,
  REJECTED,
  AWAITING_REVIEW {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(UNSCHEDULED, REJECTED, RETURNED, AWAITING_REVIEW)
    }
  },
  RETURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(AWAITING_REVIEW)
    }
  },
  UNSCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(SCHEDULED, REFER_POLICE, NOT_PROCEED)
    }
  },
  SCHEDULED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(UNSCHEDULED, REFER_POLICE)
    }
  },
  REFER_POLICE, // question: can the police refer it back?
  NOT_PROCEED;
  open fun nextStates(): List<ReportedAdjudicationStatus> = listOf()
  fun canTransitionFrom(from: ReportedAdjudicationStatus): Boolean {
    val to = this
    return from.nextStates().contains(to)
  }
  fun canTransitionTo(to: ReportedAdjudicationStatus): Boolean {
    val from = this
    return from.nextStates().contains(to)
  }
  fun isAccepted(): Boolean {
    return this == UNSCHEDULED
  }

  fun canBeIssuedValidation() {
    if (issuableStatuses().none { it == this }) throw ValidationException("$this not valid status for DIS issue")
  }

  companion object {
    fun issuableStatuses() = listOf(SCHEDULED, UNSCHEDULED)
    fun issuableStatusesForPrint() = listOf(SCHEDULED)

    fun ReportedAdjudicationStatus.validateTransition(vararg next: ReportedAdjudicationStatus) {
      next.toList().forEach {
        if (this != it && !this.canTransitionTo(it)) throw ValidationException("Invalid status transition ${this.name} - ${it.name}")
      }
    }
  }
}
