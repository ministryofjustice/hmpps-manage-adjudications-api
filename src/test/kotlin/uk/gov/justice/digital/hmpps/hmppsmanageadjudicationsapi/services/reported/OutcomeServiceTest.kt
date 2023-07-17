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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Hearing
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.HearingOutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.NotProceedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Outcome
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.OutcomeCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentComment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.QuashedReason
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingType
import java.time.LocalDate
import java.time.LocalDateTime

class OutcomeServiceTest : ReportedAdjudicationTestBase() {
  private val nomisOutcomeService: NomisOutcomeService = mock()
  private val punishmentsService: PunishmentsService = mock()
  private val outcomeService = OutcomeService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
    nomisOutcomeService,
    punishmentsService,
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      outcomeService.createReferral(1, OutcomeCode.REFER_POLICE, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createProsecution(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createNotProceed(1, NotProceedReason.NOT_FAIR, "")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.getOutcomes(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.deleteOutcome(1, 1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createChargeProved(1, 0.0, false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createChargeProvedV2(1, false)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.getLatestOutcome(1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.createQuashed(1, QuashedReason.APPEAL_UPHELD, "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.amendOutcomeViaApi(1, "details")
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      outcomeService.amendOutcomeViaService(1, OutcomeCode.CHARGE_PROVED)
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
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = ReportedAdjudicationStatus.UNSCHEDULED
          it.createdByUserId = "test"
          it.createDateTime = LocalDateTime.now()
        },
      )
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @ParameterizedTest
    @CsvSource("REJECTED", "AWAITING_REVIEW", "NOT_PROCEED", "REFER_INAD", "REFER_POLICE", "UNSCHEDULED", "CHARGE_PROVED", "QUASHED")
    fun `create outcome throws exception if invalid state `(status: ReportedAdjudicationStatus) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
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
        outcomeService.createReferral(adjudicationNumber = 1, code = code, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid status transition")
    }

    @CsvSource("NOT_PROCEED", "PROSECUTION", "SCHEDULE_HEARING", "QUASHED")
    @ParameterizedTest
    fun `throws exception if referral validation fails`(code: OutcomeCode) {
      Assertions.assertThatThrownBy {
        outcomeService.createReferral(
          adjudicationNumber = 1L,
          code = code,
          details = "details",
        )
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("invalid referral type")
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if police referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_POLICE)
    }

    @ParameterizedTest
    @CsvSource("REFER_INAD", "REFER_POLICE")
    fun `create outcome throws exception if inad referral outcome invalid state `(code: OutcomeCode) {
      assertTransition(code, OutcomeCode.REFER_INAD)
    }

    private fun assertTransition(codeFrom: OutcomeCode, codeTo: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(
            Outcome(code = codeFrom).also { o -> o.createDateTime = LocalDateTime.now().plusDays(1) },
          )
          it.status = ReportedAdjudicationStatus.SCHEDULED
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.createReferral(adjudicationNumber = 1, code = codeTo, details = "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Invalid referral transition")
    }

    @Test
    fun `create not proceed `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.UNSCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createNotProceed(
        1235L,
        NotProceedReason.NOT_FAIR,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), anyOrNull(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(OutcomeCode.NOT_PROCEED.name))
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isEqualTo(NotProceedReason.NOT_FAIR)
      assertThat(response).isNotNull
    }

    @Test
    fun `create dismissed `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createDismissed(
        1235L,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(response).isNotNull
    }

    @Test
    fun `create prosecution `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.REFER_POLICE

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      whenever(nomisOutcomeService.createHearingResultIfApplicable(any(), anyOrNull(), any())).thenReturn(1L)

      val response = outcomeService.createProsecution(
        1235L,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.PROSECUTION)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().oicHearingId).isEqualTo(1)

      assertThat(response).isNotNull
    }

    @Test
    fun `create quashed`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
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
        1235L,
        QuashedReason.APPEAL_UPHELD,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().size).isEqualTo(2)
      assertThat(argumentCaptor.value.getOutcomes().last()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().last().code).isEqualTo(OutcomeCode.QUASHED)
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.QUASHED)
      assertThat(argumentCaptor.value.getOutcomes().last().details).isEqualTo("details")
      assertThat(response).isNotNull
    }

    @CsvSource("REFER_POLICE", "REFER_INAD", "DISMISSED", "SCHEDULE_HEARING", "PROSECUTION", "NOT_PROCEED", "QUASHED")
    @ParameterizedTest
    fun `create quashed throws exception if previous outcome is not a charge proved `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(Outcome(code = code))
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.createQuashed(1, QuashedReason.APPEAL_UPHELD, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to quash this outcome")
    }

    @Test
    fun `create quashed throws exception if no previous hearing outcome `() {
      Assertions.assertThatThrownBy {
        outcomeService.createQuashed(1, QuashedReason.APPEAL_UPHELD, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to quash this outcome")
    }

    @ParameterizedTest
    @CsvSource("UNSCHEDULED, REFER_POLICE", "SCHEDULED, REFER_INAD")
    fun `create referral`(codeFrom: ReportedAdjudicationStatus, code: OutcomeCode) {
      reportedAdjudication.status = codeFrom

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createReferral(
        1235L,
        code,
        "details",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), anyOrNull(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(code)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("details")
      assertThat(argumentCaptor.value.status).isEqualTo(ReportedAdjudicationStatus.valueOf(code.name))
      assertThat(response).isNotNull
    }

    @Deprecated("to remove on completion of NN-5319")
    @Test
    fun `create charge proved `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createChargeProved(
        1235L,
        100.0,
        true,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), any(), any())
      verify(punishmentsService, atLeastOnce()).createPunishmentsFromChargeProvedIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isNull()
      assertThat(response).isNotNull
    }

    @Test
    fun `create charge proved v2 `() {
      reportedAdjudication.status = ReportedAdjudicationStatus.SCHEDULED

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.createChargeProvedV2(
        1235L,
        true,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).createHearingResultIfApplicable(any(), any(), any())
      verify(punishmentsService, never()).createPunishmentsFromChargeProvedIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.CHARGE_PROVED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isNull()
      assertThat(response).isNotNull
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
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudicationWithOutcomeAndNoHearings)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(
        reportedAdjudication.also {
          it.createDateTime = LocalDateTime.now()
          it.createdByUserId = "test"
        },
      )
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

      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.deleteOutcome(
        2,
        1,
      )

      verify(nomisOutcomeService, atLeastOnce()).deleteHearingResultIfApplicable(any(), any(), any())
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
        3,
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
        outcomeService.deleteOutcome(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @Test
    fun `delete latest outcome throws not found if no outcomes present `() {
      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Outcome not found for 1")
    }

    @ParameterizedTest
    @CsvSource("REFER_POLICE", "REFER_INAD", "SCHEDULE_HEARING", "PROSECUTION", "NOT_PROCEED", "CHARGE_PROVED")
    fun `throws invalid state if delete latest outcome is invalid type `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
        reportedAdjudication
          .also {
            it.addOutcome(Outcome(code = code).also { o -> o.createDateTime = LocalDateTime.now() })
          },
      )

      Assertions.assertThatThrownBy {
        outcomeService.deleteOutcome(1)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("Unable to delete via api - DEL/outcome")
    }

    @CsvSource("QUASHED", "NOT_PROCEED")
    @ParameterizedTest
    fun `delete latest outcome succeeds without provided id `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
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
        4,
      )
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      if (code == OutcomeCode.QUASHED) {
        verify(punishmentsService, atLeastOnce()).removeQuashedFinding(any())
      }

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
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(
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
        1,
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
      whenever(reportedAdjudicationRepository.findByReportNumber(1)).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.findByReportNumber(2)).thenReturn(reportedAdjudication2)
      whenever(reportedAdjudicationRepository.findByReportNumber(3)).thenReturn(reportedAdjudication3)
      whenever(reportedAdjudicationRepository.findByReportNumber(4)).thenReturn(reportedAdjudicationNoOutcomes)
    }

    @Test
    fun `no outcomes`() {
      val result = outcomeService.getOutcomes(4)

      assertThat(result.isEmpty()).isEqualTo(true)
    }

    @Test
    fun `get outcomes without any referral outcomes`() {
      val result = outcomeService.getOutcomes(1)

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNull()
    }

    @Test
    fun `get outcomes with referral outcomes`() {
      val result = outcomeService.getOutcomes(2)

      assertThat(result.size).isEqualTo(1)
      assertThat(result.first().outcome.code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(result.first().referralOutcome).isNotNull
      assertThat(result.first().referralOutcome!!.code).isEqualTo(OutcomeCode.NOT_PROCEED)
    }

    @Test
    fun `get outcomes with multiple referral outcomes`() {
      val result = outcomeService.getOutcomes(3)

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
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.clear()
          it.status = OutcomeCode.REFER_POLICE.status
          it.addOutcome(Outcome(code = OutcomeCode.REFER_POLICE, details = "previous"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi(1235L, "updated")

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.REFER_POLICE)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amend quashed succeeds `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = OutcomeCode.QUASHED.status
          it.addOutcome(Outcome(code = OutcomeCode.QUASHED, details = "previous", quashedReason = QuashedReason.APPEAL_UPHELD))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi(
        adjudicationNumber = 1235L,
        details = "updated",
        quashedReason = QuashedReason.FLAWED_CASE,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.QUASHED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isEqualTo(QuashedReason.FLAWED_CASE)

      assertThat(response).isNotNull
    }

    @Test
    fun `amend not proceed succeeds `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.clear()
          it.status = OutcomeCode.NOT_PROCEED.status
          it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, details = "previous", reason = NotProceedReason.NOT_FAIR))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)

      val response = outcomeService.amendOutcomeViaApi(1235L, "updated", NotProceedReason.WITNESS_NOT_ATTEND)

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND)
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @CsvSource("REFER_INAD", "SCHEDULE_HEARING", "PROSECUTION", "CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    fun `throws validation exception if invalid outcome type for amend `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.status = code.status
          it.addOutcome(Outcome(code = code, details = "previous", reason = NotProceedReason.NOT_FAIR))
        },
      )
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaApi(1, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend this outcome")
    }

    @Test
    fun `throws validation exception if no latest outcome to amend `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)

      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaApi(1, "details")
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend this outcome")
    }
  }

  @Nested
  inner class AmendOutcome {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT).also {
      it.hearings.clear()
      it.hearings.add(Hearing(dateTimeOfHearing = LocalDateTime.now(), oicHearingId = 1L, reportNumber = 1L, agencyId = "", oicHearingType = OicHearingType.GOV, locationId = 1L))
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
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService(1, OutcomeCode.CHARGE_PROVED)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("no latest outcome to amend")
    }

    @CsvSource("REFER_POLICE", "REFER_INAD", "CHARGE_PROVED", "DISMISSED", "NOT_PROCEED")
    @ParameterizedTest
    fun `throws validation exception if the latest outcome is not of the correct type `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(
            Outcome(code = OutcomeCode.values().first { oc -> oc != code }),
          )
        },
      )
      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService(1, code)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("latest outcome is not of same type")
    }

    @CsvSource("QUASHED", "SCHEDULE_HEARING", "REFER_POLICE", "NOT_PROCEED, CHARGE_PROVED")
    @ParameterizedTest
    fun `throws validation exception if outcome code not supported by this function `(code: OutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.addOutcome(Outcome(code = code))
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "")
          if (listOf(OutcomeCode.REFER_POLICE, OutcomeCode.NOT_PROCEED).contains(code)) it.hearings.clear()
        },
      )

      Assertions.assertThatThrownBy {
        outcomeService.amendOutcomeViaService(1, OutcomeCode.CHARGE_PROVED)
      }.isInstanceOf(ValidationException::class.java)
        .hasMessageContaining("unable to amend via this function")
    }

    @CsvSource("REFER_INAD", "REFER_POLICE")
    @ParameterizedTest
    fun `amends referral successfully `(code: HearingOutcomeCode) {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = code, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = code.outcomeCode!!, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        adjudicationNumber = 1L,
        outcomeCodeToAmend = code.outcomeCode!!,
        details = "updated",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).amendHearingResultIfApplicable(any(), anyOrNull(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(code.outcomeCode!!)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amends dismissed successfully `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = OutcomeCode.DISMISSED, details = "details"))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        adjudicationNumber = 1L,
        outcomeCodeToAmend = OutcomeCode.DISMISSED,
        details = "updated",
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).amendHearingResultIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.DISMISSED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isNull()
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }

    @Test
    fun `amends not proceed successfully `() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication.also {
          it.hearings.first().hearingOutcome = HearingOutcome(code = HearingOutcomeCode.COMPLETE, adjudicator = "adjudicator")
          it.addOutcome(Outcome(code = OutcomeCode.NOT_PROCEED, details = "details", reason = NotProceedReason.NOT_FAIR))
        },
      )
      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      val response = outcomeService.amendOutcomeViaService(
        adjudicationNumber = 1L,
        outcomeCodeToAmend = OutcomeCode.NOT_PROCEED,
        details = "updated",
        notProceedReason = NotProceedReason.WITNESS_NOT_ATTEND,
      )

      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())
      verify(nomisOutcomeService, atLeastOnce()).amendHearingResultIfApplicable(any(), any(), any())

      assertThat(argumentCaptor.value.getOutcomes().first()).isNotNull
      assertThat(argumentCaptor.value.getOutcomes().first().code).isEqualTo(OutcomeCode.NOT_PROCEED)
      assertThat(argumentCaptor.value.getOutcomes().first().details).isEqualTo("updated")
      assertThat(argumentCaptor.value.getOutcomes().first().reason).isEqualTo(NotProceedReason.WITNESS_NOT_ATTEND)
      assertThat(argumentCaptor.value.getOutcomes().first().quashedReason).isNull()

      assertThat(response).isNotNull
    }
  }
}
