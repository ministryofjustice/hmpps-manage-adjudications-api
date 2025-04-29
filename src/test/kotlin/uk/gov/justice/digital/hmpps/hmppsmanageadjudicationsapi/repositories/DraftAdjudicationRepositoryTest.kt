package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Pageable
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.AuditConfiguration
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Characteristic
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Damage
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DraftProtectedCharacteristics
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Evidence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Gender
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentDetails
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentRole
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.IncidentStatement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Offence
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Witness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.UserDetails
import java.time.LocalDateTime
import java.util.*

@DataJpaTest
@ActiveProfiles("jpa")
@WithMockUser(username = "ITAG_USER", authorities = ["ROLE_VIEW_ADJUDICATIONS"])
@Import(AuditConfiguration::class, UserDetails::class)
class DraftAdjudicationRepositoryTest {
  @Autowired
  lateinit var entityManager: TestEntityManager

  @Autowired
  lateinit var draftAdjudicationRepository: DraftAdjudicationRepository

  @Test
  fun `save a new draft adjudication`() {
    val draft = newDraft()

    val savedEntity = entityManager.persistAndFlush(draft)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "agencyId", "createdByUserId", "gender")
      .contains(savedEntity.id, draft.prisonerNumber, draft.agencyId, "ITAG_USER", Gender.MALE)

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident", "handoverDeadline")
      .contains(
        draft.incidentDetails.locationId,
        draft.incidentDetails.dateTimeOfIncident,
        draft.incidentDetails.handoverDeadline,
      )

    assertThat(savedEntity.incidentRole)
      .extracting("roleCode", "associatedPrisonersNumber", "associatedPrisonersName")
      .contains(
        draft.incidentRole!!.roleCode,
        draft.incidentRole!!.associatedPrisonersNumber,
        draft.incidentRole!!.associatedPrisonersName,
      )
  }

  @Test
  fun `save a new draft adjudication with all data`() {
    val draft = draftWithAllData()
    val savedEntity = draftAdjudicationRepository.save(draft)

    assertThat(savedEntity)
      .extracting("id", "prisonerNumber", "agencyId", "createdByUserId")
      .contains(savedEntity.id, draft.prisonerNumber, draft.agencyId, "ITAG_USER")

    assertThat(savedEntity.incidentDetails)
      .extracting("locationId", "dateTimeOfIncident", "dateTimeOfDiscovery", "handoverDeadline")
      .contains(
        draft.incidentDetails.locationId,
        draft.incidentDetails.dateTimeOfIncident,
        draft.incidentDetails.dateTimeOfIncident.plusDays(1),
        draft.incidentDetails.handoverDeadline,
      )

    assertThat(savedEntity.incidentRole)
      .extracting("roleCode", "associatedPrisonersNumber", "associatedPrisonersName")
      .contains(
        draft.incidentRole!!.roleCode,
        draft.incidentRole!!.associatedPrisonersNumber,
        draft.incidentRole!!.associatedPrisonersName,
      )

    assertThat(savedEntity.offenceDetails).hasSize(2)
      .extracting(
        "offenceCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName",
      )
      .contains(
        Tuple(
          draft.offenceDetails.first().offenceCode,
          draft.offenceDetails.first().victimPrisonersNumber,
          draft.offenceDetails.first().victimStaffUsername,
          draft.offenceDetails.first().victimOtherPersonsName,
        ),
        Tuple(
          draft.offenceDetails.last().offenceCode,
          draft.offenceDetails.last().victimPrisonersNumber,
          draft.offenceDetails.last().victimStaffUsername,
          draft.offenceDetails.last().victimOtherPersonsName,
        ),
      )

    assertThat(savedEntity.damagesSaved).isEqualTo(true)

    assertThat(savedEntity.damages).hasSize(1)
      .extracting(
        "code",
        "details",
      )
      .contains(
        Tuple(
          draft.damages.first().code,
          draft.damages.first().details,
        ),
      )

    assertThat(savedEntity.evidenceSaved).isEqualTo(true)

    assertThat(savedEntity.evidence).hasSize(1)
      .extracting(
        "code",
        "details",
      )
      .contains(
        Tuple(
          draft.evidence.first().code,
          draft.evidence.first().details,
        ),
      )

    assertThat(savedEntity.witnessesSaved).isEqualTo(true)

    assertThat(savedEntity.witnesses).hasSize(1)
      .extracting(
        "code",
        "firstName",
        "lastName",
      )
      .contains(
        Tuple(
          draft.witnesses.first().code,
          draft.witnesses.first().firstName,
          draft.witnesses.first().lastName,
        ),
      )

    assertThat(savedEntity.incidentStatement)
      .extracting("statement", "completed")
      .contains(draft.incidentStatement?.statement, draft.incidentStatement?.completed)
  }

  @Test
  fun `update an existing draft adjudication with modified offences`() {
    val draftWithAllData = draftWithAllData()
    val updatedDraft = draftWithAllData.copy(
      offenceDetails = mutableListOf(
        Offence(
          offenceCode = 4,
          victimPrisonersNumber = "B2345BB",
          victimStaffUsername = "ABC12D",
          victimOtherPersonsName = "Someones Name Here",
        ),
      ),
    )
    val savedEntity = draftAdjudicationRepository.save(updatedDraft)

    assertThat(savedEntity.offenceDetails).hasSize(1)
      .extracting(
        "offenceCode",
        "victimPrisonersNumber",
        "victimStaffUsername",
        "victimOtherPersonsName",
      )
      .contains(
        Tuple(
          updatedDraft.offenceDetails.first().offenceCode,
          updatedDraft.offenceDetails.first().victimPrisonersNumber,
          updatedDraft.offenceDetails.first().victimStaffUsername,
          updatedDraft.offenceDetails.first().victimOtherPersonsName,
        ),
      )
  }

  @Test
  fun `find draft adjudications`() {
    val dateTimeOfIncident = LocalDateTime.now()

    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12345",
        gender = Gender.MALE,
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 2,
          locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2f"),
          dateTimeOfIncident = dateTimeOfIncident,
          dateTimeOfDiscovery = dateTimeOfIncident.plusDays(1),
          handoverDeadline = dateTimeOfIncident.plusDays(2),
        ),
        incidentRole = IncidentRole(
          roleCode = "25a",
          associatedPrisonersNumber = "B23456",
          associatedPrisonersName = "Associated Prisoner",
        ),
        isYouthOffender = true,
      ),
    )
    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12346",
        gender = Gender.MALE,
        agencyId = "LEI",
        incidentDetails = IncidentDetails(
          locationId = 2,
          locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2f"),
          dateTimeOfIncident = dateTimeOfIncident,
          dateTimeOfDiscovery = dateTimeOfIncident,
          handoverDeadline = dateTimeOfIncident.plusDays(2),
        ),
        incidentRole = IncidentRole(
          roleCode = null,
          associatedPrisonersNumber = null,
          associatedPrisonersName = null,
        ),
        isYouthOffender = true,
      ),
    )
    entityManager.persistAndFlush(
      DraftAdjudication(
        prisonerNumber = "A12347",
        gender = Gender.MALE,
        chargeNumber = "123",
        reportByUserId = "A_SMITH",
        agencyId = "MDI",
        incidentDetails = IncidentDetails(
          locationId = 3,
          locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
          dateTimeOfIncident = dateTimeOfIncident,
          dateTimeOfDiscovery = dateTimeOfIncident.plusDays(1),
          handoverDeadline = dateTimeOfIncident.plusDays(3),
        ),
        incidentRole = IncidentRole(
          roleCode = null,
          associatedPrisonersNumber = null,
          associatedPrisonersName = null,
        ),
        isYouthOffender = true,
      ),
    )
    val foundAdjudications =
      draftAdjudicationRepository.findByAgencyIdAndCreatedByUserIdAndChargeNumberIsNullAndIncidentDetailsDateTimeOfDiscoveryBetween(
        "MDI",
        "ITAG_USER",
        dateTimeOfIncident.minusDays(1),
        dateTimeOfIncident.plusDays(2),
        Pageable.unpaged(),
      )

    assertThat(foundAdjudications).hasSize(1)
      .extracting("prisonerNumber")
      .contains(
        "A12345",
      )
  }

  @Test
  fun `delete orphaned adjudications should do nothing if they are not old enough`() {
    val deleteBefore = LocalDateTime.now()
    val draft = draftWithAllData("1")
    entityManager.persistAndFlush(draft)
    // We should not delete anything because the time we use was at the very beginning of the test, before we created anything.
    val deleted =
      draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndChargeNumberIsNotNull(deleteBefore)
    assertThat(deleted).hasSize(0)
  }

  @Test
  fun `delete orphaned adjudications should completely remove orphaned adjudications if they are old enough`() {
    val draft = draftWithAllData("1")
    entityManager.persistAndFlush(draft)
    val deleteBefore = LocalDateTime.now().plusSeconds(1)
    // We should delete the saved adjudication because the time we use is in the future, after the draft was created.
    val allDeleted =
      draftAdjudicationRepository.deleteDraftAdjudicationByCreateDateTimeBeforeAndChargeNumberIsNotNull(deleteBefore)
    assertThat(allDeleted).hasSize(1)
    val deleted = allDeleted[0]
    assertThat(entityManager.find(IncidentDetails::class.java, deleted.incidentDetails.id)).isNull()
    assertThat(entityManager.find(IncidentStatement::class.java, deleted.incidentStatement?.id)).isNull()
    deleted.offenceDetails.forEach {
      assertThat(entityManager.find(Offence::class.java, it.id)).isNull()
    }
  }

  @Test
  fun `find by associated prisoner number`() {
    val draft = draftWithAllData("1")
    entityManager.persistAndFlush(draft)

    assertThat(
      draftAdjudicationRepository.findByIncidentRoleAssociatedPrisonersNumber("B23456").first().prisonerNumber,
    ).isEqualTo(
      draft.prisonerNumber,
    )
  }

  @Test
  fun `find by victims prisoner number`() {
    val draft = draftWithAllData("1")
    entityManager.persistAndFlush(draft)

    assertThat(
      draftAdjudicationRepository.findByOffenceDetailsVictimPrisonersNumber("A1234AA").first().prisonerNumber,
    ).isEqualTo(
      draft.prisonerNumber,
    )
  }

  @Test
  fun `draft protected characteristics`() {
    val draft = draftWithAllData("1").also {
      it.offenceDetails.first().protectedCharacteristics = mutableListOf(
        DraftProtectedCharacteristics(characteristic = Characteristic.AGE),
      )
    }
    val id = draftAdjudicationRepository.save(draft).id
    assertThat(
      id?.let {
        draftAdjudicationRepository.findById(it)
          .get().offenceDetails.first().protectedCharacteristics.first().characteristic
      } == Characteristic.AGE,
    ).isTrue
  }

  private fun newDraft(): DraftAdjudication = DraftAdjudication(
    prisonerNumber = "A12345",
    gender = Gender.MALE,
    agencyId = "MDI",
    incidentDetails = IncidentDetails(
      locationId = 2,
      locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
      dateTimeOfIncident = DEFAULT_DATE_TIME,
      dateTimeOfDiscovery = DEFAULT_DATE_TIME,
      handoverDeadline = DEFAULT_DATE_TIME.plusDays(2),
    ),
    incidentRole = IncidentRole(
      roleCode = "25a",
      associatedPrisonersNumber = "B23456",
      associatedPrisonersName = "Associated Prisoner",
    ),
    isYouthOffender = true,
  )

  private fun draftWithAllData(chargeNumber: String? = null): DraftAdjudication = DraftAdjudication(
    chargeNumber = chargeNumber,
    prisonerNumber = "A12345",
    gender = Gender.MALE,
    agencyId = "MDI",
    incidentDetails = IncidentDetails(
      locationId = 2,
      locationUuid = UUID.fromString("9d306768-26a3-4bce-8b5d-3ec0f8a57b2a"),
      dateTimeOfIncident = DEFAULT_DATE_TIME,
      dateTimeOfDiscovery = DEFAULT_DATE_TIME.plusDays(1),
      handoverDeadline = DEFAULT_DATE_TIME.plusDays(2),
    ),
    incidentRole = IncidentRole(
      roleCode = "25a",
      associatedPrisonersNumber = "B23456",
      associatedPrisonersName = "Associated Prisoner",
    ),
    offenceDetails = mutableListOf(
      Offence(
        // offence with minimal data set
        offenceCode = 2,
      ),
      Offence(
        offenceCode = 3,
        victimPrisonersNumber = "A1234AA",
        victimStaffUsername = "ABC12D",
        victimOtherPersonsName = "Someones Name Here",
      ),
    ),
    incidentStatement = IncidentStatement(
      statement = "Example statement",
      completed = false,
    ),
    isYouthOffender = true,
    damages = mutableListOf(
      Damage(
        code = DamageCode.CLEANING,
        details = "details",
        reporter = "Fred",
      ),
    ),
    evidence = mutableListOf(
      Evidence(
        code = EvidenceCode.PHOTO,
        details = "details",
        reporter = "Fred",
      ),
    ),
    witnesses = mutableListOf(
      Witness(
        code = WitnessCode.OFFICER,
        firstName = "prison",
        lastName = "officer",
        reporter = "Fred",
      ),
    ),
    witnessesSaved = true,
    damagesSaved = true,
    evidenceSaved = true,
  )

  companion object {
    val DEFAULT_DATE_TIME = LocalDateTime.of(2021, 10, 3, 10, 10, 22)
  }
}
