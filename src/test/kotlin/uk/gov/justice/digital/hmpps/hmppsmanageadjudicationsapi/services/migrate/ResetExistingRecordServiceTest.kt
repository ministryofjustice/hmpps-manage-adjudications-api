package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime
import java.util.Optional

class ResetExistingRecordServiceTest : ReportedAdjudicationTestBase() {

  private val resetExistingRecordService = ResetExistingRecordService(reportedAdjudicationRepository)

  @Test
  fun `resets an exiting migration damages`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.damages.clear()
      it.damages.add(ReportedDamage(id = 1, code = DamageCode.CLEANING, details = "", reporter = ""))
      it.damages.add(ReportedDamage(id = 2, code = DamageCode.CLEANING, details = "", reporter = "", migrated = true))
    }
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.damages.size).isEqualTo(1)
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
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.evidence.size).isEqualTo(1)
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
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.witnesses.size).isEqualTo(1)
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
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.hearings.size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration punishments`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.clearPunishments()
      it.addPunishment(Punishment(id = 1, type = PunishmentType.CAUTION, schedule = mutableListOf()))
      it.addPunishment(Punishment(id = 2, type = PunishmentType.CAUTION, schedule = mutableListOf(), migrated = true))
    }
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.getPunishments().size).isEqualTo(1)
  }

  @Test
  fun `resets an existing migration punishment comments`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.punishmentComments.clear()
      it.punishmentComments.add(PunishmentComment(id = 1, comment = ""))
      it.punishmentComments.add(PunishmentComment(id = 2, comment = "", migrated = true))
    }
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.punishmentComments.size).isEqualTo(1)
  }

  @Test
  fun `reset an existing migration outcome`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.clearOutcomes()
      it.addOutcome(Outcome(id = 1, code = OutcomeCode.QUASHED))
      it.addOutcome(Outcome(id = 2, code = OutcomeCode.QUASHED, migrated = true))
    }
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.getOutcomes().size).isEqualTo(1)
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
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.offenceDetails.first().offenceCode).isEqualTo(100)
    Assertions.assertThat(existing.offenceDetails.first().migrated).isEqualTo(false)
    Assertions.assertThat(existing.offenceDetails.first().nomisOffenceCode).isNull()
    Assertions.assertThat(existing.offenceDetails.first().nomisOffenceDescription).isNull()
    Assertions.assertThat(existing.offenceDetails.first().actualOffenceCode).isNull()
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
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.NOMIS)
    Assertions.assertThat(existing.hearings.first().hearingOutcome!!.adjudicator).isEqualTo("")
    Assertions.assertThat(existing.hearings.first().hearingOutcome!!.nomisOutcome).isEqualTo(false)
  }

  @Test
  fun `reset an existing migration hearing outcome`() {
    val existing = entityBuilder.reportedAdjudication(id = 1).also {
      it.hearings.first().hearingOutcome =
        HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "", migrated = true)
    }
    whenever(reportedAdjudicationRepository.findById(any())).thenReturn(Optional.of(existing))
    resetExistingRecordService.reset(existing.id!!)

    Assertions.assertThat(existing.hearings.first().hearingOutcome).isNull()
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
