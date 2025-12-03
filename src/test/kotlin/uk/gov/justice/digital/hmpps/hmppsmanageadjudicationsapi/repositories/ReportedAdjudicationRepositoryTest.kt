package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import jakarta.validation.ConstraintViolationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.AuditConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ProtectedCharacteristics
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.RehabilitativeActivity
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportsService.Companion.transferOutAndHearingsToScheduledCutOffDate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.EntityBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@DataJpaTest
@ActiveProfiles("jpa")
@WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
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
    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1234",
        dateTime = dateTimeOfIncident,
        hearingId = null,
      ),
    )
    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1235",
        dateTime = dateTimeOfIncident.plusHours(1),
        hearingId = null,
      ),
    )
    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "1236",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "LEI",
        hearingId = null,
      ),
    )
    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "123666",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "BXI",
        hearingId = null,
      ).also {
        it.overrideAgencyId = "LEI"
        it.status = ReportedAdjudicationStatus.ADJOURNED
        it.lastModifiedAgencyId = "LEI"
      },
    )
    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "9999",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "BXI",
        hearingId = null,
      ).also {
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfFirstHearing = dateTimeOfIncident.plusHours(2)
      },
    )
    entityManager.getEntityManager().persist(
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
    entityManager.getEntityManager().persist(
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
    entityManager.getEntityManager().persist(
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
    entityManager.getEntityManager().persist(
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

    entityManager.getEntityManager().persist(
      entityBuilder.reportedAdjudication(
        chargeNumber = "199777",
        dateTime = dateTimeOfIncident.plusHours(1),
        agencyId = "BXI",
        hearingId = null,
        offenderBookingId = 1L,
      ).also {
        it.hearings.clear()
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now(),
            locationId = 1,
            locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
            agencyId = "",
            chargeNumber = "",
            oicHearingType = OicHearingType.GOV_ADULT,
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
          ),
        )
        it.clearOutcomes()
        it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
        it.addPunishment(
          Punishment(
            type = PunishmentType.DAMAGES_OWED,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 0, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1)),
            ),
          ),
        )
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
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
      )
      .contains(
        adjudication.isYouthOffender,
        adjudication.incidentRoleCode,
        adjudication.incidentRoleAssociatedPrisonersNumber,
        adjudication.incidentRoleAssociatedPrisonersName,
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
      )
      .contains(
        Tuple(
          adjudication.offenceDetails[0].offenceCode,
          adjudication.offenceDetails[0].victimPrisonersNumber,
          adjudication.offenceDetails[0].victimStaffUsername,
          adjudication.offenceDetails[0].victimOtherPersonsName,
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
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "excluded", agencyId = "LEI", dateTime = LocalDateTime.now())
        .also {
          it.hearings.clear()
          it.overrideAgencyId = "MDI"
          it.status = ReportedAdjudicationStatus.ADJOURNED
        },
    )

    val foundAdjudications = reportedAdjudicationRepository.findAllReportsByAgency(
      "LEI",
      LocalDate.now().plusDays(1).atStartOfDay(),
      LocalDate.now().plusDays(1).atTime(
        LocalTime.MAX,
      ),
      ReportedAdjudicationStatus.entries.filter { it != ReportedAdjudicationStatus.UNSCHEDULED }.map { it.name },
      Pageable.ofSize(10),
    )

    assertThat(foundAdjudications.content).hasSize(2)
      .extracting("chargeNumber")
      .contains(
        "1236",
        "123666",
      )
  }

  @Test
  fun `awaiting review reports include transferred out`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "transferred", agencyId = "IWI", dateTime = LocalDateTime.now())
        .also {
          it.hearings.clear()
          it.overrideAgencyId = "FKI"
          it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        },
    )

    val foundAdjudications = reportedAdjudicationRepository.findAllReportsByAgency(
      "IWI",
      LocalDate.now().plusDays(1).atStartOfDay(),
      LocalDate.now().plusDays(1).atTime(
        LocalTime.MAX,
      ),
      listOf(ReportedAdjudicationStatus.AWAITING_REVIEW.name),
      Pageable.ofSize(10),
    )

    assertThat(foundAdjudications.content).hasSize(1)
    assertThat(foundAdjudications.content.first().chargeNumber).isEqualTo("transferred")
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
        ReportedAdjudicationStatus.entries,
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
      entityManager.getEntityManager().persist(
        entityBuilder.reportedAdjudication(chargeNumber = "1237").also {
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
          PunishmentSchedule(duration = 10, startDate = LocalDate.now()),
        ),
      ),
    )

    val savedEntity = reportedAdjudicationRepository.save(adjudication)

    assertThat(savedEntity.getPunishments().first().type).isEqualTo(PunishmentType.ADDITIONAL_DAYS)
    assertThat(savedEntity.getPunishments().first().getSchedule().first().startDate).isEqualTo(LocalDate.now())
    assertThat(savedEntity.getPunishments().first().getSchedule().first().duration).isEqualTo(10)
  }

  @Test
  fun `suspended search `() {
    for (i in 1..10) {
      reportedAdjudicationRepository.save(
        entityBuilder.reportedAdjudication(chargeNumber = i.toString()).also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.prisonerNumber = "TEST"
          it.hearings.clear()
          it.addPunishment(
            Punishment(
              type = PunishmentType.CONFINEMENT,
              suspendedUntil = LocalDate.now().minusDays(2).plusDays(i.toLong()),
              schedule = mutableListOf(
                PunishmentSchedule(
                  duration = 10,
                  suspendedUntil = LocalDate.now().minusDays(2).plusDays(i.toLong()),
                ),
              ),
            ),
          )
        },
      )
    }

    val suspendedResult = reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsSuspendedUntilAfter(
      ReportedAdjudicationStatus.CHARGE_PROVED,
      "TEST",
      LocalDate.now(),
    )

    assertThat(suspendedResult.size).isEqualTo(8)
  }

  @Test
  fun `additional days search`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "100100").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.prisonerNumber = "TEST"
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "100101").also {
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.hearings.clear()
        it.prisonerNumber = "TEST"
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    val additionalDaysReports =
      reportedAdjudicationRepository.findByStatusAndPrisonerNumberAndPunishmentsTypeAndPunishmentsSuspendedUntilIsNull(
        ReportedAdjudicationStatus.CHARGE_PROVED,
        "TEST",
        PunishmentType.ADDITIONAL_DAYS,
      )

    assertThat(additionalDaysReports.size).isEqualTo(1)
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
  fun `count by agency and status in `() {
    assertThat(
      reportedAdjudicationRepository.countByOriginatingAgencyIdAndOverrideAgencyIdIsNullAndStatusInAndDateTimeOfDiscoveryAfter(
        "LEI",
        listOf(ReportedAdjudicationStatus.UNSCHEDULED),
        LocalDateTime.now(),
      ),
    ).isEqualTo(1)
  }

  @Test
  fun `count by override agency and status in `() {
    assertThat(
      reportedAdjudicationRepository.countByOverrideAgencyIdAndStatusInAndDateTimeOfDiscoveryAfter(
        "LEI",
        listOf(ReportedAdjudicationStatus.ADJOURNED),
        LocalDateTime.now(),
      ),
    ).isEqualTo(1)
  }

  @Test
  fun `count by transfer in`() {
    assertThat(
      reportedAdjudicationRepository.countTransfersIn(
        "MDI",
        listOf(ReportedAdjudicationStatus.UNSCHEDULED).map { it.name },
      ),
    ).isEqualTo(1)
  }

  @Test
  fun `count by transfer out`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(dateTime = LocalDateTime.now(), chargeNumber = "-9999").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.SCHEDULED
        it.overrideAgencyId = "BXI"
        it.lastModifiedAgencyId = "MDI"
      },
    )

    assertThat(
      reportedAdjudicationRepository.countTransfersOut(
        "MDI",
        listOf(ReportedAdjudicationStatus.SCHEDULED).map { it.name },
        transferOutAndHearingsToScheduledCutOffDate,
      ),
    ).isEqualTo(1)
  }

  @Test
  fun `find by override agency id `() {
    val page = reportedAdjudicationRepository.findTransfersInByAgency(
      "MDI",
      ReportedAdjudicationStatus.entries.map { it.name },
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
            consecutiveToChargeNumber = "1234",
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    val result = reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn(
      "1234",
      listOf(PunishmentType.PROSPECTIVE_DAYS),
    )
    assertThat(result.size).isEqualTo(1)
  }

  @Test
  @org.junit.jupiter.api.Disabled("Requires PostgreSQL sequence - skipped for H2 tests")
  fun `get next charge number`() {
    assertThat(reportedAdjudicationRepository.getNextChargeSequence("MDI_CHARGE_SEQUENCE")).isEqualTo(1)
  }

  @Test
  fun `adjudication summary`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 2L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now().minusDays(1),
            locationId = 1,
            locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2b"),
            agencyId = "",
            oicHearingType = OicHearingType.GOV_ADULT,
            chargeNumber = "",
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = ""),
          ),
        )
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now().minusDays(1),
            locationId = 1,
            locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2c"),
            agencyId = "",
            oicHearingType = OicHearingType.GOV_ADULT,
            chargeNumber = "",
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
          ),
        )
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      },
    )

    val adjudications = reportedAdjudicationRepository.activeChargeProvedForBookingId(
      bookingId = 2L,
      cutOff = LocalDate.now().minusDays(2).atStartOfDay(),
    )

    assertThat(adjudications).isEqualTo(1L)
  }

  @Test
  fun `reports by booking and agency`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(2)
        it.originatingAgencyId = "TJW"
        it.overrideAgencyId = "MDI"
      },
    )

    val response = reportedAdjudicationRepository.findAdjudicationsForBooking(
      offenderBookingId = 3L,
      statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
      startDate = LocalDateTime.now().minusYears(1),
      endDate = LocalDateTime.now(),
      agencies = listOf("MDI"),
      pageable = Pageable.ofSize(10),
    )
    assertThat(response.content.size).isEqualTo(2)
  }

  @Test
  fun `reports by booking and agency with punishments`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(2)
        it.originatingAgencyId = "TJW"
        it.overrideAgencyId = "MDI"
      },
    )

    val response = reportedAdjudicationRepository.findAdjudicationsForBookingWithPunishments(
      offenderBookingId = 3L,
      statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
      startDate = LocalDateTime.now().minusYears(1),
      endDate = LocalDateTime.now(),
      agencies = listOf("MDI"),
      ada = true,
      pada = false,
      suspended = false,
      pageable = Pageable.ofSize(10),
    )
    assertThat(response.content.size).isEqualTo(1)
  }

  @CsvSource(",1", "testing,0")
  @ParameterizedTest
  fun `reports by booking id with suspended punishments`(activatedBy: String?, expected: Int) {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 103L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedByChargeNumber = activatedBy,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    assertThat(
      reportedAdjudicationRepository.findAdjudicationsForBookingWithPunishments(
        offenderBookingId = 103L,
        statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
        startDate = LocalDateTime.now().minusYears(1),
        endDate = LocalDateTime.now(),
        agencies = listOf("MDI"),
        ada = false,
        pada = false,
        suspended = true,
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(expected)
  }

  @Test
  fun `reports by prisoner and agency`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(2)
        it.originatingAgencyId = "TJW"
        it.overrideAgencyId = "MDI"
      },
    )

    val response = reportedAdjudicationRepository.findAdjudicationsForPrisoner(
      prisonerNumber = "A12345",
      statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
      startDate = LocalDateTime.now().minusYears(1),
      endDate = LocalDateTime.now(),
      pageable = Pageable.ofSize(10),
    )
    assertThat(response.content.size).isEqualTo(2)
  }

  @Test
  fun `reports by prisoner and agency with punishments`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "TESTING_SUM2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(2)
        it.originatingAgencyId = "TJW"
        it.overrideAgencyId = "MDI"
      },
    )

    val response = reportedAdjudicationRepository.findAdjudicationsForPrisonerWithPunishments(
      prisonerNumber = "A12345",
      statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
      startDate = LocalDateTime.now().minusYears(1),
      endDate = LocalDateTime.now(),
      ada = true,
      pada = false,
      suspended = false,
      pageable = Pageable.ofSize(10),
    )
    assertThat(response.content.size).isEqualTo(1)
  }

  @CsvSource(",1", "testing,0")
  @ParameterizedTest
  fun `reports for prisoner with punishments excludes suspended activated`(activatedBy: String?, expected: Int) {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(
        offenderBookingId = 3L,
        chargeNumber = "TESTING_SUM",
        prisonerNumber = "testing",
      ).also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
        it.dateTimeOfDiscovery = LocalDateTime.now().minusDays(1)
        it.originatingAgencyId = "MDI"
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.ADDITIONAL_DAYS,
            suspendedUntil = LocalDate.now(),
            activatedByChargeNumber = activatedBy,
            schedule = mutableListOf(
              PunishmentSchedule(duration = 10),
            ),
          ),
        )
      },
    )

    assertThat(
      reportedAdjudicationRepository.findAdjudicationsForPrisonerWithPunishments(
        prisonerNumber = "testing",
        statuses = listOf(ReportedAdjudicationStatus.CHARGE_PROVED.name),
        startDate = LocalDateTime.now().minusYears(1),
        endDate = LocalDateTime.now(),
        ada = false,
        pada = false,
        suspended = true,
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(expected)
  }

  @Test
  fun `get active punishments`() {
    assertThat(
      reportedAdjudicationRepository.findByStatusAndOffenderBookingIdAndPunishmentsSuspendedUntilIsNullAndPunishmentsScheduleEndDateIsAfter(
        ReportedAdjudicationStatus.CHARGE_PROVED,
        1L,
        LocalDate.now().minusDays(1),
      ).size,
    ).isEqualTo(1)
  }

  @Test
  fun `find by charge number contains `() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2").also {
        it.hearings.clear()
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-1").also {
        it.hearings.clear()
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "112345-1").also {
        it.hearings.clear()
      },
    )

    assertThat(
      reportedAdjudicationRepository.findByPrisonerNumberAndChargeNumberStartsWith(
        "A12345",
        "12345-",
      ).size,
    ).isEqualTo(2)
  }

  @Test
  fun `find by associated prisoner number`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2").also {
        it.incidentRoleAssociatedPrisonersNumber = "FROM"
        it.hearings.clear()
      },
    )
    assertThat(
      reportedAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber("FROM").first().chargeNumber,
    ).isEqualTo(
      "12345-2",
    )
  }

  @Test
  fun `find by victims prisoner number`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2").also {
        it.offenceDetails.first().victimPrisonersNumber = "FROM"
        it.hearings.clear()
      },
    )
    assertThat(
      reportedAdjudicationRepository.findByOffenceDetailsVictimPrisonersNumber("FROM").first().chargeNumber,
    ).isEqualTo(
      "12345-2",
    )
  }

  @Test
  fun `offender has bookings`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2").also {
        it.hearings.clear()
      },
    )

    assertThat(reportedAdjudicationRepository.existsByOffenderBookingId(3L)).isTrue
    assertThat(reportedAdjudicationRepository.existsByOffenderBookingId(4L)).isFalse
  }

  @Test
  fun `find by offender booking id and status`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      },
    )
    assertThat(
      reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(
        3L,
        ReportedAdjudicationStatus.CHARGE_PROVED,
      ).size,
    ).isEqualTo(1)
  }

  @Test
  fun `find by prisoner number and date time`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(offenderBookingId = 3L, chargeNumber = "12345-2", prisonerNumber = "XZY")
        .also {
          it.hearings.clear()
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.dateTimeOfDiscovery = LocalDateTime.now()
        },
    )
    assertThat(
      reportedAdjudicationRepository.findByPrisonerNumberAndDateTimeOfDiscoveryBetween(
        "XZY",
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().plusDays(1),
      ).size,
    ).isEqualTo(1)
  }

  @Test
  fun `find by transfer type all`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(agencyId = "OUT", chargeNumber = "12345-2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        it.dateTimeOfDiscovery = LocalDateTime.now()
        it.overrideAgencyId = "IN"
      },
    )

    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "12345-8").also {
        it.hearings.clear()
        it.overrideAgencyId = "OUT"
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfDiscovery = LocalDateTime.now()
      },
    )

    assertThat(
      reportedAdjudicationRepository.findTransfersAllByAgency(
        agencyId = "OUT",
        statuses = listOf(ReportedAdjudicationStatus.AWAITING_REVIEW.name, ReportedAdjudicationStatus.UNSCHEDULED.name),
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(2)
  }

  @Test
  fun `find by transfer type in`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "12345-8").also {
        it.hearings.clear()
        it.overrideAgencyId = "IN"
        it.status = ReportedAdjudicationStatus.UNSCHEDULED
        it.dateTimeOfDiscovery = LocalDateTime.now()
      },
    )

    assertThat(
      reportedAdjudicationRepository.findTransfersInByAgency(
        agencyId = "IN",
        statuses = listOf(ReportedAdjudicationStatus.UNSCHEDULED.name),
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(1)
  }

  @Test
  fun `find by transfer type out`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(agencyId = "OUT", chargeNumber = "12345-2").also {
        it.hearings.clear()
        it.status = ReportedAdjudicationStatus.AWAITING_REVIEW
        it.dateTimeOfDiscovery = LocalDateTime.now()
        it.overrideAgencyId = "IN"
      },
    )

    assertThat(
      reportedAdjudicationRepository.findTransfersOutByAgency(
        agencyId = "OUT",
        statuses = listOf(ReportedAdjudicationStatus.AWAITING_REVIEW.name),
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(1)
  }

  @CsvSource("IN,0", "OUT,1")
  @ParameterizedTest
  fun `transfers out migrated data does not mark the last updated, therefore need to join scheduled to hearings to check agency`(
    agencyId: String,
    expected: Int,
  ) {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(agencyId = "OUT", chargeNumber = "12345-2").also {
        it.hearings.clear()
        it.hearings.add(
          Hearing(
            locationId = 1,
            locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2d"),
            dateTimeOfHearing = LocalDateTime.now(),
            chargeNumber = "12345-2",
            oicHearingType = OicHearingType.GOV_ADULT,
            agencyId = agencyId,
          ),
        )
        it.status = ReportedAdjudicationStatus.SCHEDULED
        it.dateTimeOfDiscovery = LocalDateTime.now()
        it.overrideAgencyId = "IN"
      },
    )

    assertThat(
      reportedAdjudicationRepository.findTransfersOutByAgency(
        agencyId = "OUT",
        statuses = listOf(ReportedAdjudicationStatus.SCHEDULED.name),
        cutOffDate = transferOutAndHearingsToScheduledCutOffDate,
        pageable = Pageable.ofSize(10),
      ).content.size,
    ).isEqualTo(expected)
  }

  @Test
  fun `protected characteristics`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "PROTECTED").also {
        it.hearings.clear()
        it.offenceDetails.clear()
        it.offenceDetails.add(
          ReportedOffence(
            offenceCode = 1,
            protectedCharacteristics = mutableListOf(
              ProtectedCharacteristics(characteristic = Characteristic.AGE),
            ),
          ),
        )
      },
    )
    assertThat(
      reportedAdjudicationRepository.findByChargeNumber("PROTECTED")!!.offenceDetails.first().protectedCharacteristics.first().characteristic
        == Characteristic.AGE,
    ).isTrue
  }

  @Test
  fun `find reports activated by `() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "activated").also {
        it.hearings.clear()
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.EXCLUSION_WORK,
            activatedByChargeNumber = "12345",
            schedule =
            mutableListOf(PunishmentSchedule(duration = 0)),
          ),
        )
      },
    )

    assertThat(reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber("12345").size).isEqualTo(1)
  }

  @Test
  fun `rehabilitative activities mapping`() {
    reportedAdjudicationRepository.save(
      entityBuilder.reportedAdjudication(chargeNumber = "rehab").also {
        it.hearings.clear()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PAYBACK,
            schedule = mutableListOf(PunishmentSchedule(duration = 1)),
            rehabilitativeActivities = mutableListOf(
              RehabilitativeActivity(details = "rehab", monitor = "monitor", endDate = LocalDate.now()),
            ),
          ),
        )
      },
    )

    assertThat(
      reportedAdjudicationRepository.findByChargeNumber("rehab")!!.getPunishments().first().rehabilitativeActivities,
    ).isNotEmpty
  }
}
