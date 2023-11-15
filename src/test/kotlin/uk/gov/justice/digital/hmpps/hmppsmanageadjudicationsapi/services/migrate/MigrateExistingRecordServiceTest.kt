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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedOffence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Finding
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicSanctionCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.Status
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.OffenceCodes
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
      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      ).also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }

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
          it.status = ReportedAdjudicationStatus.ACCEPTED
          it.addPunishment(
            punishment = Punishment(
              id = 2,
              type = PunishmentType.CAUTION,
              sanctionSeq = dto.punishments.first().sanctionSeq,
              schedule = mutableListOf(),
            ),
          )
          it.hearings.clear()
          it.hearings.add(
            Hearing(
              id = 2,
              dateTimeOfHearing = LocalDateTime.now(),
              locationId = 1,
              oicHearingType = OicHearingType.GOV_ADULT,
              agencyId = "",
              chargeNumber = "",
              oicHearingId = 1,
            ),
          )
        },
      )

      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      ).also {
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

      assertThat(argumentCaptor.value.disIssueHistory).isEmpty()
      assertThat(argumentCaptor.value.dateTimeOfIssue).isEqualTo(dto.disIssued.first().dateTimeOfIssue)
      assertThat(argumentCaptor.value.issuingOfficer).isEqualTo(dto.disIssued.first().issuingOfficer)

      assertThat(response.hearingMappings).isNotEmpty
      assertThat(response.hearingMappings!!.first().hearingId).isEqualTo(2)
      assertThat(response.hearingMappings!!.first().oicHearingId).isEqualTo(1)
    }

    @Test
    fun `damages are updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      )
        .also { it.status = ReportedAdjudicationStatus.ACCEPTED }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages.size).isEqualTo(2)
      assertThat(argumentCaptor.value.damages.last().migrated).isTrue
    }

    @Test
    fun `evidence is updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      ).also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence.size).isEqualTo(2)
      assertThat(argumentCaptor.value.evidence.last().migrated).isTrue
    }

    @Test
    fun `witnesses are updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      ).also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses.size).isEqualTo(2)
      assertThat(argumentCaptor.value.witnesses.last().migrated).isTrue
    }

    @Test
    fun `existing record has multiple offences, adds presented offence and clears rest`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(
        chargeNumber = dto.oicIncidentId.toString(),
        prisonerNumber = dto.prisoner.prisonerNumber,
        agencyId = dto.agencyId,
      ).also {
        it.status = ReportedAdjudicationStatus.ACCEPTED
        it.offenceDetails.add(ReportedOffence(offenceCode = 9999))
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.size).isEqualTo(1)
      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(OffenceCodes.MIGRATED_OFFENCE.uniqueOffenceCodes.first())
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
    fun `existing hearing outcome with code NOMIS adjourns empty results and completes nomis outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.PHASE2_HEARINGS_NO_RESULTS

      migrateExistingRecordService.accept(dto, existing(dto))
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @CsvSource("NOT_PROCEED", "D", "DISMISSED")
    @ParameterizedTest
    fun `existing record with multiple nomis outcomes will correct outcome if it has changed to negative outcome if sanctions do not exist in nomis`(finding: Finding) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.PHASE2_HEARINGS_BAD_STRUCTURE(finding, false)
      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()))
          it.addPunishment(
            Punishment(
              type = PunishmentType.CAUTION,
              schedule = mutableListOf(
                PunishmentSchedule(days = 1),
              ),
            ),
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(3)
      assertThat(argumentCaptor.value.getPunishments()).isEmpty()
      assertThat(argumentCaptor.value.hearings.maxByOrNull { it.dateTimeOfHearing }!!.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(
        when (finding) {
          Finding.D -> OutcomeCode.DISMISSED
          Finding.NOT_PROCEED, Finding.DISMISSED -> OutcomeCode.NOT_PROCEED
          else -> null
        },
      )
    }

    @Test
    fun `completed existing record with a new outcome of PROVED will adjourn previous hearing and accept latest as charge proved`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val dto = migrationFixtures.PHASE2_HEARINGS_BAD_STRUCTURE(Finding.PROVED)
      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
        }.also {
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()))
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.minByOrNull { it.dateTimeOfHearing }!!.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.maxByOrNull { it.dateTimeOfHearing }!!.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @CsvSource("true", "false")
    @ParameterizedTest
    fun `completed existing record with a new outcome of APPEAL will add quashed or not based on reducedSanctions`(hasReducedSanctions: Boolean) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val dto = migrationFixtures.PHASE2_HEARINGS_BAD_STRUCTURE(finding = Finding.APPEAL, withSanctions = false, hasReducedSanctions = hasReducedSanctions)
      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
        }.also {
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()))
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(if (hasReducedSanctions) OutcomeCode.CHARGE_PROVED else OutcomeCode.QUASHED)
      if (hasReducedSanctions) {
        assertThat(argumentCaptor.value.punishmentComments.first().comment).isEqualTo("Reduced on APPEAL")
        assertThat(argumentCaptor.value.punishmentComments.first().nomisCreatedBy).isEqualTo(dto.punishments.first().createdBy)
      }
    }

    @MethodSource("uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordServiceTest#getExceptionCases")
    @ParameterizedTest
    fun `adjudications with multiple final states should adjourn and add comments, and use final outcome`(dto: AdjudicationMigrateDto) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.clear()
          dto.hearings.forEach {
              hearing ->
            it.hearings.add(
              Hearing(
                oicHearingId = hearing.oicHearingId,
                dateTimeOfHearing = hearing.hearingDateTime,
                oicHearingType = OicHearingType.GOV_ADULT,
                agencyId = "MDI",
                locationId = 1,
                chargeNumber = dto.oicIncidentId.toString(),
                hearingOutcome = HearingOutcome(code = HearingOutcomeCode.NOMIS, adjudicator = ""),
              ),
            )
          }
        },
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      val lastOutcome = dto.hearings.maxByOrNull { it.hearingDateTime }

      when (lastOutcome?.hearingResult?.finding) {
        Finding.PROVED.name -> assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
        Finding.D.name -> assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.DISMISSED)
        else -> {}
      }

      argumentCaptor.value.hearings.filter { it.dateTimeOfHearing != lastOutcome?.hearingDateTime }.forEach {
        assertThat(it.hearingOutcome?.code).isEqualTo(HearingOutcomeCode.ADJOURN)
        assertThat(it.hearingOutcome?.details).isNotEmpty()
      }
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

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
    }

    @CsvSource("3773547", "3892422", "3823250")
    @ParameterizedTest
    fun `nomis locked records with corrupted results, adjourn the dps hearing outcome`(chargeNumber: String) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_WITH_NOT_PROCEED_DUPLICATE
      val existing = existing(dto).also {
        it.chargeNumber = chargeNumber
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
    }

    @CsvSource("3871590", "3864251", "3899085")
    @ParameterizedTest
    fun `nomis locked records with corrupted results charge proved outcome, adjourn the dps hearing outcome`(chargeNumber: String) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_WITH_CHARGE_PROVED
      val existing = existing(dto).also {
        it.chargeNumber = chargeNumber
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.details).isEqualTo("entered in error - actual finding NOT_PROCEED")
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
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

    @Test
    fun `ignores empty hearings list`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE
      val existing = existing(dto).also {
        it.hearings.clear()
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.isEmpty()).isTrue
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

    @CsvSource("NOT_PROCEED", "D", "DISMISSED")
    @ParameterizedTest
    fun `existing record with multiple nomis outcomes will accept as corrupted if new outcome has changed to negative outcome if sanctions exist in nomis`(finding: Finding) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.PHASE2_HEARINGS_BAD_STRUCTURE(finding)
      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(
        when (finding) {
          Finding.NOT_PROCEED, Finding.DISMISSED -> OutcomeCode.NOT_PROCEED
          else -> OutcomeCode.DISMISSED
        },
      )
      assertThat(argumentCaptor.value.getPunishments()).isNotEmpty
    }

    @Test
    fun `hearing no longer exists removes hearing in DPS`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome = null
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings).isEmpty()
    }

    @Test
    fun `hearing no longer exists removes hearing and outcome in DPS`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_INAD).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings).isEmpty()
      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
    }

    @Test
    fun `hearing no longer exists removes hearing, outcome and referral outcome in DPS`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_INAD).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings).isEmpty()
      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
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

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(dto.hearings.first().locationId)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(dto.hearings.first().oicHearingType)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(dto.hearings.first().hearingDateTime)
    }

    @Test
    fun `hearing result no longer exists in nomis removes hearing result and outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNull()
      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
    }

    @Test
    fun `hearing result no longer exists in nomis removes hearing result, outcome and referral outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "")
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_POLICE).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNull()
      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
    }

    @Test
    fun `hearing result adjudicator has changed`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "someone")
      }

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo(dto.hearings.first().adjudicator)
    }

    @Disabled("removed for now - live data should not throw it, but need to throw still")
    @Test
    fun `hearing result code has changed throws exception`() {
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_POLICE, adjudicator = "someone")
      }

      Assertions.assertThatThrownBy {
        migrateExistingRecordService.accept(dto, existing)
      }.isInstanceOf(ExistingRecordConflictException::class.java)
    }

    @Test
    fun `hearing result code has changed from adjourn to completed - update hearing and add outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "someone")
      }
      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @Test
    fun `hearing result code has changed from adjourned to refer police, and final outcome is charge proved, should not update adjourned`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT_REF_POLICE
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.ADJOURN, adjudicator = "someone")
        it.hearings.add(
          Hearing(
            dateTimeOfHearing = LocalDateTime.now().plusDays(1),
            agencyId = "",
            oicHearingId = 101,
            locationId = 1,
            chargeNumber = "",
            oicHearingType = OicHearingType.GOV_ADULT,
            hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = ""),
          ),
        )
        it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
        it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      }
      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @CsvSource("REFER_POLICE", "REFER_GOV", "REFER_INAD")
    @ParameterizedTest
    fun `hearing result code has changed from refer to outcome - update hearing and add outcome`(hearingOutcomeCode: HearingOutcomeCode) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = hearingOutcomeCode, adjudicator = "someone")
        it.clearOutcomes()
        it.addOutcome(Outcome(id = 1, code = hearingOutcomeCode.outcomeCode!!))
      }
      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @CsvSource("REFER_GOV", "REFER_INAD")
    @ParameterizedTest
    fun `hearing result code has changed from refer to outcome - update hearing and add referral outcome`(hearingOutcomeCode: HearingOutcomeCode) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.WITH_HEARING_AND_REFERRAL_RESULT
      val existing = existing(dto).also {
        it.hearings.first().oicHearingId = dto.hearings.first().oicHearingId
        it.hearings.first().hearingOutcome = HearingOutcome(code = hearingOutcomeCode, adjudicator = "someone")
        it.clearOutcomes()
        it.addOutcome(
          Outcome(id = 1, code = hearingOutcomeCode.outcomeCode!!).also {
            it.createDateTime = LocalDateTime.now()
          },
        )
      }
      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first { it.id == 1L }.code).isEqualTo(hearingOutcomeCode.outcomeCode!!)
      assertThat(argumentCaptor.value.getOutcomes().first { it.id == null }.code).isEqualTo(OutcomeCode.NOT_PROCEED)
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
    fun `forfeit to privilege mapping`(toTest: Triple<OicSanctionCode, String, PunishmentType>) {
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
    fun `removes existing hearing if no outcome, and nomis has a new hearing with outcome PROVED after this hearing`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_BEFORE_LATEST_WITH_RESULT

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.last().hearingOutcome = null
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @Test
    fun `removes existing hearing if no outcome, and nomis has a new hearing with outcome QUASHED after this hearing`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_BEFORE_LATEST_WITH_RESULT_QUASHED

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.last().hearingOutcome = null
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
    }

    @Test
    fun `accepts as corrupted if a new hearing before latest, and latest has a different hearing outcome`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_BEFORE_LATEST_WITH_RESULT_EXCEPTION(finding = Finding.D)
      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.DISMISSED)
    }

    @CsvSource("PROVED", "D")
    @ParameterizedTest
    fun `with a adjourned result after nomis accepts nomis result and removes DPS hearing`(finding: Finding) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_BEFORE_LATEST_WITH_RESULT_EXCEPTION(finding = finding, withResult = false)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.last().hearingOutcome!!.code = HearingOutcomeCode.ADJOURN
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(if (finding == Finding.PROVED) OutcomeCode.CHARGE_PROVED else OutcomeCode.DISMISSED)
    }

    @CsvSource("PROVED", "D", "DISMISSED", "NOT_PROCEED", "REF_POLICE")
    @ParameterizedTest
    fun `adjourns result if a new hearing before latest, and latest has a same hearing outcome`(finding: Finding) {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.HEARING_BEFORE_LATEST_WITH_RESULT_EXCEPTION(finding = finding)
      val outcomeCode = when (finding) {
        Finding.DISMISSED, Finding.NOT_PROCEED -> OutcomeCode.NOT_PROCEED
        Finding.D -> OutcomeCode.DISMISSED
        Finding.PROVED -> OutcomeCode.CHARGE_PROVED
        Finding.REF_POLICE -> OutcomeCode.REFER_POLICE
        else -> OutcomeCode.QUASHED
      }

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome!!.code = if (finding == Finding.REF_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE
          it.addOutcome(Outcome(code = outcomeCode, actualCreatedDate = LocalDateTime.now()))
        },
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.minBy { it.dateTimeOfHearing }.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.maxBy { it.dateTimeOfHearing }.hearingOutcome!!.code).isEqualTo(if (finding == Finding.REF_POLICE) HearingOutcomeCode.REFER_POLICE else HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(outcomeCode)
    }

    @Test
    fun `adjourns hearing if before latest if it has no result`() {
      val dto = migrationFixtures.HEARING_BEFORE_LATEST
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.last().hearingOutcome = null
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.hearings.minByOrNull { it.dateTimeOfHearing }!!.hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
    }

    @Test
    fun `existing record that is completed, with additional hearings ignores the empty hearings`() {
      val dto = migrationFixtures.NEW_HEARING_AFTER_COMPLETED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.last().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
    }

    @Test
    fun `remove duplicate not proceed records`() {
      val dto = migrationFixtures.NOT_PROCEED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.clear()
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
    }

    @Test
    fun `dps is set as not proceed no hearing, and nomis record has hearings - remove DPS record and replace with nomis`() {
      val dto = migrationFixtures.NOT_PROCEED_REPLACE_WITH_NOMIS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.clear()
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
    }

    @Test
    fun `dps hearing and outcome set to not proceed, followed by a nomis outcome of charge proved`() {
      val dto = migrationFixtures.NOT_PROCEED_CHARGE_PROVED_REPLACE_WITH_NOMIS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
    }

    @Test
    fun `charge 3879400 both dps and nomis have seperate dismissed outcomes - ignore nomis`() {
      val dto = migrationFixtures.CHARGE_3879400
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.chargeNumber = "3879400"
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.DISMISSED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
    }

    @Test
    fun `charge 3990011 has two charge proved due to back usage set as charge proved `() {
      val dto = migrationFixtures.CHARGE_3990011
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.chargeNumber = "3990011"
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.ADJOURN
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
    }

    @CsvSource("3944645", "3963134", "3947329", "3944650")
    @ParameterizedTest
    fun `charges where refer inad has been given an outcome in nomis ignore nomis not proceed`(chargeNumber: String) {
      val dto = migrationFixtures.REFER_INAD_GIVEN_NOMIS_OUTCOME
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.chargeNumber = chargeNumber
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.REFER_INAD
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_INAD).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED).also {
              it.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `3851533 charge proved quashed duplication - keep DPS record only, but add sanctions`() {
      val dto = migrationFixtures.CHARGE_3851533
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateExistingRecordService.accept(
        dto,
        existing(dto).also {
          it.chargeNumber = "3851533"
          it.hearings.first().hearingOutcome!!.code = HearingOutcomeCode.COMPLETE
          it.clearOutcomes()
          it.addOutcome(
            Outcome(code = OutcomeCode.CHARGE_PROVED).also {
              it.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.QUASHED).also {
              it.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
      assertThat(argumentCaptor.value.getPunishments()).isNotEmpty
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
        Triple(OicSanctionCode.FORFEIT, PrivilegeType.GYM.name, PunishmentType.PRIVILEGE),
      ).stream()
  }
}
