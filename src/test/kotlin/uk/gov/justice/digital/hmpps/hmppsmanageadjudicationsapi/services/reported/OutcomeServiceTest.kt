package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.ReportedAdjudicationDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OicHearingType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReferGovReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.PunishmentsService.Companion.latestSchedule
import java.time.LocalDate
import java.time.LocalDateTime

class OutcomeServiceTest : ReportedAdjudicationTestBase() {
  private val outcomeService = OutcomeService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      outcomeService.createReferral(
        chargeNumber = "1",
        code = OutcomeCode.REFER_POLICE,
        details = "",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createProsecution("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createNotProceed("1", NotProceedReason.NOT_FAIR, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.getOutcomes("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.deleteOutcome("1", 1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createChargeProved("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createChargeProved("1", false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.getLatestOutcome("1")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createQuashed("1", QuashedReason.APPEAL_UPHELD, "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.amendOutcomeViaApi("1", "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.amendOutcomeViaService("1", OutcomeCode.CHARGE_PROVED)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreateOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.add(entityBuilder.createHearing())
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @ParameterizedTest
    @CsvSource("REJECTED", "AWAITING_REVIEW", "NOT_PROCEED", "REFER_INAD", "REFER_POLICE", "UNSCHEDULED", "CHARGE_PROVED", "QUASHED", "REFER_GOV")
    fun `create outcome throws exception if invalid state `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = status
        },
      )

      val code = if (listOf(ReportedAdjudicationStatus.UNSCHEDULED, ReportedAdjudicationStatus.REFER_POLICE).contains(status)) {
        OutcomeCode.REFER_INAD
      } else {
        OutcomeCode.REFER_POLICE
      }

      Assertions.assertThatThrownBy {
        outcomeService.createReferral(chargeNumber = "1", code = code, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION", "SCHEDULE_HEARING", "QUASHED")
    @ParameterizedTest
    fun `throws exception if referral validation fails`(code: OutcomeCode) {
      Assertions.assertThatThrownBy {
        outcomeService.createReferral(
          chargeNumber = "1",
          code = code,
          details = "details",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("invalid referral type")
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE", "REFER_GOV")
    fun `create outcome throws exception if police referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_POLICE)
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_GOV")
    fun `create outcome throws exception if gov referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_GOV)
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if inad referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_INAD)
    }

    private fun assertTransition(codeFrom: OutcomeCode, codeTo: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(
            Outcome(code = codeFrom).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) },
          )
          it.status = ReportedAdjudicationStatus.SCHEDULED
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.createReferral(chargeNumber = "1", code = codeTo, details = "details", referGovReason = ReferGovReason.GOV_INQUIRY)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid referral transition")
    }

    @Test
    fun `create not proceed `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.UNSCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createNotProceed(
        "1235",
        NotProceedReason.NOT_FAIR,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(OutcomeCode.NOT_PROCEED.name))
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isEqualTo(NotProceedReason.NOT_FAIR)
      assertThat(response).isNotNull
    }

    @Test
    fun `create dismissed `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createDismissed(
        "1235",
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(response).isNotNull
    }

    @Test
    fun `create prosecution `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.REFER_POLICE

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createProsecution("1235")

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.PROSECUTION)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().oicHearingId).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `create refer gov `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.REFER_INAD

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createReferGov(
        chargeNumber = "1235",
        details = "details",
        referGovReason = ReferGovReason.OTHER,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_GOV)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.getOutcomes().first().referGovReason).isEqualTo(ReferGovReason.OTHER)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(OutcomeCode.REFER_GOV.name))
      assertThat(response).isNotNull
    }

    @Test
    fun `create quashed`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.CHARGE_PROVED
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
          it.hearings.add(entityBuilder.createHearing())
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "test")
          it.addOutcome(Outcome(code = OutcomeCode.CHARGE_PROVED))
        },
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createQuashed(
        "1235",
        QuashedReason.APPEAL_UPHELD,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().last()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.QUASHED)
      assertThat(argumentCaptor.value.getOutcomes().last().details).isEqualTo("details")
      assertThat(response).isNotNull
    }

    @CsvSource("REFER_POLICE", "REFER_INAD", "DISMISSED", "SCHEDULE_HEARING", "PROSECUTION", "NOT_PROCEED", "QUASHED", "REFER_GOV")
    @ParameterizedTest
    fun `create quashed throws exception if previous outcome is not a charge proved `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(Outcome(code = code))
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.createQuashed("1", QuashedReason.APPEAL_UPHELD, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to quash this outcome")
    }

    @Test
    fun `create quashed throws exception if no previous hearing outcome `() {
      Assertions.assertThatThrownBy {
        outcomeService.createQuashed("1", QuashedReason.APPEAL_UPHELD, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to quash this outcome")
    }

    @ParameterizedTest
    @CsvSource("UNSCHEDULED, REFER_POLICE", "SCHEDULED, REFER_INAD", "SCHEDULED, REFER_GOV")
    fun `create referral`(codeFrom: ReportedAdjudicationStatus, code: OutcomeCode) {
      reportedAdjudication.status = codeFrom

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createReferral(
        chargeNumber = "1235",
        code = code,
        details = "details",
        referGovReason = if (code == OutcomeCode.REFER_GOV) ReferGovReason.OTHER else null,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(code)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      if (code == OutcomeCode.REFER_GOV) assertThat(argumentCaptor.value.getOutcomes().first().referGovReason).isEqualTo(ReferGovReason.OTHER)

      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(code.name))
      assertThat(response).isNotNull
    }

    @Test
    fun `create charge proved v2 `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createChargeProved(
        "1235",
        true,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isNull()
      assertThat(response).isNotNull
    }

    @Test
    fun `throws validation exception if refer gov is missing reason when creating refer gov referral`() {
      Assertions.assertThatThrownBy {
        outcomeService.createReferral(
          chargeNumber = "1",
          code = OutcomeCode.REFER_GOV,
          details = "",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("referGovReason is mandatory for code REFER_GOV")
    }
  }

  @Nested
  inner class DeleteOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    private val reportedAdjudicationWithOutcomeAndNoHearings = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.clear()
      it.addOutcome(
        Outcome(id = 1, code = OutcomeCode.REFER_POLICE),
      )
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByChargeNumber("3")).thenReturn(reportedAdjudicationWithOutcomeAndNoHearings)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @CsvSource("REFER_POLICE", "PROSECUTION", "CHARGE_PROVED", "NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    fun `delete outcome when we have hearings `(code: OutcomeCode) {
      val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
        it.addOutcome(
          Outcome(id = 1, code = code),
        )
        it.addPunishment(
          Punishment(
            type = PunishmentType.CONFINEMENT,
            suspendedUntil = LocalDate.now(),
            schedule = mutableListOf(
              PunishmentSchedule(days = 10),
            ),
          ),
        )
      }

      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        "2",
        1,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()

      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)

      if (code == OutcomeCode.CHARGE_PROVED) {
        assertThat(argumentCaptor.value.getPunishments()).isEmpty()
      }

      assertThat(response).isNotNull
    }

    @Test
    fun `delete outcome when we have no hearings `() {
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        "3",
        1,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)

      assertThat(response).isNotNull
    }

    @Test
    fun `delete outcome throws no outcome found for adjudication `() {
      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome("1", 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @Test
    fun `delete latest outcome throws not found if no outcomes present `() {
      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome("1")
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "SCHEDULE_HEARING", "PROSECUTION", "NOT_PROCEED", "CHARGE_PROVED", "REFER_GOV")
    fun `throws invalid state if delete latest outcome is invalid type `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication
          .also {
            it.addOutcome(Outcome(code = code).also { o -> o.createDateTime = LocalDateTime.now() })
          },
      )

      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome("1")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to delete via api - DEL/outcome")
    }

    @Test
    fun `doesnt throws invalid state if delete latest outcome is corrupted status`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication
          .also {
            it.status = ReportedAdjudicationStatus.INVALID_OUTCOME
            it.addOutcome(Outcome(code = OutcomeCode.QUASHED).also { o -> o.createDateTime = LocalDateTime.now() })
          },
      )

      Assertions.assertThatNoException().isThrownBy {
        outcomeService.deleteOutcome("1")
      }
    }

    @CsvSource("QUASHED", "NOT_PROCEED")
    @ParameterizedTest
    fun `delete latest outcome succeeds without provided id `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
          if (code == OutcomeCode.NOT_PROCEED) it.hearings.clear()
          it.addOutcome(
            Outcome(id = 1, code = code).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) },
          )
          if (code == OutcomeCode.QUASHED) {
            it.addOutcome(
              Outcome(id = 1, code = OutcomeCode.CHARGE_PROVED).also { o -> o.createDateTime = LocalDateTime.now() },
            )
          }
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        "4",
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      if (code == OutcomeCode.NOT_PROCEED) assertThat(argumentCaptor.value.getOutcomes()).isEmpty() else assertThat(argumentCaptor.value.getOutcomes()).isNotEmpty
      if (code == OutcomeCode.NOT_PROCEED) {
        assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.UNSCHEDULED)
      } else {
        assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.CHARGE_PROVED)
      }

      assertThat(response).isNotNull
    }

    @CsvSource("CHARGE_PROVED", "NOT_PROCEED", "DISMISSED")
    @ParameterizedTest
    fun `delete a completed hearing outcome `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication
          .also {
            it.addOutcome(Outcome(id = 1, code = code).also { o -> o.createDateTime = LocalDateTime.now() })
            if (code == OutcomeCode.CHARGE_PROVED) {
              it.addPunishment(
                Punishment(
                  type = PunishmentType.DAMAGES_OWED,
                  schedule = mutableListOf(
                    PunishmentSchedule(days = 10),
                  ),
                ),
              )
              it.punishmentComments.add(PunishmentComment(comment = ""))
            }
          },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.deleteOutcome(
        "1",
        1,
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      if (code == OutcomeCode.CHARGE_PROVED) {
        assertThat(argumentCaptor.value.getPunishments()).isEmpty()
        assertThat(argumentCaptor.value.punishmentComments).isEmpty()
      }

      assertThat(argumentCaptor.value.getOutcomes()).isEmpty()
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.SCHEDULED)

      assertThat(response).isNotNull
    }

    @Test
    fun `deletes NOT_PROCEED from REFER_GOV`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication
          .also {
            it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
            it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD).also { o -> o.createDateTime = LocalDateTime.now() })
            it.addOutcome(Outcome(code = OutcomeCode.REFER_GOV).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) })
            it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED).also { o -> o.createDateTime = LocalDateTime.now().plusDays(2) })
          },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      outcomeService.deleteOutcome(chargeNumber = "1")
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.REFER_GOV)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.REFER_GOV)
    }

    @Test
    fun `delete outcome throws exception if ADA linked to another report`() {
      whenever(reportedAdjudicationRepository.findByPunishmentsConsecutiveToChargeNumberAndPunishmentsTypeIn(any(), any())).thenReturn(listOf(reportedAdjudication))

      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(
        reportedAdjudication
          .also {
            it.addOutcome(Outcome(id = 1, code = OutcomeCode.CHARGE_PROVED).also { o -> o.createDateTime = LocalDateTime.now() })
            it.addPunishment(
              Punishment(
                type = PunishmentType.DAMAGES_OWED,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10),
                ),
              ),
            )
            it.punishmentComments.add(PunishmentComment(comment = ""))
          },
      )

      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(
          "1",
          1,
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("is linked to another report")
    }
  }

  @Nested
  inner class GetCombinedOutcomes {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.addOutcome(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now() },
      )
    }
    private val reportedAdjudication2 = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.addOutcome(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now() },
      )
      it.addOutcome(
        Outcome(
          code = OutcomeCode.NOT_PROCEED,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) },
      )
    }
    private val reportedAdjudication3 = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.addOutcome(
        Outcome(
          code = OutcomeCode.REFER_INAD,
        ).also { o -> o.createDateTime = LocalDateTime.now() },
      )
      it.addOutcome(
        Outcome(
          code = OutcomeCode.SCHEDULE_HEARING,
        ).also { o -> o.createDateTime = LocalDateTime.now() },
      )
      it.addOutcome(
        Outcome(
          code = OutcomeCode.REFER_POLICE,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(2) },
      )
      it.addOutcome(
        Outcome(
          code = OutcomeCode.NOT_PROCEED,
        ).also { o -> o.createDateTime = LocalDateTime.now().plusDays(3) },
      )
    }
    private val reportedAdjudicationNoOutcomes = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.clearOutcomes()
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByChargeNumber("1")).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByChargeNumber("2")).thenReturn(reportedAdjudication2)
      whenever(reportedAdjudicationRepository.findByChargeNumber("3")).thenReturn(reportedAdjudication3)
      whenever(reportedAdjudicationRepository.findByChargeNumber("4")).thenReturn(reportedAdjudicationNoOutcomes)
    }

    @Test
    fun `no outcomes`() {
      val result = outcomeService.getOutcomes("4")

      assertThat(result.isEmpty()).isEqualTo(true)
    }

    @Test
    fun `get outcomes without any referral outcomes`() {
      val result = outcomeService.getOutcomes("1")

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNull()
    }

    @Test
    fun `get outcomes with referral outcomes`() {
      val result = outcomeService.getOutcomes("2")

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNotNull
      assertThat(result.first().referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `get outcomes with multiple referral outcomes`() {
      val result = outcomeService.getOutcomes("3")

      assertThat(result.size).isEqualTo(2)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(result.first().referralOutcome).isNotNull
      assertThat(result.first().referralOutcome!!.code).isEqualTo(OutcomeCode.SCHEDULE_HEARING)
      assertThat(result.last().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.last().referralOutcome).isNotNull
      assertThat(result.last().referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }
  }

  @Nested
  inner class AmendOutcomeViaApi {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = "test"
        },
      )
    }

    @Test
    fun `amend refer police succeeds `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.clear()
          it.status = OutcomeCode.REFER_POLICE.status
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, details = "previous"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi("1235", "updated")

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amend quashed succeeds `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = OutcomeCode.QUASHED.status
          it.addOutcome(Outcome(code = OutcomeCode.QUASHED, details = "previous", quashedReason = QuashedReason.APPEAL_UPHELD))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi(
        chargeNumber = "1235",
        details = "updated",
        quashedReason = QuashedReason.FLAWED_CASE,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.QUASHED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isEqualTo(QuashedReason.FLAWED_CASE)

      assertThat(response).isNotNull
    }

    @Test
    fun `amend not proceed succeeds `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.clear()
          it.status = OutcomeCode.NOT_PROCEED.status
          it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, details = "previous", notProceedReason = NotProceedReason.NOT_FAIR))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi("1235", "updated", NotProceedReason.WITNESS_NOT_ATTEND)

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND)
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amend not proceed from referral outcome refer gov succeeds `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
          it.status = OutcomeCode.NOT_PROCEED.status
          it.addOutcome(Outcome(code = OutcomeCode.REFER_INAD).also { o -> o.createDateTime = LocalDateTime.now() })
          it.addOutcome(Outcome(code = OutcomeCode.REFER_GOV).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) })
          it.addOutcome(
            Outcome(code = OutcomeCode.NOT_PROCEED, details = "previous", notProceedReason = NotProceedReason.NOT_FAIR).also {
                o ->
              o.createDateTime = LocalDateTime.now().plusDays(2)
            },
          )
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi(
        chargeNumber = "1235",
        details = "updated",
        reason = NotProceedReason.WITNESS_NOT_ATTEND,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().last()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().last().details).isEqualTo("updated")

      assertThat(response).isNotNull
    }

    @Test
    fun `amend refer gov succeeds`() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.REFER_INAD, adjudicator = "")
          it.status = OutcomeCode.REFER_GOV.status
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_INAD, details = "previous").also {
                o ->
              o.createDateTime = LocalDateTime.now()
            },
          )
          it.addOutcome(
            Outcome(code = OutcomeCode.REFER_GOV, details = "previous").also {
                o ->
              o.createDateTime = LocalDateTime.now().plusDays(1)
            },
          )
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      outcomeService.amendOutcomeViaApi(
        chargeNumber = "1235",
        details = "updated",
        referGovReason = ReferGovReason.OTHER,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_INAD)
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.REFER_GOV)
      assertThat(argumentCaptor.value.getOutcomes().last().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().last().referGovReason).isEqualTo(ReferGovReason.OTHER)
    }

    @CsvSource("REFER_INAD", "SCHEDULE_HEARING", "PROSECUTION", "CHARGE_PROVED", "DISMISSED")
    @ParameterizedTest
    fun `throws validation exception if invalid outcome type for amend `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = code.status
          it.addOutcome(Outcome(code = code, details = "previous", notProceedReason = NotProceedReason.NOT_FAIR))
        },
      )
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaApi("1", "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend this outcome")
    }

    @Test
    fun `throws validation exception if no latest outcome to amend `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaApi("1", "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend this outcome")
    }
  }

  @Nested
  inner class AmendOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.clear()
      it.hearings.add(Hearing(dateTimeOfHearing = LocalDateTime.now(), oicHearingId = 1L, chargeNumber = "1", agencyId = "", oicHearingType = OicHearingType.GOV, locationId = 1L))
      it.createdByUserId = ""
      it.createDateTime = LocalDateTime.now()
    }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = "test"
        },
      )
    }

    @Test
    fun `throws entity not found exception if no latest outcome `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService("1", OutcomeCode.CHARGE_PROVED)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("no latest outcome to amend")
    }

    @CsvSource("REFER_POLICE", "REFER_INAD", "CHARGE_PROVED", "DISMISSED", "NOT_PROCEED", "REFER_GOV")
    @ParameterizedTest
    fun `throws validation exception if the latest outcome is not of the correct type `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(
            Outcome(code = OutcomeCode.entries.first { oc -> oc != code }),
          )
        },
      )
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService("1", code)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("latest outcome is not of same type")
    }

    @CsvSource("QUASHED", "SCHEDULE_HEARING", "REFER_POLICE", "NOT_PROCEED, CHARGE_PROVED")
    @ParameterizedTest
    fun `throws validation exception if outcome code not supported by this function `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(Outcome(code = code))
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          if (listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED).contains(code)) it.hearings.clear()
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService("1", OutcomeCode.CHARGE_PROVED)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend via this function")
    }

    @CsvSource("REFER_INAD", "REFER_POLICE", "REFER_GOV")
    @ParameterizedTest
    fun `amends referral successfully `(code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = code, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = code.outcomeCode!!, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        chargeNumber = "1",
        outcomeCodeToAmend = code.outcomeCode!!,
        details = "updated",
        referGovReason = if (code == HearingOutcomeCode.REFER_GOV) ReferGovReason.OTHER else null,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(code.outcomeCode!!)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      if (code == HearingOutcomeCode.REFER_GOV) assertThat(argumentCaptor.value.getOutcomes().first().referGovReason).isEqualTo(ReferGovReason.OTHER)
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amends dismissed successfully `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = OutcomeCode.DISMISSED, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        chargeNumber = "1",
        outcomeCodeToAmend = OutcomeCode.DISMISSED,
        details = "updated",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amends not proceed successfully `() {
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, details = "details", notProceedReason = NotProceedReason.NOT_FAIR))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        chargeNumber = "1",
        outcomeCodeToAmend = OutcomeCode.NOT_PROCEED,
        details = "updated",
        notProceedReason = NotProceedReason.WITNESS_NOT_ATTEND,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().notProceedReason).isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND)
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class Deactivations {

    private fun currentChargeForActivation() = entityBuilder.reportedAdjudication(chargeNumber = "12345").also {
      it.status = ReportedAdjudicationStatus.CHARGE_PROVED
      it.addOutcome(
        Outcome(id = 1, code = OutcomeCode.CHARGE_PROVED, actualCreatedDate = LocalDateTime.now()),
      )
    }

    private fun reportToActivateFrom(chargeNumber: String) = entityBuilder.reportedAdjudication(chargeNumber = "activated").also {
      it.clearPunishments()
      it.addPunishment(
        Punishment(
          id = 1,
          type = PunishmentType.ADDITIONAL_DAYS,
          suspendedUntil = null,
          activatedByChargeNumber = chargeNumber,
          schedule =
          mutableListOf(
            PunishmentSchedule(id = 1, days = 10, suspendedUntil = LocalDate.now())
              .also { s -> s.createDateTime = LocalDateTime.now() },
            PunishmentSchedule(id = 2, days = 10, startDate = LocalDate.now(), endDate = LocalDate.now())
              .also { s -> s.createDateTime = LocalDateTime.now().plusDays(1) },
          ),
        ),
      )
    }

    private fun assertDeactivation(response: ReportedAdjudicationDto, report: ReportedAdjudication) {
      val ada = report.getPunishments().first { it.id == 1L }
      assertThat(ada.suspendedUntil).isEqualTo(LocalDate.now())
      assertThat(ada.activatedByChargeNumber).isNull()
      assertThat(ada.schedule.latestSchedule().startDate).isNull()
      assertThat(ada.schedule.latestSchedule().endDate).isNull()
      assertThat(ada.schedule.latestSchedule().suspendedUntil).isEqualTo(LocalDate.now())

      assertThat(response.suspendedPunishmentEvents!!.size).isEqualTo(1)
      assertThat(response.suspendedPunishmentEvents!!.first()).isEqualTo(
        SuspendedPunishmentEvent(chargeNumber = report.chargeNumber, agencyId = report.originatingAgencyId, status = report.status),
      )
    }

    @Test
    fun `quash outcome deactivates activated suspended from punishments`() {
      val outcomeServiceV2 = OutcomeService(
        reportedAdjudicationRepository,
        offenceCodeLookupService,
        authenticationFacade,
      )

      val currentCharge = currentChargeForActivation()
      val reportToTest = reportToActivateFrom(currentCharge.chargeNumber)
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(currentCharge)
      whenever(reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber("12345")).thenReturn(
        listOf(reportToTest),
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(currentCharge)

      assertDeactivation(outcomeServiceV2.createQuashed(chargeNumber = currentCharge.chargeNumber, reason = QuashedReason.OTHER, details = ""), reportToTest)
    }

    @Test
    fun `delete outcome deactivates activated suspended from punishments`() {
      val outcomeServiceV2 = OutcomeService(
        reportedAdjudicationRepository,
        offenceCodeLookupService,
        authenticationFacade,
      )

      val currentCharge = currentChargeForActivation()
      val reportToTest = reportToActivateFrom(currentCharge.chargeNumber)
      whenever(reportedAdjudicationRepository.findByChargeNumber("12345")).thenReturn(currentCharge)
      whenever(reportedAdjudicationRepository.findByPunishmentsActivatedByChargeNumber("12345")).thenReturn(
        listOf(reportToTest),
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(currentCharge)
      assertDeactivation(outcomeServiceV2.deleteOutcome(chargeNumber = currentCharge.chargeNumber, id = 1), reportToTest)
    }
  }
}
