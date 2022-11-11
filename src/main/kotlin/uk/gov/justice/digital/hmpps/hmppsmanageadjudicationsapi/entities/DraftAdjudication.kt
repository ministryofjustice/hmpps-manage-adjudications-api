package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.Pronouns
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

@Entity
@Table(name = "draft_adjudications")
data class DraftAdjudication(
  override val id: Long? = null,
  val prisonerNumber: String,
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  val gender: Gender,
  val reportNumber: Long? = null,
  val reportByUserId: String? = null,
  var isYouthOffender: Boolean? = null,
  val agencyId: String,
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
  val pronouns: List<Pronouns>
) {
  MALE(Pronouns.male()),
  FEMALE(Pronouns.female());
}
