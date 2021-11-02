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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@WithMockUser(username = "ITAG_USER")
@Import(AuditConfiguration::class, UserDetails::class)
class DraftAdjudicationRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun `save a new draft adjudication`() {
    val dateTimeOfIncident = LocalDateTime.now()

    val savedEntity = entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12345",
        incidentDetails = IncidentDetails(locationId = 2, dateTimeOfIncident = dateTimeOfIncident)
      )
    )
    assertThat(savedEntity)
      .extracting("id", "prisonerNumber")
      .contains(savedEntity.id, "A12345")

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident")
      .contains(2L, dateTimeOfIncident)
  }
}
