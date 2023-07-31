package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.AdditionalVictim
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferIgnoreStatuses
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
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

  @Autowired
  lateinit var hearingRepository: HearingRepository

  private val entityBuilder: EntityBuilder = EntityBuilder()

  private val dateTimeOfIncident = LocalDateTime.now()

  @BeforeEach
  fun setUp() {
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1234",
        dateTime = dateTimeOfIncident,
        hearingId = null,
      ),
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1235",
        dateTime = dateTimeOfIncident.plusHours(1),
        hearingId = null,
      ),
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1236",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "LEI",
        hearingId = null,
      ),
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "12366",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "TJW",
        hearingId = null,
      ).also {
        it.overrideAgencyId = "LEI"
        it.status = ReportedAdjudicationStatus.SCHEDULED
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "123666",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "TJW",
        hearingId = null,
      ).also {
        it.overrideAgencyId = "LEI"
        it.status = ReportedAdjudicationStatus.ADJOURNED
        it.lastModifiedAgencyId = "LEI"
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "9999",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "TJW",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfFirstHearing = dateTimeOfIncident.plusHours(2)
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "9998",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "XXX",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.SCHEDULED
        it.dateTimeOfIssue = LocalDateTime.now()
        it.dateTimeOfFirstHearing = LocalDateTime.now()
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "9997",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "LEI",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfIssue = LocalDateTime.now()
        it.dateTimeOfFirstHearing = LocalDateTime.now()
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "19997",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "LEI",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfIssue = LocalDateTime.now()
        it.dateTimeOfFirstHearing = LocalDateTime.now()
        it.overrideAgencyId = "MDI"
      },
    )
    entityManager.persistAndFlush(
      entityBuilder.reportedAdjudication(
        chargeNumber = "199977",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "LEI",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfIssue = LocalDateTime.now()
        it.dateTimeOfFirstHearing = LocalDateTime.now()
        it.overrideAgencyId = "MDI"
        it.lastModifiedAgencyId = "MDI"
      },
    )
  }

  @Test
  fun `save a new reported adjudication`() {
    val adjudication = entityBuilder.reportedAdjudication(chargeNumber = "1238", hearingId = null)
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "chargeNumber", "originatingAgencyId", "createdByUserId", "gender")
      .contains(
        adjudication.id,
        adjudication.prisonerNumber,
        adjudication.chargeNumber,
        adjudication.originatingAgencyId,
        adjudication.createdByUserId,
        adjudication.gender,
      )

    assertThat(savedEntity)
      .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery", "handoverDeadline", "statement")
      .contains(
        adjudication.locationId,
        adjudication.dateTimeOfIncident,
        adjudication.dateTimeOfIncident.plusDays(1),
        adjudication.handoverDeadline,
        adjudication.statement,
      )

    assertThat(savedEntity)
      .extracting(
        "isYouthOffender",
        "incidentRoleCode",
        "incidentRoleAssociatedPrisonersNumber",
        "incidentRoleAssociatedPrisonersName",
        "additionalAssociates",
      )
      .contains(
        adjudication.isYouthOffender,
        adjudication.incidentRoleCode,
        adjudication.incidentRoleAssociatedPrisonersNumber,
        adjudication.incidentRoleAssociatedPrisonersName,
        adjudication.additionalAssociates,
      )

    assertThat(savedEntity.offenceDetails).hasSize(1)
      .extracting(
        "offenceCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName",
      )
      .contains(
        Tuple(
          adjudication.offenceDetails[0].offenceCode,
          adjudication.offenceDetails[0].victimPrisonersNumber,
          adjudication.offenceDetails[0].victimStaffUsername,
          adjudication.offenceDetails[0].victimOtherPersonsName,
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
        "lastName",
      )
      .contains(
        Tuple(
          adjudication.witnesses[0].code,
          adjudication.witnesses[0].firstName,
          adjudication.witnesses[0].lastName,
        ),
      )

    assertThat(savedEntity.hearings).hasSize(1)
      .extracting(
        "locationId",
        "dateTimeOfHearing",
        "agencyId",
        "chargeNumber",
        "oicHearingId",
      )
      .contains(
        Tuple(
          adjudication.hearings[0].locationId,
          adjudication.hearings[0].dateTimeOfHearing,
          adjudication.hearings[0].agencyId,
          adjudication.hearings[0].chargeNumber,
          adjudication.hearings[0].oicHearingId,
        ),
      )
  }

  @Test
  fun `update offence details of an existing reported adjudication`() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")

    adjudication!!.offenceDetails =
      mutableListOf(
        ReportedOffence(
          offenceCode = 5,
          victimPrisonersNumber = "C2345CC",
          victimStaffUsername = "DEF34G",
          victimOtherPersonsName = "Yet Another Person",
          additionalVictims = mutableListOf(
            AdditionalVictim(),
          ),
        ),
      )
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "chargeNumber", "createdByUserId")
      .contains(
        adjudication.id,
        adjudication.prisonerNumber,
        adjudication.chargeNumber,
        adjudication.createdByUserId,
      )

    assertThat(savedEntity.offenceDetails).hasSize(1)
      .extracting(
        "offenceCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName",
        "additionalVictims",
      )
      .contains(
        Tuple(
          adjudication.offenceDetails[0].offenceCode,
          adjudication.offenceDetails[0].victimPrisonersNumber,
          adjudication.offenceDetails[0].victimStaffUsername,
          adjudication.offenceDetails[0].victimOtherPersonsName,
          adjudication.offenceDetails[0].additionalVictims,
        ),
      )
  }

  @Test
  fun `set the status an existing reported adjudication`() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")

    adjudication!!.transition(
      to = ReportedAdjudicationStatus.REJECTED,
      reason = "Status Reason",
      details = "Status Details",
      reviewUserId = "A_REVIEWER",
    )
    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("status", "statusReason", "statusDetails")
      .contains(
        adjudication.status,
        adjudication.statusReason,
        adjudication.statusDetails,
      )
  }

  @Test
  fun `find reported adjudications by report number`() {
    val foundAdjudication = reportedAdjudicationRepository.findByChargeNumber("1234")

    assertThat(foundAdjudication)
      .extracting("chargeNumber", "statement")
      .contains(
        "1234",
        "Example statement",
      )
  }

  @Test
  fun `find reported adjudications by agency id`() {
    val foundAdjudications = reportedAdjudicationRepository.findAllReportsByAgency(
      "LEI",
      LocalDate.now().plusDays(1).atStartOfDay(),
      LocalDate.now().plusDays(1).atTime(
        LocalTime.MAX,
      ),
      ReportedAdjudicationStatus.values().toList().filter { it != ReportedAdjudicationStatus.UNSCHEDULED }.map { it.name },
      transferIgnoreStatuses.map { it.name },
      Pageable.ofSize(10),
    )

    assertThat(foundAdjudications.content).hasSize(3)
      .extracting("chargeNumber")
      .contains(
        "1236",
        "12366",
        "123666",
      )
  }

  @Test
  fun `find reported adjudications by agency id and first hearing date`() {
    val foundAdjudications = reportedAdjudicationRepository.findReportsForPrint(
      "XXX",
      LocalDate.now().minusDays(1).atStartOfDay(),
      LocalDate.now().plusDays(1).atTime(
        LocalTime.MAX,
      ),
      ReportedAdjudicationStatus.issuableStatusesForPrint().map { it.name },
    )

    assertThat(foundAdjudications).hasSize(1)
      .extracting("chargeNumber")
      .contains(
        "9998",
      )
  }

  @Test
  fun `find reported adjudications by created user and agency id`() {
    val foundAdjudications =
      reportedAdjudicationRepository.findByCreatedByUserIdAndOriginatingAgencyIdAndDateTimeOfDiscoveryBetweenAndStatusIn(
        "ITAG_USER",
        "MDI",
        LocalDate.now().plusDays(1).atStartOfDay(),
        LocalDate.now().plusDays(1).atTime(
          LocalTime.MAX,
        ),
        ReportedAdjudicationStatus.values().toList(),
        Pageable.ofSize(10),
      )

    assertThat(foundAdjudications.content).hasSize(2)
      .extracting("chargeNumber")
      .contains(
        "1234",
        "1235",
      )
  }

  @Test
  fun `validation error to confirm annotation works`() {
    assertThatThrownBy {
      entityManager.persistAndFlush(
        entityBuilder.reportedAdjudication("1237").also {
          it.damages.add(
            ReportedDamage(
              code = DamageCode.REDECORATION,
              details = "",
              reporter = "11111111111111111111111111111111111111111111111111111",
            ),
          )
        },
      )
    }.isInstanceOf(ConstraintViolationException::class.java)
  }

  @Test
  fun `get adjudications by report number in `() {
    val adjudications = reportedAdjudicationRepository.findByChargeNumberIn(
      listOf("1234", "1235", "1236"),
    )

    assertThat(adjudications.size).isEqualTo(3)
  }

  @Test
  fun `get hearings from hearing repository `() {
    val dod = dateTimeOfIncident.plusWeeks(1)
    val hearings = hearingRepository.findByAgencyIdAndDateTimeOfHearingBetween(
      "MDI",
      dod.toLocalDate().atStartOfDay(),
      dod.toLocalDate().plusDays(1).atStartOfDay(),
    )

    assertThat(hearings.size).isEqualTo(2)
  }

  @Test
  fun `set issue details `() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")
    val now = LocalDateTime.now()
    adjudication!!.issuingOfficer = "testing"
    adjudication.dateTimeOfIssue = now

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity)
      .extracting("issuingOfficer", "dateTimeOfIssue")
      .contains("testing", now)
  }

  @Test
  fun `hearing outcome`() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")
    adjudication!!.hearings.first().hearingOutcome = HearingOutcome(
      adjudicator = "test",
      code = HearingOutcomeCode.REFER_POLICE,
    )

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.hearings.first().hearingOutcome)
      .extracting("code", "adjudicator")
      .contains(HearingOutcomeCode.REFER_POLICE, "test")
  }

  @Test
  fun `adjudication outcome`() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")
    adjudication!!.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE))

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
  }

  @Test
  fun `punishment and schedule `() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")
    adjudication!!.addPunishment(
      Punishment(
        type = PunishmentType.ADDITIONAL_DAYS,
        schedule = mutableListOf(
          PunishmentSchedule(days = 10, startDate = LocalDate.now()),
        ),
      ),
    )

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.getPunishments().first().type).isEqualTo(PunishmentType.ADDITIONAL_DAYS)
    assertThat(savedEntity.getPunishments().first().schedule.first().startDate).isEqualTo(LocalDate.now())
    assertThat(savedEntity.getPunishments().first().schedule.first().days).isEqualTo(10)
  }

  @Test
  fun `suspended search `() {
    for (i in 1..10) {
      reportedAdjudicationRepository.save(
        entityBuilder.reportedAdjudication(chargeNumber = i.toString()).also {
          it.prisonerNumber = "TEST"
          it.hearings.clear()
          it.addPunishment(
            Punishment(
              type = PunishmentType.CONFINEMENT,
              suspendedUntil = LocalDate.now().minusDays(2).plusDays(i.toLong()),
              schedule = mutableListOf(PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now().minusDays(2).plusDays(i.toLong()))),
            ),
          )
        },
      )
    }

    val suspendedResult = reportedAdjudicationRepository.findByPrisonerNumberAndPunishmentsSuspendedUntilAfter("TEST", LocalDate.now())

    assertThat(suspendedResult.size).isEqualTo(8)
  }

  @Test
  fun `additional days search`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "100100").also {
        it.hearings.clear()
        it.prisonerNumber = "TEST"
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            schedule = mutableListOf(
              PunishmentSchedule(days = 10),
            ),
          ),
        )
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "100101").also {
        it.hearings.clear()
        it.prisonerNumber = "TEST"
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10),
            ),
          ),
        )
      },
    )

    val additionalDaysReports = reportedAdjudicationRepository.findByPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull("TEST", PunishmentType.ADDITIONAL_DAYS)

    assertThat(additionalDaysReports.size).isEqualTo(1)
  }

  @Test
  fun `hearing without outcome test`() {
    val hearings = hearingRepository.findByHearingOutcomeIsNull()

    assertThat(hearings.size).isEqualTo(10)
  }

  @Test
  fun `punishment comments`() {
    val adjudication = reportedAdjudicationRepository.findByChargeNumber("1236")
    adjudication!!.punishmentComments.add(PunishmentComment(comment = "some text"))

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.punishmentComments.first().id).isEqualTo(1)
    assertThat(savedEntity.punishmentComments.first().comment).isEqualTo("some text")
  }

  @Test
  fun `findBy prisoner id and statuses `() {
    val adjudications = reportedAdjudicationRepository.findByPrisonerNumberAndStatusIn(
      prisonerNumber = "A12345",
      statuses = listOf(ReportedAdjudicationStatus.UNSCHEDULED),
    )

    assertThat(adjudications.size).isEqualTo(4)
  }

  @Test
  fun `count by agency and status `() {
    assertThat(
      reportedAdjudicationRepository.countByOriginatingAgencyIdAndStatus("LEI", ReportedAdjudicationStatus.UNSCHEDULED),
    ).isEqualTo(3)
  }

  @Test
  fun `count by override agency id and status`() {
    assertThat(
      reportedAdjudicationRepository.countTransfers("MDI", listOf(ReportedAdjudicationStatus.UNSCHEDULED).map { it.name }),
    ).isEqualTo(1)
  }

  @Test
  fun `find by override agency id `() {
    val page = reportedAdjudicationRepository.findTransfersByAgency(
      "MDI",
      LocalDateTime.now().minusYears(1),
      LocalDateTime.now().plusYears(1),
      ReportedAdjudicationStatus.values().toList().map { it.name },
      Pageable.ofSize(10),
    )

    assertThat(page.content).hasSize(1)
      .extracting("chargeNumber")
      .contains(
        "19997",
      )
  }

  @Test
  fun `find by consecutive report number and type `() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "-9999").also {
        it.hearings.clear()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PROSPECTIVE_DAYS,
            consecutiveChargeNumber = "1234",
            schedule = mutableListOf(
              PunishmentSchedule(days = 10),
            ),
          ),
        )
      },
    )

    val result = reportedAdjudicationRepository.findByPunishmentsConsecutiveChargeNumberAndPunishmentsType("1234", PunishmentType.PROSPECTIVE_DAYS)
    assertThat(result.size).isEqualTo(1)
  }

  @Test
  fun `delete migrated record`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "99999/1").also {
        it.hearings.clear()
        it.migrated = true
      },
    )

    assertThat(reportedAdjudicationRepository.findByChargeNumber("99999/1")).isNotNull
    reportedAdjudicationRepository.deleteByMigratedIsTrue()
    assertThat(reportedAdjudicationRepository.findByChargeNumber("99999/1")).isNull()
  }
}
