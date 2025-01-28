package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.integration

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDateTime
import java.util.UUID

data class AdjudicationIntTestDataSet(
  var chargeNumber: String? = null,
  val prisonerNumber: String,
  val offenderBookingId: Long,
  val gender: Gender = Gender.MALE,
  val agencyId: String,
  val locationId: Long,
  val locationUuid: UUID? = null,
  val dateTimeOfIncident: LocalDateTime,
  val dateTimeOfIncidentISOString: String,
  var dateTimeOfDiscovery: LocalDateTime? = null,
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
  var protectedCharacteristics: List<Characteristic>? = null,
)

data class OffenceTestDataSet(
  val offenceCode: Int,
  val paragraphNumber: String,
  val paragraphDescription: String,
  val victimPrisonersNumber: String? = null,
  val victimStaffUsername: String? = null,
  val victimOtherPersonsName: String? = null,
  var protectedCharacteristics: List<Characteristic>? = null,
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
