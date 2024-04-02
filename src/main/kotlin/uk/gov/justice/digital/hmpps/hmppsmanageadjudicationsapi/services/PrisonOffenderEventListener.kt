package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrisonOffenderEventListener(
  private val mapper: ObjectMapper,
  private val transferService: TransferService,
  private val prisonerMergeService: PrisonerMergeService,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)

    const val PRISONER_TRANSFER_EVENT_TYPE = "prisoner-offender-search.prisoner.received"
    const val PRISONER_MERGE_EVENT_TYPE = "prison-offender-events.prisoner.merged"

    fun isValidReason(reason: String?): Boolean {
      val toTest = reason ?: return false

      return try {
        Reason.values().any { it == Reason.valueOf(toTest) }
      } catch (e: IllegalArgumentException) {
        false
      }
    }
  }

  @SqsListener("adjudications", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-adjudications-prisoner-event-queue", kind = SpanKind.SERVER)
  fun onPrisonOffenderEvent(requestJson: String) {
    val (message, messageAttributes) = mapper.readValue(requestJson, HMPPSMessage::class.java)
    val eventType = messageAttributes.eventType.Value
    log.info("Received message $message, type $eventType")

    val hmppsDomainEvent = mapper.readValue(message, HMPPSDomainEvent::class.java)
    when (eventType) {
      PRISONER_TRANSFER_EVENT_TYPE -> {
        if (isValidReason(hmppsDomainEvent.additionalInformation?.reason)) {
          transferService.processTransferEvent(
            prisonerNumber = hmppsDomainEvent.additionalInformation?.nomsNumber,
            agencyId = hmppsDomainEvent.additionalInformation?.prisonId,
          )
        }
      }
      PRISONER_MERGE_EVENT_TYPE -> {
        if (hmppsDomainEvent.additionalInformation?.reason == "MERGE") {
          prisonerMergeService.merge(
            prisonerFrom = hmppsDomainEvent.additionalInformation.removedNomsNumber,
            prisonerTo = hmppsDomainEvent.additionalInformation.nomsNumber,
          )
        }
      }
      else -> {
        log.debug("Ignoring message with type $eventType")
      }
    }
  }
}

enum class Reason {
  TRANSFERRED,
  NEW_ADMISSION,
  READMISSION,
  TEMPORARY_ABSENCE_RETURN,
  RETURN_FROM_COURT,
}

data class HMPPSEventType(val Value: String, val Type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes,
)
