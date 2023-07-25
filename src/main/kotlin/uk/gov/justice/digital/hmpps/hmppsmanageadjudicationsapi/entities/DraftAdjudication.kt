package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.Pronouns

@Entity
@Table(name = "draft_adjudications")
data class DraftAdjudication(
  override val id: Long? = null,
  val prisonerNumber: String,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  var gender: Gender,
  val chargeNumber: String? = null,
  val reportByUserId: String? = null,
  var isYouthOffender: Boolean? = null,
  @Column(name = "originating_agency_id")
  val agencyId: String,
  val overrideAgencyId: String? = null,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_details_id")
  val incidentDetails: IncidentDetails,
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_role_id")
  var incidentRole: IncidentRole? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_adjudication_fk_id")
  var offenceDetails: MutableList<Offence> = mutableListOf(),
  @OneToOne(optional = true, cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  @JoinColumn(name = "incident_statement_id")
  var incidentStatement: IncidentStatement? = null,
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_adjudication_fk_id")
  var damages: MutableList<Damage> = mutableListOf(),
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_adjudication_fk_id")
  var evidence: MutableList<Evidence> = mutableListOf(),
  @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "draft_adjudication_fk_id")
  var witnesses: MutableList<Witness> = mutableListOf(),
  var damagesSaved: Boolean? = null,
  var evidenceSaved: Boolean? = null,
  var witnessesSaved: Boolean? = null,
) : BaseEntity()

enum class Gender(
  val pronouns: List<Pronouns>,
) {
  MALE(Pronouns.male()),
  FEMALE(Pronouns.female()),
  ;
}
