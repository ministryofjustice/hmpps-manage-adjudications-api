package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
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

      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(0)
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceCode).isEqualTo(dto.offence.offenceCode)
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceDescription).isEqualTo(dto.offence.offenceDescription)
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
      assertThat(argumentCaptor.value.getOutcomes()).isNotEmpty
      assertThat(argumentCaptor.value.hearings).isNotEmpty

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
    }

    @Test
    fun `evidence is updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence.size).isEqualTo(2)
    }

    @Test
    fun `witnesses are updated`() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val dto = migrationFixtures.COMPLETE_CHARGE_PROVED
      val existing = entityBuilder.reportedAdjudication(chargeNumber = dto.oicIncidentId.toString(), prisonerNumber = dto.prisoner.prisonerNumber, agencyId = dto.agencyId)

      migrateExistingRecordService.accept(dto, existing)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses.size).isEqualTo(2)
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
