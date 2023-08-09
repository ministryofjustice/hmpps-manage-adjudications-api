package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomePlea
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.LocalDateTime

class MigrateNewRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateNewRecordService = MigrateNewRecordService(reportedAdjudicationRepository)

  @BeforeEach
  fun `return save for audit`() {
    whenever(reportedAdjudicationRepository.save(any())).thenReturn(entityBuilder.reportedAdjudication().also { it.hearings.clear() })
  }

  @Nested
  inner class CoreAdjudication {

    @Test
    fun `process adult`() {
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.agencyIncidentId).isEqualTo(dto.agencyIncidentId)
      assertThat(argumentCaptor.value.chargeNumber).isEqualTo("${dto.oicIncidentId}-${dto.offenceSequence}")
      assertThat(argumentCaptor.value.originatingAgencyId).isEqualTo(dto.agencyId)
      assertThat(argumentCaptor.value.overrideAgencyId).isNull()
      assertThat(argumentCaptor.value.locationId).isEqualTo(dto.locationId)
      assertThat(argumentCaptor.value.dateTimeOfIncident).isEqualTo(dto.incidentDateTime)
      assertThat(argumentCaptor.value.dateTimeOfDiscovery).isEqualTo(dto.incidentDateTime)
      assertThat(argumentCaptor.value.prisonerNumber).isEqualTo(dto.prisoner.prisonerNumber)
      assertThat(argumentCaptor.value.offenderBookingId).isEqualTo(dto.bookingId)
      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.MALE)
      assertThat(argumentCaptor.value.isYouthOffender).isEqualTo(false)
      assertThat(argumentCaptor.value.statement).isEqualTo(dto.statement)
      assertThat(argumentCaptor.value.migrated).isEqualTo(true)
      assertThat(argumentCaptor.value.lastModifiedAgencyId).isNull()
      assertThat(argumentCaptor.value.handoverDeadline).isEqualTo(DraftAdjudicationService.daysToActionFromIncident(dto.incidentDateTime))
      assertThat(argumentCaptor.value.dateTimeOfIssue).isNull()
      assertThat(argumentCaptor.value.statusDetails).isNull()
      assertThat(argumentCaptor.value.statusReason).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(0)
      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceCode).isEqualTo(dto.offence.offenceCode)
      assertThat(argumentCaptor.value.offenceDetails.first().nomisOffenceDescription).isEqualTo(dto.offence.offenceDescription)
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersName).isNull()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.incidentRoleCode).isNull()
      assertThat(argumentCaptor.value.damages).isEmpty()
      assertThat(argumentCaptor.value.evidence).isEmpty()
      assertThat(argumentCaptor.value.witnesses).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)

      assertThat(response.chargeNumberMapping.chargeNumber).isEqualTo("${dto.oicIncidentId}-${dto.offenceSequence}")
      assertThat(response.chargeNumberMapping.offenceSequence).isEqualTo(dto.offenceSequence)
      assertThat(response.chargeNumberMapping.oicIncidentId).isEqualTo(dto.oicIncidentId)
    }

    @Test
    fun `process yoi`() {
      val dto = migrationFixtures.YOUTH_SINGLE_OFFENCE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.isYouthOffender).isEqualTo(true)
    }

    @Test
    fun `process transfer`() {
      val dto = migrationFixtures.ADULT_TRANSFER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.overrideAgencyId).isEqualTo(dto.prisoner.currentAgencyId)
    }

    @Test
    fun `process non binary gender`() {
      val dto = migrationFixtures.NON_BINARY_GENDER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.MALE)
    }

    @Test
    fun `process unknown gender`() {
      val dto = migrationFixtures.UNKNOWN_GENDER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.MALE)
    }

    @Test
    fun `process female`() {
      val dto = migrationFixtures.FEMALE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.FEMALE)
    }

    @Test
    fun `process with damages`() {
      val dto = migrationFixtures.WITH_DAMAGES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages).isNotEmpty
      assertThat(argumentCaptor.value.damages.first().code).isEqualTo(dto.damages.first().damageType)
      assertThat(argumentCaptor.value.damages.first().details).isEqualTo(dto.damages.first().details)
      assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo(dto.damages.first().createdBy)
    }

    @Test
    fun `process with damages and no details`() {
      val dto = migrationFixtures.WITH_DAMAGES_AND_NO_DETAIL
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.damages).isNotEmpty
      assertThat(argumentCaptor.value.damages.first().code).isEqualTo(dto.damages.first().damageType)
      assertThat(argumentCaptor.value.damages.first().details).isEqualTo("No recorded details")
      assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo(dto.damages.first().createdBy)
    }

    @Test
    fun `process with witnesses`() {
      val dto = migrationFixtures.WITH_WITNESSES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.witnesses).isNotEmpty
      assertThat(argumentCaptor.value.witnesses.first().firstName).isEqualTo(dto.witnesses.first().firstName)
      assertThat(argumentCaptor.value.witnesses.first().lastName).isEqualTo(dto.witnesses.first().lastName)
      assertThat(argumentCaptor.value.witnesses.first().reporter).isEqualTo(dto.witnesses.first().createdBy)
    }

    @Test
    fun `process with evidence`() {
      val dto = migrationFixtures.WITH_EVIDENCE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.evidence).isNotEmpty
      assertThat(argumentCaptor.value.evidence.first().code).isEqualTo(dto.evidence.first().evidenceCode)
      assertThat(argumentCaptor.value.evidence.first().details).isEqualTo(dto.evidence.first().details)
      assertThat(argumentCaptor.value.evidence.first().reporter).isEqualTo(dto.evidence.first().reporter)
    }
  }

  @Nested
  inner class Punishments {

    @Test
    fun `process punishments - CC`() {
      val dto = migrationFixtures.WITH_PUNISHMENT
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(
            punishment = Punishment(id = 2, type = PunishmentType.CAUTION, sanctionSeq = dto.punishments.first().sanctionSeq, schedule = mutableListOf()),
          )
        },
      )

      val response = migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.CONFINEMENT)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().days).isEqualTo(dto.punishments.first().days)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().suspendedUntil).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isEqualTo(dto.punishments.first().effectiveDate)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isEqualTo(
        dto.punishments.first().effectiveDate.plusDays(dto.punishments.first().days!!.toLong()),
      )
      assertThat(argumentCaptor.value.getPunishments().first().privilegeType).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().otherPrivilege).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().stoppagePercentage).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().consecutiveChargeNumber).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedByChargeNumber).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().activatedFromChargeNumber).isNull()

      assertThat(response.punishmentMappings).isNotEmpty
      assertThat(response.punishmentMappings!!.first().punishmentId).isNotNull
      assertThat(response.punishmentMappings!!.first().sanctionSeq).isEqualTo(dto.punishments.first().sanctionSeq)
      assertThat(response.punishmentMappings!!.first().bookingId).isEqualTo(dto.bookingId)
    }

    @Test
    fun `process punishments - CAUTION`() {
      val dto = migrationFixtures.WITH_CAUTION
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.CAUTION)

      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().days).isEqualTo(0)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().suspendedUntil).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isNull()
    }

    @Test
    fun `process punishments - SUSPENDED`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_SUSPENDED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().suspendedUntil).isEqualTo(
        dto.punishments.first().effectiveDate,
      )
    }

    @Test
    fun `process punishments - EXTRA WORK`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_EXTRA_WORK
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.EXTRA_WORK)
    }

    @Test
    fun `process punishments - EXCLUSION WORK`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_EXCLUSION_WORK
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.EXCLUSION_WORK)
    }

    @Test
    fun `process punishments - REMOVAL WING`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_REMOVAL_WING
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.REMOVAL_WING)
    }

    @Test
    fun `process punishments - REMOVAL ACTIVITY`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_REMOVAL_ACTIVITY
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.REMOVAL_ACTIVITY)
    }

    @Test
    fun `process punishments - PRIVILEGE`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_PRIVILEGES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(argumentCaptor.value.getPunishments().first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(argumentCaptor.value.getPunishments().first().otherPrivilege).isEqualTo(dto.punishments.first().sanctionCode)
    }

    @Test
    fun `process punishments - ADA`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_ADA
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.ADDITIONAL_DAYS)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isNull()
    }

    @Test
    fun `process punishments - PROSPECTIVE ADA`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_PROSPECITVE_ADA
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.PROSPECTIVE_DAYS)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isNull()
    }

    @Test
    fun `process punishments - PROSPECTIVE ADA SUSPENDED`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_PROSPECITVE_ADA_SUSPENDED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.PROSPECTIVE_DAYS)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().suspendedUntil).isEqualTo(dto.punishments.first().effectiveDate)
    }

    @Disabled
    @Test
    fun `process punishments - DAMAGES`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_DAMAGES_AMOUNT
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.DAMAGES_OWED)
      assertThat(argumentCaptor.value.getPunishments().first().amount).isEqualTo(dto.punishments.first().compensationAmount?.toDouble())
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().days).isEqualTo(0)
    }

    @Test
    fun `process punishments - EARNINGS STOP PCT`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_STOPPAGE_PERCENTAGE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.EARNINGS)
      assertThat(argumentCaptor.value.getPunishments().first().stoppagePercentage).isEqualTo(dto.punishments.first().compensationAmount?.toInt())
    }

    @Test
    fun `process punishments - EARNINGS NO STOP PCT`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_EARNINGS_NO_STOPPAGE_PERCENTAGE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.EARNINGS)
      assertThat(argumentCaptor.value.getPunishments().first().stoppagePercentage).isNull()
    }

    @Test
    fun `process punishments - COMMENT`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_COMMENT
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.punishmentComments.first().comment).isEqualTo(dto.punishments.first().comment)
    }

    @Test
    fun `process punishments - CONSECUTIVE`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_CONSECUTIVE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().consecutiveChargeNumber).isEqualTo(dto.punishments.first().consecutiveChargeNumber)
    }

    @Test
    fun `process punishments - WITH START DATE`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_START_DATE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isEqualTo(dto.punishments.first().effectiveDate)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isEqualTo(
        dto.punishments.first().effectiveDate.plusDays(
          dto.punishments.first().days!!.toLong(),
        ),
      )
    }

    @Disabled
    @Test
    fun `process punishments - OTHER NO AMOUNT throws exception`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_DAMAGES_NO_AMOUNT

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the sanction code ${dto.punishments.first().sanctionCode} has no amount")
    }

    @Test
    fun `should reject additional punishments if caution is set - to discuss - most likely accept them?`() {
      // give damages owed is other, perhaps safer to not do this.  TBC
    }

    @Test
    fun `any case that is not known will be mapped to PRIVILEGE `() {
      val dto = migrationFixtures.WITH_PUNISHMENT_UNKNOWN_CODE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.PRIVILEGE)
      assertThat(argumentCaptor.value.getPunishments().first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      assertThat(argumentCaptor.value.getPunishments().first().otherPrivilege).isEqualTo(dto.punishments.first().sanctionCode)
    }
  }

  @Nested
  inner class HearingsAndResults {

    @Test
    fun `single hearing no result`() {
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.hearings.clear()
          it.hearings.add(
            Hearing(id = 2, dateTimeOfHearing = LocalDateTime.now(), locationId = 1, oicHearingType = OicHearingType.GOV_ADULT, agencyId = "", chargeNumber = "", oicHearingId = 1),
          )
        },
      )

      val dto = migrationFixtures.WITH_HEARING
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(dto.hearings.first().hearingDateTime)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(dto.hearings.first().locationId)
      assertThat(argumentCaptor.value.hearings.first().chargeNumber).isEqualTo("${dto.oicIncidentId}-${dto.offenceSequence}")
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(dto.agencyId)
      assertThat(argumentCaptor.value.hearings.first().oicHearingId).isEqualTo(dto.hearings.first().oicHearingId)
      assertThat(argumentCaptor.value.hearings.first().oicHearingType).isEqualTo(dto.hearings.first().oicHearingType)

      assertThat(response.hearingMappings).isNotEmpty
      assertThat(response.hearingMappings!!.first().hearingId).isEqualTo(2)
      assertThat(response.hearingMappings!!.first().oicHearingId).isEqualTo(1)
    }

    @Test
    fun `single hearing with result - CHARGE_PROVED `() {
      val dto = migrationFixtures.WITH_HEARING_AND_RESULT
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo(dto.hearings.first().adjudicator)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.NOT_GUILTY)

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().first().actualCreatedDate).isEqualTo(dto.hearings.first().hearingResult!!.createdDateTime)
    }

    @Test
    fun `single hearing with result - DISMISSED `() {
      val dto = migrationFixtures.WITH_HEARING_AND_DISMISSED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
    }

    @Test
    fun `single hearing with result - NOT PROCEED `() {
      val dto = migrationFixtures.WITH_HEARING_AND_NOT_PROCEED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.COMPLETE)

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `single hearing with result - REFER POLICE `() {
      val dto = migrationFixtures.WITH_HEARING_AND_REFER_POLICE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
    }

    @Test
    fun `single hearing with result - PROSECUTION `() {
      val dto = migrationFixtures.WITH_HEARING_AND_PROSCUTION
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)

      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.PROSECUTION)
      assertThat(argumentCaptor.value.getOutcomes().last().actualCreatedDate).isEqualTo(dto.hearings.first().hearingResult!!.createdDateTime.plusMinutes(1))
    }

    @Test
    fun `REFER_POLICE - SCHEDULE HEARING `() {
      val dto = migrationFixtures.POLICE_REFERRAL_NEW_HEARING
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.last()).isNotNull

      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(argumentCaptor.value.getOutcomes().last().actualCreatedDate).isEqualTo(dto.hearings.first().hearingResult!!.createdDateTime.plusMinutes(1))
    }

    @Test
    fun `multiple hearings and no results will adjourn hearing`() {
      val dto = migrationFixtures.WITH_HEARINGS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome).isNotNull
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.code).isEqualTo(HearingOutcomeCode.ADJOURN)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.adjudicator).isEqualTo(dto.hearings.first().adjudicator)
      assertThat(argumentCaptor.value.hearings.first().hearingOutcome!!.plea).isEqualTo(HearingOutcomePlea.NOT_ASKED)
      assertThat(argumentCaptor.value.hearings.last().hearingOutcome).isNull()
    }

    /*
       for the below, really need to get some stats together for prod queries.  ideally get Andy to run them.
     */
    @Test
    fun `multiple hearings and multiple results`() {
    }

    @Test
    fun `multiple hearing with results - REFER_POLICE,PROSECUTION `() {
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
