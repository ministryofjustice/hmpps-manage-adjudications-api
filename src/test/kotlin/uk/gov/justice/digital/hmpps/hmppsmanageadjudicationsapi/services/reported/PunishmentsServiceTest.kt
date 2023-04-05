package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.reported.PunishmentRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PrivilegeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException
import javax.validation.ValidationException

class PunishmentsServiceTest : ReportedAdjudicationTestBase() {

  private val punishmentsService = PunishmentsService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)))
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreatePunishments {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun `init`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          it.outcomes.add(Outcome(code = OutcomeCode.CHARGE_PROVED))
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @CsvSource(
      "ADJOURNED", "REFER_POLICE", "REFER_INAD", "SCHEDULED", "UNSCHEDULED", "AWAITING_REVIEW", "PROSECUTION",
      "NOT_PROCEED", "DISMISSED", "REJECTED", "RETURNED",
    )
    @ParameterizedTest
    fun `validation error - wrong status code - must be CHARGE_PROVED `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also { it.status = status },
      )
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("status is not CHARGE_PROVED")
    }

    @Test
    fun `validation error - privilege missing sub type `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, days = 1)))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("subtype missing for type PRIVILEGE")
    }

    @Test
    fun `validation error - other privilege missing description `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.PRIVILEGE, privilegeType = PrivilegeType.OTHER, days = 1)))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("description missing for type PRIVILEGE - sub type OTHER")
    }

    @Test
    fun `validation error - earnings missing stoppage percentage `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.EARNINGS, days = 1)))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("stoppage percentage missing for type EARNINGS")
    }

    @CsvSource(" PRIVILEGE", "EARNINGS", "CONFINEMENT", "REMOVAL_ACTIVITY", "EXCLUSION_WORK", "EXTRA_WORK", "REMOVAL_WING")
    @ParameterizedTest
    fun `validation error - not suspended missing start date `(type: PunishmentType) {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1, endDate = LocalDate.now())))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing start date for schedule")
    }

    @CsvSource(" PRIVILEGE", "EARNINGS", "CONFINEMENT", "REMOVAL_ACTIVITY", "EXCLUSION_WORK", "EXTRA_WORK", "REMOVAL_WING")
    @ParameterizedTest
    fun `validation error - not suspended missing end date `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1, startDate = LocalDate.now())))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing end date for schedule")
    }

    @CsvSource(" PRIVILEGE", "EARNINGS", "CONFINEMENT", "REMOVAL_ACTIVITY", "EXCLUSION_WORK", "EXTRA_WORK", "REMOVAL_WING")
    @ParameterizedTest
    fun `validation error - suspended missing all schedule dates `() {
      Assertions.assertThatThrownBy {
        punishmentsService.create(adjudicationNumber = 1, listOf(PunishmentRequest(type = PunishmentType.REMOVAL_ACTIVITY, days = 1)))
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("missing all schedule data")
    }

    @Test
    fun `creates a set of punishments `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = punishmentsService.create(
        adjudicationNumber = 1,
        listOf(
          PunishmentRequest(
            type = PunishmentType.PRIVILEGE,
            privilegeType = PrivilegeType.OTHER,
            otherPrivilege = "other",
            days = 1,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(1),
          ),
          PunishmentRequest(
            type = PunishmentType.PROSPECTIVE_DAYS,
            days = 1,
          ),
          PunishmentRequest(
            type = PunishmentType.ADDITIONAL_DAYS,
            days = 1,
          ),
        ),
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      Assertions.assertThat(argumentCaptor.value.punishments.size).isEqualTo(3)
      Assertions.assertThat(argumentCaptor.value.punishments.first()).isNotNull
      Assertions.assertThat(argumentCaptor.value.punishments.first().type).isEqualTo(PunishmentType.PRIVILEGE)
      Assertions.assertThat(argumentCaptor.value.punishments.first().privilegeType).isEqualTo(PrivilegeType.OTHER)
      Assertions.assertThat(argumentCaptor.value.punishments.first().otherPrivilege).isEqualTo("other")
      Assertions.assertThat(argumentCaptor.value.punishments.first().schedule.first()).isNotNull
      Assertions.assertThat(argumentCaptor.value.punishments.first().schedule.first().startDate).isEqualTo(LocalDate.now())
      Assertions.assertThat(argumentCaptor.value.punishments.first().schedule.first().endDate).isEqualTo(LocalDate.now().plusDays(1))
      Assertions.assertThat(argumentCaptor.value.punishments.first().schedule.first().days).isEqualTo(1)

      Assertions.assertThat(response).isNotNull
    }
  }
}
