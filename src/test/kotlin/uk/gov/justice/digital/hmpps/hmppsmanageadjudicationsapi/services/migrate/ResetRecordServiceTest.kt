package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePreMigrate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingPreMigrate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentPreMigrate
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class ResetRecordServiceTest : ReportedAdjudicationTestBase() {

  private val sessionFactory: SessionFactory = mock()
  private val resetRecordService = ResetRecordService(sessionFactory, reportedAdjudicationRepository)

  @Test
  fun `resets an exiting migration damages`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.damages.clear()
      it.damages.add(ReportedDamage(id = 1, code = DamageCode.CLEANING, details = "", reporter = ""))
      it.damages.add(ReportedDamage(id = 2, code = DamageCode.CLEANING, details = "", reporter = "", migrated = true))
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.damages.size).isEqualTo(1)
  }

  @Test
  fun `resets an exiting migration evidence`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.evidence.clear()
      it.evidence.add(ReportedEvidence(id = 1, code = EvidenceCode.PHOTO, details = "", reporter = ""))
      it.evidence.add(
        ReportedEvidence(
          id = 2,
          code = EvidenceCode.PHOTO,
          details = "",
          reporter = "",
          migrated = true,
        ),
      )
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.evidence.size).isEqualTo(1)
  }

  @Test
  fun `resets an exiting migration witnesses`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.witnesses.clear()
      it.witnesses.add(
        ReportedWitness(
          id = 1,
          code = WitnessCode.OFFICER,
          firstName = "",
          lastName = "",
          reporter = "",
        ),
      )
      it.witnesses.add(
        ReportedWitness(
          id = 2,
          code = WitnessCode.OFFICER,
          reporter = "",
          firstName = "",
          lastName = "",
          migrated = true,
        ),
      )
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.witnesses.size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration hearings`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.hearings.clear()
      it.hearings.add(
        Hearing(
          id = 1,
          dateTimeOfHearing = LocalDateTime.now(),
          oicHearingType = OicHearingType.GOV_ADULT,
          agencyId = "",
          chargeNumber = "",
          locationId = 1,
        ),
      )
      it.hearings.add(
        Hearing(
          id = 2,
          dateTimeOfHearing = LocalDateTime.now(),
          oicHearingType = OicHearingType.GOV_ADULT,
          agencyId = "",
          chargeNumber = "",
          locationId = 1,
          migrated = true,
        ),
      )
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.hearings.size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration punishments`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.clearPunishments()
      it.addPunishment(Punishment(id = 1, type = PunishmentType.CAUTION, schedule = mutableListOf()))
      it.addPunishment(Punishment(id = 2, type = PunishmentType.CAUTION, schedule = mutableListOf(), migrated = true))
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.getPunishments().size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration punishment comments`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.punishmentComments.clear()
      it.punishmentComments.add(PunishmentComment(id = 1, comment = ""))
      it.punishmentComments.add(PunishmentComment(id = 2, comment = "", migrated = true))
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.punishmentComments.size).isEqualTo(1)
  }

  @Test
  fun `reset an existing migration outcome`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.clearOutcomes()
      it.addOutcome(Outcome(id = 1, code = OutcomeCode.QUASHED))
      it.addOutcome(Outcome(id = 2, code = OutcomeCode.QUASHED, migrated = true))
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.getOutcomes().size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration offence code`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.offenceDetails.clear()
      it.offenceDetails.add(
        ReportedOffence(
          id = 1,
          offenceCode = 0,
          actualOffenceCode = 100,
          nomisOffenceCode = "51:17",
          nomisOffenceDescription = "something",
          migrated = true,
        ),
      )
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.offenceDetails.first().offenceCode).isEqualTo(100)
    assertThat(existing.offenceDetails.first().migrated).isEqualTo(false)
    assertThat(existing.offenceDetails.first().nomisOffenceCode).isNull()
    assertThat(existing.offenceDetails.first().nomisOffenceDescription).isNull()
    assertThat(existing.offenceDetails.first().actualOffenceCode).isNull()
  }

  @Test
  fun `resets a nomis hearing outcome`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.hearings.first().hearingOutcome = HearingOutcome(
        nomisOutcome = true,
        adjudicator = "adjudicator",
        code = HearingOutcomeCode.COMPLETE,
      )
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
    assertThat(existing.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("")
    assertThat(existing.hearings.first().hearingOutcome!!.nomisOutcome).isEqualTo(false)
  }

  @Test
  fun `reset an existing migration hearing outcome`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.hearings.first().hearingOutcome =
        HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "", migrated = true)
    }
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
    resetRecordService.reset(existing.chargeNumber)

    assertThat(existing.hearings.first().hearingOutcome).isNull()
  }

  @Nested
  inner class Phase3 {
    @Test
    fun `reset copies pre migrate hearing data back to hearing and removes pre migrate record`() {
      val date = LocalDateTime.now().minusYears(1)
      val existing = entityBuilder.reportedAdjudication(id = 1).also {
        it.hearings.first().hearingPreMigrate = HearingPreMigrate(
          locationId = 100,
          dateTimeOfHearing = date,
          oicHearingType = OicHearingType.INAD_YOI,
        )
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
      resetRecordService.reset(existing.chargeNumber)

      assertThat(existing.hearings.first().hearingPreMigrate).isNull()
      assertThat(existing.hearings.first().locationId).isEqualTo(100)
      assertThat(existing.hearings.first().dateTimeOfHearing).isEqualTo(date)
      assertThat(existing.hearings.first().oicHearingType).isEqualTo(OicHearingType.INAD_YOI)
    }

    @Test
    fun `reset copies pre migrate hearing outcome data back to hearing outcome and removes pre migrate record`() {
      val existing = entityBuilder.reportedAdjudication(id = 1).also {
        it.hearings.first().hearingOutcome = HearingOutcome(
          code = HearingOutcomeCode.REFER_INAD,
          adjudicator = "dave",
          hearingOutcomePreMigrate =
          HearingOutcomePreMigrate(code = HearingOutcomeCode.COMPLETE, adjudicator = "another"),
        )
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
      resetRecordService.reset(existing.chargeNumber)

      assertThat(existing.hearings.first().hearingOutcome!!.hearingOutcomePreMigrate).isNull()
      assertThat(existing.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(existing.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("another")
    }

    @Test
    fun `reset copies pre migrate punishment data back to punishment and removes pre migrate record`() {
      val existing = entityBuilder.reportedAdjudication(id = 1).also {
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "test",
            schedule =
            mutableListOf(PunishmentSchedule(days = 1), PunishmentSchedule(migrated = true, days = 2)),
            punishmentPreMigrate = PunishmentPreMigrate(
              type = PunishmentType.CAUTION,
            ),
            stoppagePercentage = 1,
            amount = 10.0,
            nomisStatus = "SUSP",
          ),
        )
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(existing)
      resetRecordService.reset(existing.chargeNumber)

      assertThat(existing.getPunishments().first().punishmentPreMigrate).isNull()
      assertThat(existing.getPunishments().first().schedule.last().days).isEqualTo(1)
      assertThat(existing.getPunishments().first().type).isEqualTo(PunishmentType.CAUTION)
      assertThat(existing.getPunishments().first().privilegeType).isNull()
      assertThat(existing.getPunishments().first().otherPrivilege).isNull()
      assertThat(existing.getPunishments().first().consecutiveChargeNumber).isNull()
      assertThat(existing.getPunishments().first().amount).isNull()
      assertThat(existing.getPunishments().first().stoppagePercentage).isNull()
      assertThat(existing.getPunishments().first().nomisStatus).isNull()
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
