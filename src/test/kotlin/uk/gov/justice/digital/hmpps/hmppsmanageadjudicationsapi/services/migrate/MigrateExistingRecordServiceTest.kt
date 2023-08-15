package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDate
import java.time.LocalDateTime

class MigrateExistingRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateExistingRecordService = MigrateExistingRecordService(reportedAdjudicationRepository)

  @BeforeEach
  fun `return save`() {
    whenever(reportedAdjudicationRepository.save(any())).thenReturn(entityBuilder.reportedAdjudication().also { it.hearings.clear() })
  }

  @Nested
  inner class Phase1 {

    @Test
    fun `sanity check - its not same prisoner -throws exception`() {
      val existing = entityBuilder.reportedAdjudication()
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing)
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("Prisoner different between nomis and adjudications")
    }

    @Test
    fun `sanity check - its not same agency -throws exception`() {
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE
      val existing = entityBuilder.reportedAdjudication(prisonerNumber = dto.prisoner.prisonerNumber, agencyId = "XYZ")

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing)
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("agency different between nomis and adjudications")
    }

    @Test
    fun `offence code has been altered and no longer matches - switch to migration mode`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().actualOffenceCode).isEqualTo(1002)
      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(0)
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceCode).isEqualTo(dto.offence.offenceCode)
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceDescription).isEqualTo(dto.offence.offenceDescription)
      assertThat(argumentCaptor.value.offenceDetails.first().migrated).isTrue
    }

    @Test
    fun `adjudication is in correct state, and any hearings, outcomes, punishments`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(
            punishment = Punishment(id = 2, type = PunishmentType.CAUTION, sanctionSeq = dto.punishments.first().sanctionSeq, schedule = mutableListOf()),
          )
          it.hearings.clear()
          it.hearings.add(
            Hearing(id = 2, dateTimeOfHearing = LocalDateTime.now(), locationId = 1, oicHearingType = OicHearingType.GOV_ADULT, agencyId = "", chargeNumber = "", oicHearingId = 1),
          )
        },
      )

      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId).also {
        it.hearings.clear()
        it.clearPunishments()
        it.clearOutcomes()
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }

      val response = migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenderBookingId).isEqualTo(dto.bookingId)
      assertThat(argumentCaptor.value.getPunishments()).isNotEmpty
      assertThat(argumentCaptor.value.getPunishments().first().migrated).isTrue
      assertThat(argumentCaptor.value.getOutcomes()).isNotEmpty
      assertThat(argumentCaptor.value.getOutcomes().first().migrated).isTrue
      assertThat(argumentCaptor.value.hearings).isNotEmpty
      assertThat(argumentCaptor.value.hearings.first().migrated).isTrue
      assertThat(argumentCaptor.value.punishmentComments).isNotEmpty
      assertThat(argumentCaptor.value.punishmentComments.first().migrated).isTrue

      assertThat(response.chargeNumberMapping.chargeNumber).isEqualTo(dto.oicIncidentId.toString())
      assertThat(response.chargeNumberMapping.offenceSequence).isEqualTo(dto.offenceSequence)
      assertThat(response.chargeNumberMapping.oicIncidentId).isEqualTo(dto.oicIncidentId)

      assertThat(response.punishmentMappings).isNotEmpty
      assertThat(response.punishmentMappings!!.first().punishmentId).isNotNull
      assertThat(response.punishmentMappings!!.first().sanctionSeq).isEqualTo(dto.punishments.first().sanctionSeq)
      assertThat(response.punishmentMappings!!.first().bookingId).isEqualTo(dto.bookingId)

      assertThat(response.hearingMappings).isNotEmpty
      assertThat(response.hearingMappings!!.first().hearingId).isEqualTo(2)
      assertThat(response.hearingMappings!!.first().oicHearingId).isEqualTo(1)
    }

    @Test
    fun `damages are updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages.size).isEqualTo(2)
      assertThat(argumentCaptor.value.damages.last().migrated).isTrue
    }

    @Test
    fun `evidence is updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence.size).isEqualTo(2)
      assertThat(argumentCaptor.value.evidence.last().migrated).isTrue
    }

    @Test
    fun `witnesses are updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses.size).isEqualTo(2)
      assertThat(argumentCaptor.value.witnesses.last().migrated).isTrue
    }
  }

  @Nested
  inner class Phase2 {

    private fun existing(dto: AdjudicationMigrateDto) = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId).also {
      it.hearings.clear()
      it.hearings.add(
        Hearing(
          oicHearingId = 1,
          dateTimeOfHearing = LocalDate.now().atStartOfDay(),
          oicHearingType = OicHearingType.GOV_ADULT,
          agencyId = "MDI",
          locationId = 1,
          chargeNumber = dto.oicIncidentId.toString(),
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = ""),
        ),
      )
    }

    @Test
    fun `existing hearing outcome with code NOMIS and no corresponding result throws exception`() {
      val dto = migrationFixtures.PHASE2_HEARINGS_NO_RESULTS

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing(dto))
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("${dto.oicIncidentId} has a NOMIS hearing outcome, and record no longer exists in NOMIS")
    }

    @CsvSource("PROVED", "D", "NOT_PROCEED")
    @ParameterizedTest
    fun `existing hearing outcome with code NOMIS will add corresponding result`(finding: Finding) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.PHASE2_HEARINGS_AND_NOMIS(finding)
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      when (finding) {
        Finding.PROVED -> assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
        Finding.D -> assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
        Finding.NOT_PROCEED -> assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
        else -> {}
      }
    }

    @Test
    fun `existing hearing outcome with code NOMIS has additional hearings after - ADJOURN case`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARINGS_AND_RESULTS_MULTIPLE_PROVED
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
    }

    @Test
    fun `existing hearing outcome with code NOMIS has REF_POLICE`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_REFER_POLICE
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
    }

    @Test
    fun `existing hearing outcome with code NOMIS has PROSECUTION`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_WITH_PROSECUTION
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.PROSECUTION)
    }

    @Test
    fun `existing hearing outcome with code NOMIS has REFER_POLICE and another hearing`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.POLICE_REFERRAL_NEW_HEARING
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
    }

    @Test
    fun `existing hearing outcomes with code NOMIS has CHARG_PROVED and QUASHED`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.QUASHED_SECOND_HEARING
      val existing = existing(dto).also {
        it.hearings.add(
          Hearing(
            oicHearingId = 2,
            dateTimeOfHearing = LocalDate.now().atStartOfDay().plusMinutes(1),
            oicHearingType = OicHearingType.GOV_ADULT,
            agencyId = "MDI",
            locationId = 1,
            chargeNumber = dto.oicIncidentId.toString(),
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = ""),
          ),
        )
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
    }
  }

  @Nested
  inner class Phase3 {
    // these tests are applicable to all (phase 2 and 3)
    @Test
    fun `hearing no longer exists in and should be removed`() {
    }

    @Test
    fun `hearing data matches id and has been altered - update hearing`() {
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
