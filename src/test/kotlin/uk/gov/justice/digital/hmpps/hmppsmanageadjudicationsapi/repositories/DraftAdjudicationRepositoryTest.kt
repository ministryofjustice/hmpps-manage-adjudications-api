package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.AuditConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@WithMockUser(username = "ITAG_USER")
@Import(AuditConfiguration::class, UserDetails::class)
class DraftAdjudicationRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var draftAdjudicationRepository: DraftAdjudicationRepository

  @Test
  fun `save a new draft adjudication`() {
    val draft = newDraft()

    val savedEntity = entityManager.persistAndFlush(draft)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "agencyId", "createdByUserId")
      .contains(savedEntity.id, draft.prisonerNumber, draft.agencyId, "ITAG_USER")

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
      .contains(draft.incidentDetails.locationId, draft.incidentDetails.dateTimeOfIncident, draft.incidentDetails.handoverDeadline)

    assertThat(savedEntity.incidentRole)
      .extracting("roleCode", "associatedPrisonersNumber")
      .contains(draft.incidentRole.roleCode, draft.incidentRole.associatedPrisonersNumber)
  }

  @Test
  fun `save a new draft adjudication with all data`() {
    val draft = draftWithAllData()
    val savedEntity = draftAdjudicationRepository.save(draft)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "agencyId", "createdByUserId")
      .contains(savedEntity.id, draft.prisonerNumber, draft.agencyId, "ITAG_USER")

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
      .contains(draft.incidentDetails.locationId, draft.incidentDetails.dateTimeOfIncident, draft.incidentDetails.handoverDeadline)

    assertThat(savedEntity.incidentRole)
      .extracting("roleCode", "associatedPrisonersNumber")
      .contains(draft.incidentRole.roleCode, draft.incidentRole.associatedPrisonersNumber)

    assertThat(savedEntity.offenceDetails).hasSize(2)
      .extracting("offenceCode", "paragraphNumber", "victimPrisonersNumber", "victimStaffUsername", "victimOtherPersonsName")
      .contains(
        Tuple(draft.offenceDetails!![0].offenceCode, draft.offenceDetails!![0].paragraphNumber,
          draft.offenceDetails!![0].victimPrisonersNumber, draft.offenceDetails!![0].victimStaffUsername, draft.offenceDetails!![0].victimOtherPersonsName),
        Tuple(draft.offenceDetails!![1].offenceCode, draft.offenceDetails!![1].paragraphNumber,
          draft.offenceDetails!![1].victimPrisonersNumber, draft.offenceDetails!![1].victimStaffUsername, draft.offenceDetails!![1].victimOtherPersonsName),
      )

    assertThat(savedEntity.incidentStatement)
      .extracting("statement", "completed")
      .contains(draft.incidentStatement?.statement, draft.incidentStatement?.completed)
  }

  @Test
  fun `update an existing draft adjudication with modified offences`() {
    val draftWithAllData = draftWithAllData()
    val updatedDraft = draftWithAllData.copy(
      offenceDetails = mutableListOf(
        Offence(
          offenceCode = 4,
          paragraphNumber = "2",
          victimPrisonersNumber = "B2345BB",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "Someones Name Here"
        ),
      ),
    )
    val savedEntity = draftAdjudicationRepository.save(updatedDraft)

    assertThat(savedEntity.offenceDetails).hasSize(1)
      .extracting("offenceCode", "paragraphNumber", "victimPrisonersNumber", "victimStaffUsername", "victimOtherPersonsName")
      .contains(
        Tuple(updatedDraft.offenceDetails!![0].offenceCode, updatedDraft.offenceDetails!![0].paragraphNumber,
          updatedDraft.offenceDetails!![0].victimPrisonersNumber, updatedDraft.offenceDetails!![0].victimStaffUsername,
          updatedDraft.offenceDetails!![0].victimOtherPersonsName),
      )
  }

  @Test
  fun `find draft adjudications`() {
    val dateTimeOfIncident = LocalDateTime.now()

    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12345",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 2,
          dateTimeOfIncident = dateTimeOfIncident,
          handoverDeadline = dateTimeOfIncident.plusDays(2)
        ),
        incidentRole = IncidentRole(
          roleCode = "25a",
          associatedPrisonersNumber = "B23456"
        ),
      )
    )
    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12346",
        agencyId = "LEI",
        incidentDetails = IncidentDetails(
          locationId = 2,
          dateTimeOfIncident = dateTimeOfIncident,
          handoverDeadline = dateTimeOfIncident.plusDays(2)
        ),
        incidentRole = IncidentRole(
          roleCode = null,
          associatedPrisonersNumber = null
        ),
      )
    )
    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12347",
        reportNumber = 123,
        reportByUserId = "A_SMITH",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 3,
          dateTimeOfIncident = dateTimeOfIncident,
          handoverDeadline = dateTimeOfIncident.plusDays(3)
        ),
        incidentRole = IncidentRole(
          roleCode = null,
          associatedPrisonersNumber = null
        ),
      )
    )
    val foundAdjudications = draftAdjudicationRepository.findUnsubmittedByAgencyIdAndCreatedByUserId("MDI", "ITAG_USER")

    assertThat(foundAdjudications).hasSize(1)
      .extracting("prisonerNumber")
      .contains(
        "A12345"
      )
  }

  private fun newDraft(): DraftAdjudication {
    return DraftAdjudication(
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2,
        dateTimeOfIncident = DEFAULT_DATE_TIME,
        handoverDeadline = DEFAULT_DATE_TIME.plusDays(2)
      ),
      incidentRole = IncidentRole(
        roleCode = "25a",
        associatedPrisonersNumber = "B23456"
      ),
    )
  }

  private fun draftWithAllData(): DraftAdjudication {
    return DraftAdjudication(
      prisonerNumber = "A12345",
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2,
        dateTimeOfIncident = DEFAULT_DATE_TIME,
        handoverDeadline = DEFAULT_DATE_TIME.plusDays(2)
      ),
      incidentRole = IncidentRole(
        roleCode = "25a",
        associatedPrisonersNumber = "B23456"
      ),
      offenceDetails = mutableListOf(
        Offence( // offence with minimal data set
          offenceCode = 2,
          paragraphNumber = "1",
        ),
        Offence( // offence with all data set
          offenceCode = 3,
          paragraphNumber = "2",
          victimPrisonersNumber = "A1234AA",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "Someones Name Here"
        ),
      ),
      incidentStatement = IncidentStatement(
        statement = "Example statement",
        completed = false,
      ),
    )
  }

  companion object {
    val DEFAULT_DATE_TIME = LocalDateTime.of(2021, 10, 3, 10, 10, 22)
  }
}
