package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class HearingServiceTest : ReportedAdjudicationTestBase() {
  private var hearingService = HearingService(
    reportedAdjudicationRepository, offenceCodeLookupService, authenticationFacade
  )

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

    Assertions.assertThatThrownBy {
      hearingService.createHearing(1, 1, LocalDateTime.now())
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.amendHearing(1, 1, 1, LocalDateTime.now())
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")

    Assertions.assertThatThrownBy {
      hearingService.deleteHearing(1, 1)
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }

  @Nested
  inner class CreateHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
        it.hearings.clear()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `create a hearing`() {
      val now = LocalDateTime.now()
      val response = hearingService.createHearing(
        1235L,
        1,
        now
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now)
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().reportNumber).isEqualTo(reportedAdjudication.reportNumber)

      assertThat(response).isNotNull
    }
  }

  @Nested
  inner class AmendHearing {

    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `amend a hearing`() {
      val now = LocalDateTime.now()
      val response = hearingService.amendHearing(
        1235L,
        1,
        2,
        now.plusDays(1)
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(1)
      assertThat(argumentCaptor.value.hearings.first().locationId).isEqualTo(2)
      assertThat(argumentCaptor.value.hearings.first().dateTimeOfHearing).isEqualTo(now.plusDays(1))
      assertThat(argumentCaptor.value.hearings.first().agencyId).isEqualTo(reportedAdjudication.agencyId)
      assertThat(argumentCaptor.value.hearings.first().reportNumber).isEqualTo(reportedAdjudication.reportNumber)

      assertThat(response).isNotNull
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.amendHearing(1, 1, 1, LocalDateTime.now())
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }
  }

  @Nested
  inner class DeleteHearing {
    private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
      .also {
        it.createdByUserId = ""
        it.createDateTime = LocalDateTime.now()
      }

    @BeforeEach
    fun init() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
      whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    }

    @Test
    fun `throws an entity not found if the hearing for the supplied id does not exists`() {
      whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(
        reportedAdjudication
          .also { it.hearings.clear() }
      )

      Assertions.assertThatThrownBy {
        hearingService.deleteHearing(1, 1)
      }.isInstanceOf(EntityNotFoundException::class.java)
        .hasMessageContaining("Hearing not found for 1")
    }

    @Test
    fun `delete a hearing`() {
      val response = hearingService.deleteHearing(
        1235L,
        1,
      )

      val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
      verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

      assertThat(argumentCaptor.value.hearings.size).isEqualTo(0)

      assertThat(response).isNotNull
    }
  }
}
