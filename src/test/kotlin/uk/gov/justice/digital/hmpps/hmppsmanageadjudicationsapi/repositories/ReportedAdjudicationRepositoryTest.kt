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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
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
        isYouthOffender = false,
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        incidentRoleAssociatedPrisonersName = null,
        statement = "Example",
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = false,
        incidentRoleCode = "25a",
        incidentRoleAssociatedPrisonersNumber = "B23456",
        incidentRoleAssociatedPrisonersName = "Associated Prisoner",
        statement = "Example 2",
        status = ReportedAdjudicationStatus.AWAITING_REVIEW,
        statusReason = null,
        statusDetails = null,
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
        isYouthOffender = true,
        incidentRoleCode = null,
        incidentRoleAssociatedPrisonersNumber = null,
        incidentRoleAssociatedPrisonersName = null,
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
        damages = mutableListOf(),
        evidence = mutableListOf(),
        witnesses = mutableListOf()
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
      .extracting("isYouthOffender", "incidentRoleCode", "incidentRoleAssociatedPrisonersNumber", "incidentRoleAssociatedPrisonersName")
      .contains(
        adjudication.isYouthOffender,
        adjudication.incidentRoleCode,
        adjudication.incidentRoleAssociatedPrisonersNumber,
        adjudication.incidentRoleAssociatedPrisonersName,
      )

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

    assertThat(savedEntity.damages).hasSize(1)
      .extracting(
        "code",
        "details",
      )
      .contains(
        Tuple(
          adjudication.damages[0].code,
          adjudication.damages[0].details,
        ),
      )

    assertThat(savedEntity.evidence).hasSize(1)
      .extracting(
        "code",
        "details",
      )
      .contains(
        Tuple(
          adjudication.evidence[0].code,
          adjudication.evidence[0].details,
        ),
      )

    assertThat(savedEntity.witnesses).hasSize(1)
      .extracting(
        "code",
        "firstName",
        "lastName"
      )
      .contains(
        Tuple(
          adjudication.witnesses[0].code,
          adjudication.witnesses[0].firstName,
          adjudication.witnesses[0].lastName,
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
  fun `set the status an existing reported adjudication`() {
    val adjudication = reportedAdjudicationRepository.findByReportNumber(1236L)

    adjudication!!.transition(
      ReportedAdjudicationStatus.REJECTED,
      "A_REVIEWER",
      "Status Reason",
      "Status Details"
    )
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("id", "status", "reviewUserId", "statusReason", "statusDetails")
      .contains(
        adjudication.id,
        adjudication.status,
        adjudication.reviewUserId,
        adjudication.statusReason,
        adjudication.statusDetails
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
      statusReason = null,
      statusDetails = null,
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
          code = WitnessCode.PRISON_OFFICER,
          firstName = "prison",
          lastName = "officer",
          reporter = "Fred"
        ),
      )
    )
  }
}
