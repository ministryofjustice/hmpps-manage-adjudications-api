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
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.draft.WitnessRequestItem
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudication
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedWitness
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.WitnessCode
import java.time.LocalDateTime

class WitnessesServiceTest : ReportedAdjudicationTestBase() {
  private val witnessesService = WitnessesService(
    reportedAdjudicationRepository,
    authenticationFacade,
  )

  private val reportedAdjudication = entityBuilder.reportedAdjudication(dateTime = DATE_TIME_OF_INCIDENT)
    .also {
      it.witnesses = mutableListOf(
        ReportedWitness(code = WitnessCode.STAFF, firstName = "first", lastName = "last", reporter = "Rod"),
        ReportedWitness(code = WitnessCode.OFFICER, firstName = "first", lastName = "last", reporter = "Fred"),
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
  fun `update witnesses for adjudication`() {
    val response = witnessesService.updateWitnesses(
      "1",
      listOf(
        WitnessRequestItem(
          reportedAdjudication.witnesses.first().code,
          reportedAdjudication.witnesses.first().firstName,
          reportedAdjudication.witnesses.first().lastName,
          reportedAdjudication.witnesses.first().reporter,
        ),
        WitnessRequestItem(WitnessCode.OTHER_PERSON, "first", "last", "Fred"),
      ),
    )

    val argumentCaptor = ArgumentCaptor.forClass(ReportedAdjudication::class.java)
    verify(reportedAdjudicationRepository).save(argumentCaptor.capture())

    assertThat(argumentCaptor.value.witnesses.size).isEqualTo(2)
    assertThat(argumentCaptor.value.witnesses.first().code).isEqualTo(WitnessCode.STAFF)
    assertThat(argumentCaptor.value.witnesses.first().firstName).isEqualTo("first")
    assertThat(argumentCaptor.value.witnesses.first().reporter).isEqualTo("Rod")
    assertThat(argumentCaptor.value.witnesses.last().code).isEqualTo(WitnessCode.OTHER_PERSON)
    assertThat(argumentCaptor.value.witnesses.last().firstName).isEqualTo("first")
    assertThat(argumentCaptor.value.witnesses.last().reporter).isEqualTo("Fred")
    assertThat(argumentCaptor.value.lastModifiedAgencyId).isNull()

    assertThat(response).isNotNull
  }

  @Test
  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    whenever(reportedAdjudicationRepository.findByChargeNumber(any())).thenReturn(null)

    Assertions.assertThatThrownBy {
      witnessesService.updateWitnesses("1", listOf(WitnessRequestItem(WitnessCode.STAFF, "first", "last")))
    }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessageContaining("ReportedAdjudication not found for 1")
  }
}
