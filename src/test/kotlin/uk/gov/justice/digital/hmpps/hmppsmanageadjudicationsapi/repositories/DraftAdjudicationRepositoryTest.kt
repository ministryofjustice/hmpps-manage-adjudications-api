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

  @Autowired
  lateinit var draftAdjudicationRepository: DraftAdjudicationRepository

  @Test
  fun `save a new draft adjudication`() {
    val savedEntity = entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12345"
      )
    )

    assertThat(draftAdjudicationRepository.findById(savedEntity.id).orElseThrow())
      .extracting("id", "prisonerNumber")
      .contains(savedEntity.id, "A12345")
  }

  @Test
  fun `add the incident details to an already existing draft adjudication`() {
    val savedEntity = entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12345"
      )
    )

    val draftAdjudication = draftAdjudicationRepository.findById(savedEntity.id).orElseThrow()
    val dateTimeOfIncident = LocalDateTime.now()

    draftAdjudication.addIncidentDetails(IncidentDetails(2, dateTimeOfIncident))

    val draftAdjudicationWithIncidentDetails = draftAdjudicationRepository.findById(savedEntity.id).orElseThrow()

    assertThat(draftAdjudicationWithIncidentDetails)
      .extracting("id", "prisonerNumber")
      .contains(savedEntity.id, "A12345")

    assertThat(draftAdjudicationWithIncidentDetails.getIncidentDetails())
      .extracting("locationId", "dateTimeOfIncident")
      .contains(2L, dateTimeOfIncident)
  }
}
