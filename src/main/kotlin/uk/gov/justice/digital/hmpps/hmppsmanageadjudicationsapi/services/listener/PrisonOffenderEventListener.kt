package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.listener

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.HMPPSDomainEvent

@Service
class PrisonOffenderEventListener(
  private val mapper: ObjectMapper,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("adjudications", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "hmpps-adjudications-prisoner-event-queue", kind = SpanKind.SERVER)
  fun onPrisonOffenderEvent(requestJson: String) {
    val (message, messageAttributes) = mapper.readValue(requestJson, HMPPSMessage::class.java)
    val eventType = messageAttributes.eventType.Value
    log.info("Received message $message, type $eventType")

    val hmppsDomainEvent = mapper.readValue(message, HMPPSDomainEvent::class.java)
    when (eventType) {
      "prisoner-offender-search.prisoner.received" -> {
        // TODO: call to update prisoner location
      }
      "prison-offender-events.prisoner.merged" -> {
        // TODO: call to update prisoner number
      }
      else -> {
        log.debug("Ignoring message with type $eventType")
      }
    }
  }
}

data class HMPPSEventType(val Value: String, val Type: String)
data class HMPPSMessageAttributes(val eventType: HMPPSEventType)
data class HMPPSMessage(
  val Message: String,
  val MessageAttributes: HMPPSMessageAttributes,
)
