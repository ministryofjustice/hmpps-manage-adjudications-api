package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.DamageRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.DamageCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedDamage
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

class DraftDamagesServiceTest : ReportedAdjudicationTestBase() {

  private val damagesService = DamagesService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    .also {
      it.damages = mutableListOf(
        ReportedDamage(code = DamageCode.CLEANING, details = "details", reporter = "Rod"),
        ReportedDamage(code = DamageCode.REDECORATION, details = "details 3", reporter = "Fred"),
      )
    }

  @BeforeEach
  fun init() {
    whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(reportedAdjudication)
    whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    reportedAdjudication.createdByUserId = "Jane"
    reportedAdjudication.createDateTime = LocalDateTime.now()
  }

  @Test
  fun `update damages for adjudication`() {
    val response = damagesService.updateDamages(
      1,
      listOf(
        DamageRequestItem(
          reportedAdjudication.damages.first().code,
          reportedAdjudication.damages.first().details,
          reportedAdjudication.damages.first().reporter,
        ),
        DamageRequestItem(DamageCode.ELECTRICAL_REPAIR, "details 2", "Fred"),
      ),
    )

    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.damages.size).isEqualTo(2)
    assertThat(argumentCaptor.value.damages.first().code).isEqualTo(DamageCode.CLEANING)
    assertThat(argumentCaptor.value.damages.first().details).isEqualTo("details")
    assertThat(argumentCaptor.value.damages.first().reporter).isEqualTo("Rod")
    assertThat(argumentCaptor.value.damages.last().code).isEqualTo(DamageCode.ELECTRICAL_REPAIR)
    assertThat(argumentCaptor.value.damages.last().details).isEqualTo("details 2")
    assertThat(argumentCaptor.value.damages.last().reporter).isEqualTo("Fred")

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByReportNumber(any())).thenReturn(null)

    Assertions.assertThatThrownBy {
      damagesService.updateDamages(1, listOf(DamageRequestItem(DamageCode.CLEANING, "details")))
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
