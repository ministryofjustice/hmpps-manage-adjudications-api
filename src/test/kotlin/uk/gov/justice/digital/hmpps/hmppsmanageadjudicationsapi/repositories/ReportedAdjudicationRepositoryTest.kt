package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Java6Assertions.assertThat
import org.assertj.core.groups.Tuple
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        statement = "Example",
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
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
        incidentRoleCode = "25a",
        incidentRoleAssociatedPrisonersNumber = "B23456",
        statement = "Example 2",
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
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
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        statement = "Example 3",
        offenceDetails = mutableListOf(
          ReportedOffence(
            offenceCode = 2,
            paragraphCode = "3",
          ),
          ReportedOffence(
            offenceCode = 3,
            paragraphCode = "4",
            victimPrisonersNumber = "B1234BB",
            victimStaffUsername = "ABC12D",
            victimOtherPersonsName = "Another Person",
          )
        ),
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
      )
    )
  }

  @Test
  fun `save a new reported adjudication`() {
    val adjudication = reportedAdjudication()
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "reportNumber", "bookingId", "agencyId", "createdByUserId")
      .contains(
        adjudication.id,
        adjudication.prisonerNumber,
        adjudication.reportNumber,
        adjudication.bookingId,
        adjudication.agencyId,
        adjudication.createdByUserId
      )

    assertThat(savedEntity)
      .extracting("locationId", "dateTimeOfIncident", "handoverDeadline", "statement")
      .contains(
        adjudication.locationId,
        adjudication.dateTimeOfIncident,
        adjudication.handoverDeadline,
        adjudication.statement
      )

    assertThat(savedEntity)
      .extracting("incidentRoleCode", "incidentRoleAssociatedPrisonersNumber")
      .contains(adjudication.incidentRoleCode, adjudication.incidentRoleAssociatedPrisonersNumber)

    assertThat(savedEntity.offenceDetails).hasSize(2)
      .extracting(
        "offenceCode",
        "paragraphCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName"
      )
      .contains(
        Tuple(
          adjudication.offenceDetails!![0].offenceCode, adjudication.offenceDetails!![0].paragraphCode,
          adjudication.offenceDetails!![0].victimPrisonersNumber, adjudication.offenceDetails!![0].victimStaffUsername,
          adjudication.offenceDetails!![0].victimOtherPersonsName
        ),
      )
  }

  @Test
  fun `update offence details of an existing reported adjudication`() {
    val adjudication = reportedAdjudicationRepository.findByReportNumber(1236L)

    adjudication!!.offenceDetails =
      mutableListOf(
        ReportedOffence(
          offenceCode = 5,
          paragraphCode = "6",
          victimPrisonersNumber = "C2345CC",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Yet Another Person",
        ),
      )
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "reportNumber", "bookingId", "createdByUserId")
      .contains(
        adjudication.id,
        adjudication.prisonerNumber,
        adjudication.reportNumber,
        adjudication.bookingId,
        adjudication.createdByUserId
      )

    assertThat(savedEntity.offenceDetails).hasSize(1)
      .extracting(
        "offenceCode",
        "paragraphCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName"
      )
      .contains(
        Tuple(
          adjudication.offenceDetails!![0].offenceCode, adjudication.offenceDetails!![0].paragraphCode,
          adjudication.offenceDetails!![0].victimPrisonersNumber, adjudication.offenceDetails!![0].victimStaffUsername,
          adjudication.offenceDetails!![0].victimOtherPersonsName
        ),
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
    val foundAdjudications = reportedAdjudicationRepository.findByAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
      "LEI",
      LocalDate.now().atStartOfDay(),
      LocalDate.now().atTime(
        LocalTime.MAX
      ),
      ReportedAdjudicationStatus.values().toList(),
      Pageable.ofSize(10)
    )

    assertThat(foundAdjudications.content).hasSize(1)
      .extracting("reportNumber")
      .contains(
        1236L
      )
  }

  @Test
  fun `find reported adjudications by created user and agency id`() {
    val foundAdjudications =
      reportedAdjudicationRepository.findByCreatedByUserIdAndAgencyIdAndDateTimeOfIncidentBetweenAndStatusIn(
        "ITAG_USER", "MDI",
        LocalDate.now().atStartOfDay(),
        LocalDate.now().atTime(
          LocalTime.MAX
        ),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(10)
      )

    assertThat(foundAdjudications.content).hasSize(2)
      .extracting("reportNumber")
      .contains(
        1234L, 1235L
      )
  }

  private fun reportedAdjudication(): ReportedAdjudication {
    return ReportedAdjudication(
      reportNumber = 123L,
      bookingId = 234L,
      prisonerNumber = "A12345",
      agencyId = "MDI",
      locationId = 2,
      dateTimeOfIncident = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME,
      handoverDeadline = DraftAdjudicationRepositoryTest.DEFAULT_DATE_TIME.plusDays(2),
      incidentRoleCode = "25a",
      incidentRoleAssociatedPrisonersNumber = "B23456",
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
      statusReason = null,
      statusDetails = null,
    )
  }
}
