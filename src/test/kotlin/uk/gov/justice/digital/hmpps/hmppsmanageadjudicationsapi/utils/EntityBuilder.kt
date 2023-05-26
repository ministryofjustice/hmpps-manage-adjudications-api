package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepositoryTest
import java.time.LocalDateTime
import java.util.*

const val ADJUDICATION_NUMBER = "abcd"

class EntityBuilder {

  fun reportedAdjudication(
    reportNumber: String = ADJUDICATION_NUMBER,
    dateTime: LocalDateTime = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
    agencyId: String = "MDI",
    hearingId: Long? = 1L,
  ): ReportedAdjudication {
    return ReportedAdjudication(
      reportNumber = reportNumber,
      bookingId = 234L,
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = agencyId,
      locationId = 2,
      dateTimeOfIncident = dateTime,
      dateTimeOfDiscovery = dateTime.plusDays(1),
      handoverDeadline = dateTime.plusDays(2),
      isYouthOffender = false,
      incidentRoleCode = "25a",
      incidentRoleAssociatedPrisonersNumber = "B23456",
      incidentRoleAssociatedPrisonersName = "Associated Prisoner",
      offenceDetails = mutableListOf(
        ReportedOffence(
          // offence with all data set
          offenceCode = 3,
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "A Person",
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
      draftCreatedOn = dateTime,
      hearings = mutableListOf(
        createHearing(reportNumber, dateTime, agencyId, hearingId),
      ),
      outcomes = mutableListOf(),
      disIssueHistory = mutableListOf(),
      punishments = mutableListOf(),
      punishmentComments = mutableListOf(),
    )
  }

  fun createHearing(
    reportNumber: String = ADJUDICATION_NUMBER,
    dateTime: LocalDateTime = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
    agencyId: String = "MDI",
    hearingId: Long? = 1L,
  ) = Hearing(
    id = hearingId,
    locationId = 1L,
    dateTimeOfHearing = dateTime.plusWeeks(1),
    agencyId = agencyId,
    reportNumber = reportNumber,
    oicHearingId = "3",
    oicHearingType = OicHearingType.GOV,
  )
}
