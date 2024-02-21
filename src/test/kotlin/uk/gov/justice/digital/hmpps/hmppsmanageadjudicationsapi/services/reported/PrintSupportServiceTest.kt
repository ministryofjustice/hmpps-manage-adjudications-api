package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Punishment
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentSchedule
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import java.time.LocalDate
import java.time.LocalDateTime

class PrintSupportServiceTest : ReportedAdjudicationTestBase() {

  private val printSupportService = PrintSupportService(reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade)

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    Assertions.assertThatThrownBy {
      printSupportService.getDis5Data(
        chargeNumber = "1",
      )
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class Dis5 {
    @Test
    fun `get dis5 data for current establishment equal to originating agency`() {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1),
          entityBuilder.reportedAdjudication(chargeNumber = "88888", offenderBookingId = 1).also {
            it.overrideAgencyId = report.originatingAgencyId
          },
          entityBuilder.reportedAdjudication(agencyId = "YYY", chargeNumber = "88888", offenderBookingId = 1).also {
            it.overrideAgencyId = "XXX"
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.previousAtCurrentEstablishmentCount).isEqualTo(2)
      assertThat(data.chargeNumber).isEqualTo(report.chargeNumber)
      assertThat(data.dateOfIncident).isEqualTo(report.dateTimeOfIncident.toLocalDate())
      assertThat(data.dateOfDiscovery).isEqualTo(report.dateTimeOfDiscovery.toLocalDate())
      assertThat(data.previousCount).isEqualTo(3)
    }

    @Test
    fun `get dis5 data for current establishment equal to override agency`() {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1).also {
        it.overrideAgencyId = "LEI"
      }
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1).also {
            it.originatingAgencyId = "LEI"
          },
          entityBuilder.reportedAdjudication(chargeNumber = "88888", offenderBookingId = 1).also {
            it.originatingAgencyId = "LEI"
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.previousAtCurrentEstablishmentCount).isEqualTo(2)
      assertThat(data.previousCount).isEqualTo(2)
    }

    @Test
    fun `active suspended punishments are listed`() {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1),
          entityBuilder.reportedAdjudication(chargeNumber = "88888", offenderBookingId = 1).also {
            it.overrideAgencyId = report.originatingAgencyId
            // valid
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now().plusDays(5),
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now().plusDays(5)),
                ),
              ),
            )
            // suspended but cut off
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                suspendedUntil = LocalDate.now().minusDays(5),
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now().minusDays(5)),
                ),
              ),
            )
            // not suspended
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10),
                ),
              ),
            )
            // corrupted
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                actualCreatedDate = LocalDateTime.now().plusDays(5),
                suspendedUntil = LocalDate.now().plusDays(5),
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10, suspendedUntil = LocalDate.now().plusDays(5)),
                ),
              ),
            )
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.suspendedPunishments.size).isEqualTo(1)
    }

    @Test
    fun `match same offence using DPS created ids`() {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1),
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.sameOffenceCount).isEqualTo(1)
    }

    @CsvSource("51:1B", "51:25D")
    @ParameterizedTest
    fun `match same offence from migrated data - see OffenceCodes MIGRATED_OFFENCE`(nomisOffenceCode: String) {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1).also {
            it.offenceDetails.first().offenceCode = 0
            it.offenceDetails.first().nomisOffenceCode = nomisOffenceCode
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.sameOffenceCount).isEqualTo(1)
      assertThat(data.lastReportedOffence!!.chargeNumber).isEqualTo("99999")
    }

    @Test
    fun `last reported offence details set`() {
      val report = entityBuilder.reportedAdjudication(offenderBookingId = 1)
      whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(report)
      whenever(reportedAdjudicationRepository.findByOffenderBookingIdAndStatus(any(), any())).thenReturn(
        listOf(
          entityBuilder.reportedAdjudication(chargeNumber = "77777", offenderBookingId = 1),
          entityBuilder.reportedAdjudication(chargeNumber = "99999", offenderBookingId = 1).also {
            it.dateTimeOfDiscovery = LocalDateTime.now().plusDays(10)
            it.statement = "the new statement"
            it.dateTimeOfIncident = LocalDateTime.now().plusDays(11)
            it.addPunishment(
              Punishment(
                type = PunishmentType.CONFINEMENT,
                schedule = mutableListOf(
                  PunishmentSchedule(days = 10),
                ),
              ),
            )
          },
        ),
      )

      val data = printSupportService.getDis5Data(chargeNumber = "12345")
      assertThat(data.lastReportedOffence).isNotNull
      assertThat(data.lastReportedOffence!!.chargeNumber).isEqualTo("99999")
      assertThat(data.lastReportedOffence!!.punishments.size).isEqualTo(1)
      assertThat(data.lastReportedOffence!!.dateOfIncident).isEqualTo(LocalDate.now().plusDays(11))
      assertThat(data.lastReportedOffence!!.dateOfDiscovery).isEqualTo(LocalDate.now().plusDays(10))
      assertThat(data.lastReportedOffence!!.statement).isEqualTo("the new statement")
    }
  }
}
