package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDateTime

data class AdjudicationIntTestDataSet(
  val chargeNumber: String,
  val prisonerNumber: String,
  val gender: Gender = Gender.MALE,
  val agencyId: String,
  val locationId: Long,
  val dateTimeOfIncident: LocalDateTime,
  val dateTimeOfIncidentISOString: String,
  val dateTimeOfDiscovery: LocalDateTime? = null,
  val dateTimeOfDiscoveryISOString: String? = null,
  val handoverDeadlineISOString: String,
  val isYouthOffender: Boolean,
  val incidentRoleCode: String,
  val incidentRoleParagraphNumber: String,
  val incidentRoleParagraphDescription: String,
  val incidentRoleAssociatedPrisonersNumber: String,
  val offence: OffenceTestDataSet,
  val statement: String,
  val createdByUserId: String,
  val damages: List<DamagesTestDataSet>,
  val evidence: List<EvidenceTestDataSet>,
  val witnesses: List<WitnessTestDataSet>,
  val dateTimeOfHearing: LocalDateTime? = null,
  val dateTimeOfHearingISOString: String? = null,
  var overrideAgencyId: String? = null,
)

data class OffenceTestDataSet(
  val offenceCode: Int,
  val paragraphNumber: String,
  val paragraphDescription: String,
  val victimPrisonersNumber: String? = null,
  val victimStaffUsername: String? = null,
  val victimOtherPersonsName: String? = null,
)

data class DamagesTestDataSet(
  val code: DamageCode,
  val details: String,
)

data class EvidenceTestDataSet(
  val code: EvidenceCode,
  val details: String,
)

data class WitnessTestDataSet(
  val code: WitnessCode,
  val firstName: String,
  val lastName: String,
)

data class NomisOffenceTestDataSet(
  val nomisCodes: List<String>,
  val victimStaffUsernames: List<String>,
  val victimPrisonersNumbers: List<String>,
)
