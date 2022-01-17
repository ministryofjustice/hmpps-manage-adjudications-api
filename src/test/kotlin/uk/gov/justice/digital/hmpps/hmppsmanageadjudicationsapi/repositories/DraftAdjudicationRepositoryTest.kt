package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Java6Assertions.assertThat
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
    val dateTimeOfIncident = LocalDateTime.now()

    val savedEntity = entityManager.persistAndFlush(
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
    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "agencyId", "createdByUserId")
      .contains(savedEntity.id, "A12345", "MDI", "ITAG_USER")

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
      .contains(2L, dateTimeOfIncident, dateTimeOfIncident.plusDays(2))

    assertThat(savedEntity.incidentRole)
      .extracting("roleCode", "associatedPrisonersNumber")
      .contains("25a", "B23456")
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
}
