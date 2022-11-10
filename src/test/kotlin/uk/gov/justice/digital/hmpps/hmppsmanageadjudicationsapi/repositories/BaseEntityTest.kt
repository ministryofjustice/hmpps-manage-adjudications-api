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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@WithMockUser(username = "ITAG_USER")
@Import(AuditConfiguration::class, UserDetails::class)
class BaseEntityTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Test
  fun `check the correct audit data is used`() {
    val draft = newDraft()

    val savedEntity = entityManager.persistAndFlush(draft)

    Thread.sleep(2000)

    savedEntity.incidentRole?.roleCode = "25c"
    val updatedEntity = entityManager.persistAndFlush(savedEntity)

    assertThat(updatedEntity.incidentRole?.createdByUserId).isEqualTo("ITAG_USER")
    assertThat(updatedEntity.incidentRole?.modifiedByUserId).isEqualTo("ITAG_USER")
    assertThat(updatedEntity.incidentRole?.modifiedDateTime).isGreaterThan(updatedEntity.incidentRole?.createDateTime)
  }

  private fun newDraft(): DraftAdjudication {
    return DraftAdjudication(
      prisonerNumber = "A12345",
      gender = Gender.MALE,
      agencyId = "MDI",
      incidentDetails = IncidentDetails(
        locationId = 2,
        dateTimeOfIncident = DEFAULT_DATE_TIME,
        dateTimeOfDiscovery = DEFAULT_DATE_TIME,
        handoverDeadline = DEFAULT_DATE_TIME.plusDays(2)
      ),
      incidentRole = IncidentRole(
        roleCode = "25a",
        associatedPrisonersNumber = "B23456",
        associatedPrisonersName = "Associated Prisoner",
      ),
      isYouthOffender = true
    )
  }

  companion object {
    val DEFAULT_DATE_TIME = LocalDateTime.of(2021, 10, 3, 10, 10, 22)
  }
}
