package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.DraftAdjudicationRepositoryTest
import java.time.LocalDateTime

class EntityBuilder {

  fun reportedAdjudication(
    reportNumber: Long = 1235L,
    dateTime: LocalDateTime = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
    agencyId: String = "MDI"
  ): ReportedAdjudication {
    return ReportedAdjudication(
      reportNumber = reportNumber,
      bookingId = 234L,
      prisonerNumber = "A12345",
      agencyId = agencyId,
      locationId = 2,
      dateTimeOfIncident = dateTime,
      handoverDeadline = dateTime.plusDays(2),
      isYouthOffender = false,
      incidentRoleCode = "25a",
      incidentRoleAssociatedPrisonersNumber = "B23456",
      incidentRoleAssociatedPrisonersName = "Associated Prisoner",
      offenceDetails = mutableListOf(
        ReportedOffence( // offence with minimal data set
          offenceCode = 2,
          paragraphCode = "3"
        ),
        ReportedOffence(
          // offence with all data set
          offenceCode = 3,
          paragraphCode = "4",
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
          reporter = "Fred"
        )
      ),
      evidence = mutableListOf(
        ReportedEvidence(
          code = EvidenceCode.PHOTO,
          details = "details",
          reporter = "Fred"
        )
      ),
      witnesses = mutableListOf(
        ReportedWitness(
          code = WitnessCode.OFFICER,
          firstName = "prison",
          lastName = "officer",
          reporter = "Fred"
        ),
      ),
      draftCreatedOn = dateTime
    )
  }
}
