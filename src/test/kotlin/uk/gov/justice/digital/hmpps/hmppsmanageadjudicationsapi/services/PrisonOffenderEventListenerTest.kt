package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class PrisonOffenderEventListenerTest {

  private val transferService: TransferService = mock()
  private val prisonerMergeService: PrisonerMergeService = mock()
  private lateinit var prisonOffenderEventListener: PrisonOffenderEventListener
  private val objectMapper = ObjectMapper().findAndRegisterModules().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  @BeforeEach
  fun setUp() {
    prisonOffenderEventListener = PrisonOffenderEventListener(objectMapper, transferService, prisonerMergeService)
  }

  @Test
  fun `transfer event calls transfer service`() {
    prisonOffenderEventListener.onPrisonOffenderEvent("/messages/transfer.json".readResourceAsText())

    verify(transferService, atLeastOnce()).processTransferEvent("AA1234A", "BXI")
  }

  @Test
  fun `return from court does not call transfer service`() {
    prisonOffenderEventListener.onPrisonOffenderEvent("/messages/not_transfer.json".readResourceAsText())

    verify(transferService, never()).processTransferEvent(any(), any())
  }

  @Test
  fun `prisoner merge calls prisoner merge service`() {
    prisonOffenderEventListener.onPrisonOffenderEvent("/messages/merge.json".readResourceAsText())

    verify(prisonerMergeService, atLeastOnce()).merge(prisonerFrom = "A0237FC", prisonerTo = "A3203AJ")
  }

  private fun String.readResourceAsText(): String = PrisonOffenderEventListenerTest::class.java.getResource(this)?.readText()
    ?: throw AssertionError("can not find file")
}
