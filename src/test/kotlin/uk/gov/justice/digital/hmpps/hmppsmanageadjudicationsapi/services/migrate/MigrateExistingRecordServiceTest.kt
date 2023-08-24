package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.AdjudicationMigrateDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeAdjournReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateExistingRecordService.Companion.mapToPunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.utils.MigrationEntityBuilder
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.stream.Stream

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
  inner class Phase2point5 {

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
        ),
      )
      it.hearings.add(
        Hearing(
          oicHearingId = 2,
          dateTimeOfHearing = LocalDate.now().plusDays(1).atStartOfDay(),
          oicHearingType = OicHearingType.GOV_ADULT,
          agencyId = "MDI",
          locationId = 1,
          chargeNumber = dto.oicIncidentId.toString(),
        ),
      )
      it.hearings.add(
        Hearing(
          oicHearingId = 3,
          dateTimeOfHearing = LocalDate.now().plusDays(2).atStartOfDay(),
          oicHearingType = OicHearingType.GOV_ADULT,
          agencyId = "MDI",
          locationId = 1,
          chargeNumber = dto.oicIncidentId.toString(),
        ),
      )
    }

    @Test
    fun `multiple hearings without results will adjourn each hearing except the final one`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.PHASE2_HEARINGS_NO_RESULTS
      val existing = existing(dto)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.reason).isEqualTo(HearingOutcomeAdjournReason.OTHER)
      assertThat(argumentCaptor.value.hearings[1].hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings[1].hearingOutcome!!.reason).isEqualTo(HearingOutcomeAdjournReason.OTHER)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome).isNull()
    }
  }

  @Nested
  inner class Phase3 {
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
          hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = ""),
        ),
      )
    }

    @Test
    fun `hearing no longer exists in nomis throws exception`() {
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing(dto))
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("${dto.oicIncidentId} hearing no longer exists in nomis")
    }

    @Test
    fun `hearing location id, date time and type changed in nomis`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = null
        it.hearings.first().locationId = 100
        it.hearings.first().oicHearingType = OicHearingType.INAD_YOI
      }

      val locationId = existing.hearings.first().locationId
      val dt = existing.hearings.first().dateTimeOfHearing
      val type = existing.hearings.first().oicHearingType

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(dto.hearings.first().locationId)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(dto.hearings.first().oicHearingType)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(dto.hearings.first().hearingDateTime)
      assertThat(argumentCaptor.value.hearings.first().hearingPreMigrate).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingPreMigrate!!.locationId).isEqualTo(locationId)
      assertThat(argumentCaptor.value.hearings.first().hearingPreMigrate!!.oicHearingType).isEqualTo(type)
      assertThat(argumentCaptor.value.hearings.first().hearingPreMigrate!!.dateTimeOfHearing).isEqualTo(dt)
    }

    @Test
    fun `hearing result no longer exists in nomis throws exception`() {
      val dto = migrationFixtures.WITH_HEARING

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing(dto))
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("${dto.oicIncidentId} hearing result no longer exists in nomis")
    }

    @Test
    fun `hearing result adjudicator has changed`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "someone")
      }

      val adjudicator = existing.hearings.first().hearingOutcome!!.adjudicator

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo(dto.hearings.first().adjudicator)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.hearingOutcomePreMigrate).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.hearingOutcomePreMigrate!!.adjudicator).isEqualTo(adjudicator)
    }

    @Test
    fun `hearing result code has changed throws exception`() {
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "someone")
      }

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing)
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("${dto.oicIncidentId} hearing result code has changed")
    }

    @Test
    fun `throws exception if additional hearings and results found - note this will need to be addressed with more data available`() {
      val dto = migrationFixtures.WITH_ADDITIONAL_HEARINGS_IN_NOMIS
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "someone")
      }

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing)
      }.isInstanceOf(ExistingRecordConflictException::class.java)
        .hasMessageContaining("${dto.oicIncidentId} has additional hearings and results in nomis")
    }

    @Test
    fun `punishments created if they exist in nomis only`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
        it.clearPunishments()
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.CONFINEMENT)
    }

    @MethodSource("uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateExistingRecordServiceTest#sanctionMappings")
    @ParameterizedTest
    fun `sanction to punishment mapper maps correctly `(toTest: Triple<OicSanctionCode, Status, PunishmentType>) {
      assertThat(
        MigrationEntityBuilder().createPunishment(
          code = toTest.first.name,
          status = toTest.second.name,
        ).mapToPunishmentType(),
      ).isEqualTo(toTest.third)
    }

    @Test
    fun `damages owed mapping`() {
      assertThat(
        MigrationEntityBuilder().createPunishment(
          code = OicSanctionCode.OTHER.name,
          amount = BigDecimal.ONE,
        ).mapToPunishmentType(),
      ).isEqualTo(PunishmentType.DAMAGES_OWED)
    }

    @MethodSource("uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateExistingRecordServiceTest#privilegeMappings")
    @ParameterizedTest
    fun `forfeit ot privilege mapping`(toTest: Triple<OicSanctionCode, String, PunishmentType>) {
      assertThat(
        MigrationEntityBuilder().createPunishment(
          code = toTest.first.name,
          comment = toTest.second,
        ).mapToPunishmentType(),
      ).isEqualTo(toTest.third)
    }

    @Test
    fun `forfeit other mapping`() {
      assertThat(
        MigrationEntityBuilder().createPunishment(
          code = OicSanctionCode.FORFEIT.name,
          comment = "other",
        ).mapToPunishmentType("other"),
      ).isEqualTo(PunishmentType.PRIVILEGE)
    }

    @Test
    fun `punishments created if they are not matched with adjudications`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
        it.clearPunishments()
        it.addPunishment(
          Punishment(
            type = PunishmentType.EXTRA_WORK,
            schedule = mutableListOf(
              PunishmentSchedule(days = 0),
            ),
          ),
        )
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.EXTRA_WORK)
      assertThat(argumentCaptor.value.getPunishments().last().type).isEqualTo(PunishmentType.CONFINEMENT)
    }

    @Disabled
    @Test
    fun `punishments are matched in adjudications - add latest schedule`() {
      // this will be captured during discovery with John
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }

  companion object {
    @JvmStatic
    fun sanctionMappings(): Stream<Triple<OicSanctionCode, Status, PunishmentType>> =
      listOf(
        Triple(OicSanctionCode.REMACT, Status.IMMEDIATE, PunishmentType.REMOVAL_ACTIVITY),
        Triple(OicSanctionCode.REMWIN, Status.IMMEDIATE, PunishmentType.REMOVAL_WING),
        Triple(OicSanctionCode.CC, Status.IMMEDIATE, PunishmentType.CONFINEMENT),
        Triple(OicSanctionCode.CAUTION, Status.IMMEDIATE, PunishmentType.CAUTION),
        Triple(OicSanctionCode.ADA, Status.IMMEDIATE, PunishmentType.ADDITIONAL_DAYS),
        Triple(OicSanctionCode.ADA, Status.PROSPECTIVE, PunishmentType.PROSPECTIVE_DAYS),
        Triple(OicSanctionCode.STOP_PCT, Status.IMMEDIATE, PunishmentType.EARNINGS),
        Triple(OicSanctionCode.EXTW, Status.IMMEDIATE, PunishmentType.EXTRA_WORK),
        Triple(OicSanctionCode.EXTRA_WORK, Status.IMMEDIATE, PunishmentType.EXCLUSION_WORK),
      ).stream()

    @JvmStatic
    fun privilegeMappings(): Stream<Triple<OicSanctionCode, String, PunishmentType>> =
      listOf(
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.CANTEEN.name, PunishmentType.PRIVILEGE),
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.ASSOCIATION.name, PunishmentType.PRIVILEGE),
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.FACILITIES.name, PunishmentType.PRIVILEGE),
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.MONEY.name, PunishmentType.PRIVILEGE),
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.TV.name, PunishmentType.PRIVILEGE),
      ).stream()
  }
}
