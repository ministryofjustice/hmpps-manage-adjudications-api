package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.draft.DraftAdjudicationService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateNewRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateNewRecordService = MigrateNewRecordService(reportedAdjudicationRepository)

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
      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().additionalVictims).isEmpty()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersName).isNull()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.additionalAssociates).isEmpty()
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
    fun `process with staff victim`() {
      val dto = migrationFixtures.WITH_STAFF_VICTIM
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isEqualTo(dto.victims.first().victimIdentifier)
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isNull()
    }

    @Test
    fun `process with prisoner victim`() {
      val dto = migrationFixtures.WITH_PRISONER_VICTIM
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isEqualTo(dto.victims.first().victimIdentifier)
      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
    }

    @Test
    fun `process with associate`() {
      val dto = migrationFixtures.WITH_ASSOCIATE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersName).isNull()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersNumber).isEqualTo(dto.associates.first().associatedPrisoner)
    }

    @Test
    fun `process multiple associates`() {
      val dto = migrationFixtures.ADDITIONAL_ASSOCIATES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersName).isNull()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersNumber).isEqualTo(dto.associates.first().associatedPrisoner)
      assertThat(argumentCaptor.value.additionalAssociates.size).isEqualTo(dto.associates.size - 1)
      assertThat(argumentCaptor.value.additionalAssociates.first().incidentRoleAssociatedPrisonersNumber).isEqualTo(dto.associates.last().associatedPrisoner)
    }

    @Test
    fun `process multiple victims`() {
      val dto = migrationFixtures.ADDITIONAL_VICTIMS
      val copyOfVictims = dto.victims
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isEqualTo(copyOfVictims.first().victimIdentifier)
      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().additionalVictims.size).isEqualTo(dto.victims.size - 1)
      val victim2 = argumentCaptor.value.offenceDetails.first().additionalVictims[0].victimPrisonersNumber
      val victim3 = argumentCaptor.value.offenceDetails.first().additionalVictims[1].victimPrisonersNumber
      assertThat(copyOfVictims.firstOrNull { it.victimIdentifier == victim2 }).isNotNull
      assertThat(copyOfVictims.firstOrNull { it.victimIdentifier == victim3 }).isNotNull
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

  @Disabled
  @Nested
  inner class OffenceCodesAndRoles {

    @Test
    fun `process adult`() {
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(17002)
    }

    @Test
    fun `process with others offence`() {
      val dto = migrationFixtures.OFFENCE_WITH_OTHERS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(1002)
      assertThat(argumentCaptor.value.incidentRoleCode).isEqualTo("25b")
    }

    @Test
    fun `process with others and same offence code for all roles`() {
      val dto = migrationFixtures.OFFENCE_WITH_OTHERS_AND_SAME_CODE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(1001)
      assertThat(argumentCaptor.value.incidentRoleCode).isEqualTo("25b")
    }

    @Test
    fun `process offence not on file throws exception`() {
      val dto = migrationFixtures.OFFENCE_NOT_ON_FILE

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the offence code ${dto.offence.offenceCode} is unknown")
    }
  }

  @Nested
  inner class Punishments {

    @Test
    fun `process punishments - CAUTION`() {
      val dto = migrationFixtures.WITH_PUNISHMENT
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        entityBuilder.reportedAdjudication().also {
          it.addPunishment(
            punishment = Punishment(id = 2, type = PunishmentType.CAUTION, schedule = mutableListOf()),
          )
        },
      )

      val response = migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.CAUTION)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().days).isEqualTo(0)
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().suspendedUntil).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().startDate).isNull()
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isNull()
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
    fun `process punishments - CC`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_CC
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      assertThat(argumentCaptor.value.getPunishments().first().type).isEqualTo(PunishmentType.CONFINEMENT)
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
      assertThat(argumentCaptor.value.getPunishments().first().otherPrivilege).isEqualTo("see comment")
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
      assertThat(argumentCaptor.value.getPunishments().first().schedule.first().endDate).isEqualTo(dto.punishments.first().effectiveDate.plusDays(
        dto.punishments.first().days!!.toLong()
      ))
    }

    @Test
    fun `process punishments - CONSECUTIVE throws exception`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_CONSECUTIVE_INVALID

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the sanction code ${dto.punishments.first().sanctionCode} can not be consecutive")
    }

    @Test
    fun `process punishments - OTHER NO AMOUNT throws exception`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_DAMAGES_NO_AMOUNT

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the sanction code ${dto.punishments.first().sanctionCode} has no amount")
    }

    @Test
    fun `process punishments - invalid code throws exception`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_INVALID_CODE

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the sanction code ${dto.punishments.first().sanctionCode} is invalid")
    }

    @Test
    fun `process punishments - invalid status throws exception`() {
      val dto = migrationFixtures.WITH_PUNISHMENT_INVALID_STATUS

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the sanction status ${dto.punishments.first().sanctionStatus} is invalid")
    }

    @Test
    fun `should reject additional punishments if caution is set - to discuss`() {
      // give damages owed is other, perhaps safer to not do this.  TBC
    }

    @Test
    fun `what to do with OTHER cases` () {

    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
