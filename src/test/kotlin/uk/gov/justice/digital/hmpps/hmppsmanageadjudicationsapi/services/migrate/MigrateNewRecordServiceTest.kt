package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateNewRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateNewRecordService = MigrateNewRecordService(reportedAdjudicationRepository)

  @Nested
  inner class CoreAdjudication {

    @Test
    fun `process adult`() {
      val dto = migrationFixtures.ADULT_SINGLE_OFFENCE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.agencyIncidentId).isEqualTo(dto.agencyIncidentId)
      assertThat(argumentCaptor.value.chargeNumber).isEqualTo(dto.oicIncidentId.toString())
      assertThat(argumentCaptor.value.originatingAgencyId).isEqualTo(dto.agencyId)
      assertThat(argumentCaptor.value.overrideAgencyId).isNull()
      assertThat(argumentCaptor.value.locationId).isEqualTo(dto.locationId)
      assertThat(argumentCaptor.value.dateTimeOfIncident).isEqualTo(dto.incidentDateTime)
      assertThat(argumentCaptor.value.dateTimeOfDiscovery).isEqualTo(dto.incidentDateTime)
      assertThat(argumentCaptor.value.prisonerNumber).isEqualTo(dto.prisoner.prisonerNumber)
      assertThat(argumentCaptor.value.gender).isEqualTo(Gender.MALE)
    }

    @Test
    fun `process multiple offences on adjudication` () {
      val dto = migrationFixtures.ADULT_MULITPLE_OFFENCES.first()
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    }

    @Test
    fun `process yoi`() {
      val dto = migrationFixtures.YOUTH_SINGLE_OFFENCE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process transfer`() {
      val dto = migrationFixtures.ADULT_TRANSFER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process non binary gender`() {
      val dto = migrationFixtures.NON_BINARY_GENDER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process unknown gender`() {
      val dto = migrationFixtures.UNKNOWN_GENDER
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with staff victim`() {
      val dto = migrationFixtures.WITH_STAFF_VICTIM
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with prisoner victim`() {
      val dto = migrationFixtures.WITH_PRISONER_VICTIM
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with associate`() {
      val dto = migrationFixtures.WITH_ASSOCIATE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process multiple associates`() {
      val dto = migrationFixtures.ADDITIONAL_ASSOCIATES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process multiple victims`() {
      val dto = migrationFixtures.ADDITIONAL_VICTIMS
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process offence not on file throws exception`() {
      val dto = migrationFixtures.OFFENCE_NOT_ON_FILE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with damages`() {
      val dto = migrationFixtures.WITH_DAMAGES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with witnesses`() {
      val dto = migrationFixtures.WITH_WITNESSES
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }

    @Test
    fun `process with evidence`() {
      val dto = migrationFixtures.WITH_EVIDENCE
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
    }
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
