package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
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
      assertThat(argumentCaptor.value.draftCreatedOn).isEqualTo(dto.incidentDateTime)
      assertThat(argumentCaptor.value.prisonerNumber).isEqualTo(dto.prisoner.prisonerNumber)
      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.MALE)
      assertThat(argumentCaptor.value.isYouthOffender).isEqualTo(false)
      assertThat(argumentCaptor.value.statement).isEqualTo(dto.statement)
      assertThat(argumentCaptor.value.migrated).isEqualTo(true)
      assertThat(argumentCaptor.value.lastModifiedAgencyId).isNull()
      assertThat(argumentCaptor.value.handoverDeadline).isEqualTo(DraftAdjudicationService.daysToActionFromIncident(dto.incidentDateTime))
      assertThat(argumentCaptor.value.dateTimeOfIssue).isNull()
      assertThat(argumentCaptor.value.statusDetails).isNull()
      assertThat(argumentCaptor.value.statusReason).isNull()
      // assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(17002)
      assertThat(argumentCaptor.value.offenceDetails.first().victimStaffUsername).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimOtherPersonsName).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().victimPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.offenceDetails.first().additionalVictims).isEmpty()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersName).isNull()
      assertThat(argumentCaptor.value.incidentRoleAssociatedPrisonersNumber).isNull()
      assertThat(argumentCaptor.value.additionalAssociates).isEmpty()
      // assertThat(argumentCaptor.value.incidentRoleCode).isNull()
      assertThat(argumentCaptor.value.damages).isEmpty()
      assertThat(argumentCaptor.value.evidence).isEmpty()
      assertThat(argumentCaptor.value.witnesses).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)

      assertThat(response.chargeNumberMapping.chargeNumber).isEqualTo("${dto.oicIncidentId}-${dto.offenceSequence}")
      assertThat(response.chargeNumberMapping.offenceSequence).isEqualTo(dto.offenceSequence)
      assertThat(response.chargeNumberMapping.oicIncidentId).isEqualTo(dto.oicIncidentId)
    }

    @Disabled
    @Test
    fun `process with others offence`() {
      val dto = migrationFixtures.OFFENCE_WITH_OTHERS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      migrateNewRecordService.accept(dto)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.offenceDetails.first().offenceCode).isEqualTo(1002)
      assertThat(argumentCaptor.value.incidentRoleCode).isEqualTo("25b")
    }

    @Disabled
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

    @Disabled
    @Test
    fun `process offence not on file throws exception`() {
      val dto = migrationFixtures.OFFENCE_NOT_ON_FILE

      Assertions.assertThatThrownBy {
        migrateNewRecordService.accept(dto)
      }.isInstanceOf(UnableToMigrateException::class.java)
        .hasMessageContaining("the offence code ${dto.offence.offenceCode} is unknown")
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

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
