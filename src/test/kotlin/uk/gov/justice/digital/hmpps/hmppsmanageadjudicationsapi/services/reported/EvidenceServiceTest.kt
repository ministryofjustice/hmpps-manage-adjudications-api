package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.EvidenceRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.EvidenceCode
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedEvidence
import java.time.LocalDateTime

class EvidenceServiceTest : ReportedAdjudicationTestBase() {
  private val evidenceService = EvidenceService(
    reportedAdjudicationRepository,
    offenceCodeLookupService,
    authenticationFacade,
  )

  private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    .also {
      it.evidence = mutableListOf(
        ReportedEvidence(code = EvidenceCode.PHOTO, identifier = "identifier", details = "details", reporter = "Rod"),
        ReportedEvidence(
          code = EvidenceCode.BAGGED_AND_TAGGED,
          identifier = "identifier",
          details = "details 3",
          reporter = "Fred",
        ),
      )
    }

  @BeforeEach
  fun init() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(reportedAdjudication)
    whenever(reportedAdjudicationRepository.save(any())).thenReturn(reportedAdjudication)
    whenever(authenticationFacade.currentUsername).thenReturn("Fred")
    reportedAdjudication.createdByUserId = "Jane"
    reportedAdjudication.createDateTime = LocalDateTime.now()
  }

  @Test
  fun `update evidence for adjudication`() {
    val response = evidenceService.updateEvidence(
      "1",
      listOf(
        EvidenceRequestItem(
          reportedAdjudication.evidence.first().code,
          reportedAdjudication.evidence.first().identifier,
          reportedAdjudication.evidence.first().details,
          reportedAdjudication.evidence.first().reporter,
        ),
        EvidenceRequestItem(EvidenceCode.BODY_WORN_CAMERA, "identifier 2", "details 2", "Fred"),
      ),
    )

    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.evidence.size).isEqualTo(2)
    assertThat(argumentCaptor.value.evidence.first().code).isEqualTo(EvidenceCode.PHOTO)
    assertThat(argumentCaptor.value.evidence.first().details).isEqualTo("details")
    assertThat(argumentCaptor.value.evidence.first().identifier).isEqualTo("identifier")
    assertThat(argumentCaptor.value.evidence.first().reporter).isEqualTo("Rod")
    assertThat(argumentCaptor.value.evidence.last().code).isEqualTo(EvidenceCode.BODY_WORN_CAMERA)
    assertThat(argumentCaptor.value.evidence.last().details).isEqualTo("details 2")
    assertThat(argumentCaptor.value.evidence.last().identifier).isEqualTo("identifier 2")
    assertThat(argumentCaptor.value.evidence.last().reporter).isEqualTo("Fred")
    assertThat(argumentCaptor.value.lastModifiedAgencyId).isNull()

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)

    Assertions.assertThatThrownBy {
      evidenceService.updateEvidence("1", listOf(EvidenceRequestItem(EvidenceCode.PHOTO, "", "details")))
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
