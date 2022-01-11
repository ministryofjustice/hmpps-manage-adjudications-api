package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
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

  @BeforeEach
  fun setUp() {
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
    entityManager.persistAndFlush(
      ReportedAdjudication(
        prisonerNumber = "A12347",
        reportNumber = 1236L,
        bookingId = 55L,
        agencyId = "LEI",
        locationId = 4,
        dateTimeOfIncident = dateTimeOfIncident.plusHours(1),
        handoverDeadline = dateTimeOfIncident.plusHours(1).plusDays(2),
        statement = "Example 3"
      )
    )
  }

  @Test
  fun `find reported adjudications by report number`() {
    val foundAdjudication = reportedAdjudicationRepository.findByReportNumber(1234L)

    assertThat(foundAdjudication)
      .extracting("reportNumber", "statement")
      .contains(
        1234L, "Example"
      )
  }

  @Test
  fun `find reported adjudications by agency id`() {
    val foundAdjudications = reportedAdjudicationRepository.findByAgencyId("LEI", Pageable.ofSize(10))

    assertThat(foundAdjudications.content).hasSize(1)
      .extracting("reportNumber")
      .contains(
        1236L
      )
  }

  @Test
  fun `find reported adjudications by created user and agency id`() {
    val foundAdjudications1 = reportedAdjudicationRepository.findAll()
    val foundAdjudications = reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyId("ITAG_USER", "MDI", Pageable.ofSize(10))

    assertThat(foundAdjudications.content).hasSize(2)
      .extracting("reportNumber")
      .contains(
        1234L, 1235L
      )
  }
}
