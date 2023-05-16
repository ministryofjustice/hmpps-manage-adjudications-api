package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.ValidationException
import org.hibernate.validator.constraints.Length
import org.jetbrains.annotations.TestOnly
import java.lang.IllegalStateException
import java.time.LocalDateTime

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
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var disIssueHistory: MutableList<DisIssueHistory>,
  var dateTimeOfFirstHearing: LocalDateTime? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  private var outcomes: MutableList<Outcome>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  private var punishments: MutableList<Punishment>,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "reported_adjudication_fk_id")
  var punishmentComments: MutableList<PunishmentComment>,
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

  fun calculateStatus() {
    this.status = when (this.getOutcomes().isEmpty()) {
      true ->
        when (this.hearings.isEmpty()) {
          true -> ReportedAdjudicationStatus.UNSCHEDULED
          false -> {
            if (this.getLatestHearing().isAdjourn()) {
              ReportedAdjudicationStatus.ADJOURNED
            } else {
              ReportedAdjudicationStatus.SCHEDULED
            }
          }
        }
      false -> {
        if (this.getLatestHearing().isAdjourn()) {
          ReportedAdjudicationStatus.ADJOURNED
        } else {
          this.getOutcomes().sortedByDescending { it.createDateTime }.first().code.status
        }
      }
    }
  }

  fun getLatestHearing(): Hearing? = this.hearings.maxByOrNull { it.dateTimeOfHearing }

  fun addOutcome(outcome: Outcome) = this.outcomes.add(outcome)

  fun getOutcomes() = this.outcomes.filter { it.deleted != true }

  fun getOutcomeToRemove() = this.getOutcomes().getOutcomeToRemove()

  @TestOnly
  fun clearOutcomes() = this.outcomes.clear()

  fun getPunishments() = this.punishments.filter { it.deleted != true }

  fun addPunishment(punishment: Punishment) = this.punishments.add(punishment)

  fun addPunishmentComment(punishmentComment: PunishmentComment) = this.punishmentComments.add(punishmentComment)

  fun clearPunishments() = this.punishments.clear()

  private fun Hearing?.isAdjourn() = this?.hearingOutcome?.code == HearingOutcomeCode.ADJOURN

  companion object {
    fun List<Outcome>.getOutcomeToRemove() = this.maxBy { it.createDateTime!! }
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
      return listOf(UNSCHEDULED, REFER_POLICE, REFER_INAD, DISMISSED, NOT_PROCEED, CHARGE_PROVED, ADJOURNED)
    }
  },
  REFER_POLICE {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(PROSECUTION, NOT_PROCEED, SCHEDULED)
    }
  },
  REFER_INAD {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(NOT_PROCEED, SCHEDULED)
    }
  },
  PROSECUTION,
  DISMISSED,
  NOT_PROCEED,
  ADJOURNED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(SCHEDULED)
    }
  },
  CHARGE_PROVED {
    override fun nextStates(): List<ReportedAdjudicationStatus> {
      return listOf(QUASHED)
    }
  },
  QUASHED,
  ;
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
