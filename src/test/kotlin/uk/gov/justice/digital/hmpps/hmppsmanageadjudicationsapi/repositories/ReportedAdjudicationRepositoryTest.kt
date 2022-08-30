package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Assertions.assertThatThrownBy
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatusAudit
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.validation.ConstraintViolationException

@DataJpaTest
@ActiveProfiles("test")
@WithMockUser(username = "ITAG_USER")
@Import(AuditConfiguration::class, UserDetails::class)
class ReportedAdjudicationRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var reportedAdjudicationRepository: ReportedAdjudicationRepository

  private val entityBuilder: EntityBuilder = EntityBuilder()

  @BeforeEach
  fun setUp() {
    val dateTimeOfIncident = LocalDateTime.now()

    entityManager.persistAndFlush(entityBuilder.reportedAdjudication(1234L, dateTimeOfIncident))
    entityManager.persistAndFlush(entityBuilder.reportedAdjudication(1235L, dateTimeOfIncident.plusHours(1)))
    entityManager.persistAndFlush(entityBuilder.reportedAdjudication(1236L, dateTimeOfIncident.plusHours(1), "LEI"))
  }

  @Test
  fun `save a new reported adjudication`() {
    val adjudication = entityBuilder.reportedAdjudication(1238L)
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
      ReportedAdjudicationStatusAudit(
        status = Status.REJECTED,
        statusReason = "Status Reason",
        statusDetails = "Status Details"
      ),
      "A_REVIEWER",
    )
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.getLatestStatus())
      .extracting("status", "statusReason", "statusDetails")
      .contains(
        adjudication.getLatestStatus().status,
        adjudication.getLatestStatus().statusReason,
        adjudication.getLatestStatus().statusDetails
      )
  }

  @Test
  fun `find reported adjudications by report number`() {
    val foundAdjudication = reportedAdjudicationRepository.findByReportNumber(1234L)

    assertThat(foundAdjudication)
      .extracting("reportNumber", "statement")
      .contains(
        1234L, "Example statement"
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
      Status.values().toList(),
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
        Status.values().toList(),
        Pageable.ofSize(10)
      )

    assertThat(foundAdjudications.content).hasSize(2)
      .extracting("reportNumber")
      .contains(
        1234L, 1235L
      )
  }

  @Test
  fun `validation error to confirm annotation works`() {
    assertThatThrownBy {
      entityManager.persistAndFlush(
        entityBuilder.reportedAdjudication(1237L).also {
          it.damages.add(ReportedDamage(code = DamageCode.REDECORATION, details = "", reporter = "11111111111111111111111111111111111111111111111111111"))
        }
      )
    }.isInstanceOf(ConstraintViolationException::class.java)
  }
}
