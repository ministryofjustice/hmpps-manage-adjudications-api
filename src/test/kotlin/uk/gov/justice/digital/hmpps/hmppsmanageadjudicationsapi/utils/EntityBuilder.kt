package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepositoryTest
import java.time.LocalDateTime
import java.util.*

class EntityBuilder {

  fun reportedAdjudication(
    id: Long? = null,
    chargeNumber: String = "1235",
    dateTime: LocalDateTime = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
    agencyId: String = "MDI",
    hearingId: Long? = 1L,
    prisonerNumber: String = "A12345",
    offenderBookingId: Long? = null,
  ): ReportedAdjudication {
    return ReportedAdjudication(
      id = id,
      chargeNumber = chargeNumber,
      prisonerNumber = prisonerNumber,
      offenderBookingId = offenderBookingId,
      gender = Gender.MALE,
      originatingAgencyId = agencyId,
      locationId = 2,
      locationUuid = UUID.fromString("0194ac90-2def-7c63-9f46-b3ccc911fdff"),
      dateTimeOfIncident = dateTime,
      dateTimeOfDiscovery = dateTime.plusDays(1),
      handoverDeadline = dateTime.plusDays(2),
      isYouthOffender = false,
      incidentRoleCode = "25a",
      incidentRoleAssociatedPrisonersNumber = "B23456",
      incidentRoleAssociatedPrisonersName = "Associated Prisoner",
      offenceDetails = mutableListOf(
        ReportedOffence(
          offenceCode = 1002,
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "A Person",
          protectedCharacteristics = mutableListOf(),
        ),
      ),
      statement = "Example statement",
      status = ReportedAdjudicationStatus.AWAITING_REVIEW,
      damages = mutableListOf(
        ReportedDamage(
          code = DamageCode.CLEANING,
          details = "details",
          reporter = "Fred",
        ),
      ),
      evidence = mutableListOf(
        ReportedEvidence(
          code = EvidenceCode.PHOTO,
          details = "details",
          reporter = "Fred",
          identifier = "identifier",
        ),
      ),
      witnesses = mutableListOf(
        ReportedWitness(
          code = WitnessCode.OFFICER,
          firstName = "prison",
          lastName = "officer",
          reporter = "Fred",
        ),
      ),
      hearings = mutableListOf(
        createHearing(chargeNumber, dateTime, agencyId, hearingId),
      ),
      outcomes = mutableListOf(),
      disIssueHistory = mutableListOf(),
      punishments = mutableListOf(),
      punishmentComments = mutableListOf(),
      createdOnBehalfOfOfficer = "officer",
      createdOnBehalfOfReason = "some reason",
    ).also {
      it.createDateTime = LocalDateTime.now()
      it.createdByUserId = ""
    }
  }

  fun createHearing(
    chargeNumber: String = "1235",
    dateTime: LocalDateTime = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
    agencyId: String = "MDI",
    hearingId: Long? = 1L,
  ) = Hearing(
    id = hearingId,
    locationId = 1L,
    locationUuid = UUID.fromString("0194ac91-6646-7bdb-b8f8-584844283a3a"),
    dateTimeOfHearing = dateTime.plusWeeks(1),
    agencyId = agencyId,
    chargeNumber = chargeNumber,
    oicHearingId = 3L,
    oicHearingType = OicHearingType.GOV,
  )
}
