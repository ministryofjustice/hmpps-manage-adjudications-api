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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@WithMockUser(username = "ITAG_USER")
@Import(AuditConfiguration::class, UserDetails::class)
class ReportedAdjudicationRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var reportedAdjudicationRepository: ReportedAdjudicationRepository

  @Test
  fun `find reported adjudications by report number`() {
    val dateTimeOfIncident = LocalDateTime.now()

    entityManager.persistAndFlush(
      ReportedAdjudication(
        prisonerNumber = "A12345",
        reportNumber = 1234L,
        bookingId = 44L,
        agencyId = "MDI",
        locationId = 2,
        dateTimeOfIncident = dateTimeOfIncident,
        handoverDeadline = dateTimeOfIncident.plusDays(2),
        statement = "Example"
      )
    )
    entityManager.persistAndFlush(
      ReportedAdjudication(
        prisonerNumber = "A12345",
        reportNumber = 1235L,
        bookingId = 44L,
        agencyId = "MDI",
        locationId = 3,
        dateTimeOfIncident = dateTimeOfIncident.plusHours(1),
        handoverDeadline = dateTimeOfIncident.plusHours(1).plusDays(2),
        statement = "Example 2"
      )
    )
    val foundAdjudication = reportedAdjudicationRepository.findByReportNumber(1234L)

    assertThat(foundAdjudication)
      .extracting("reportNumber", "statement")
      .contains(
        1234L, "Example"
      )
  }
}
