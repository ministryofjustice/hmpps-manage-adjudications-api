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
  private lateinit var prisonOffenderEventListener: PrisonOffenderEventListener
  private val objectMapper = ObjectMapper().findAndRegisterModules().apply {
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  @BeforeEach
  fun setUp() {
    prisonOffenderEventListener = PrisonOffenderEventListener(objectMapper, transferService)
  }

  @Test
  fun `transfer event calls transfer service`() {
    prisonOffenderEventListener.onPrisonOffenderEvent("/messages/transfer.json".readResourceAsText())

    verify(transferService, atLeastOnce()).processTransferEvent("AA1234A", "TJW")
  }

  @Test
  fun `return from court does not call transfer service`() {
    prisonOffenderEventListener.onPrisonOffenderEvent("/messages/not_transfer.json".readResourceAsText())

    verify(transferService, never()).processTransferEvent(any(), any())
  }

  private fun String.readResourceAsText(): String {
    return PrisonOffenderEventListenerTest::class.java.getResource(this)?.readText()
      ?: throw AssertionError("can not find file")
  }
}
