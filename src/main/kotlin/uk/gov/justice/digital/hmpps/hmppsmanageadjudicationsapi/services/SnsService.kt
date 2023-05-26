package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Service
class SnsService(hmppsQueueService: HmppsQueueService, private val objectMapper: ObjectMapper) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val domaineventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw RuntimeException("Topic with name domainevents doesn't exist")
  }
  private val domaineventsTopicClient by lazy { domaineventsTopic.snsClient }

  @WithSpan(value = "hmpps-domain-events-topic", kind = SpanKind.PRODUCER)
  fun publishDomainEvent(
    eventType: AdjudicationDomainEventType,
    description: String,
    occurredAt: LocalDateTime,
    additionalInformation: AdditionalInformation? = null,
  ) {
    publishToDomainEventsTopic(
      HMPPSDomainEvent(
        eventType.value,
        additionalInformation,
        occurredAt.atZone(ZoneId.systemDefault()).toInstant(),
        description,
      ),
    )
  }

  private fun publishToDomainEventsTopic(payload: HMPPSDomainEvent) {
    log.debug("Event {} for id {}", payload.eventType, payload.additionalInformation)
    domaineventsTopicClient.publish(
      PublishRequest.builder()
        .topicArn(domaineventsTopic.arn)
        .message(objectMapper.writeValueAsString(payload))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(payload.eventType).build(),
          ),
        )
        .build()
        .also { log.info("Published event $payload to outbound topic") },
    )
  }
}

data class AdditionalInformation(
  val adjudicationNumber: String? = null,
  val prisonerNumber: String? = null,
  val reason: String? = null,
  val bookingId: Long? = null,
  val prisonId: String? = null,
  val hearingNumber: String? = null,
  val sanctionSequence: Long? = null,
)

data class HMPPSDomainEvent(
  val eventType: String? = null,
  val additionalInformation: AdditionalInformation?,
  val version: String,
  val occurredAt: String,
  val description: String,
) {
  constructor(
    eventType: String,
    additionalInformation: AdditionalInformation?,
    occurredAt: Instant,
    description: String,
  ) : this(
    eventType,
    additionalInformation,
    "1.0",
    occurredAt.toOffsetDateFormat(),
    description,
  )
}

enum class AdjudicationDomainEventType(val value: String) {
  ADJUDICATION_CREATED("adjudication.report.created"),
  HEARING_CREATED("adjudication.hearing.created"),
  HEARING_UPDATED("adjudication.hearing.updated"),
  HEARING_DELETED("adjudication.hearing.deleted"),
  HEARING_RESULT_CREATED("adjudication.hearing.result.created"),
  HEARING_RESULT_UPDATED("adjudication.hearing.result.updated"),
  HEARING_RESULT_DELETED("adjudication.hearing.result.deleted"),
  SANCTION_CREATED("adjudication.sanction.created"),
  SANCTION_UDPATED("adjudication.sanction.updated"),
  SANCTION_DELETED("adjudication.sanction.deleted"),
  SANCTIONS_DELETED("adjudication.sanctions.deleted"),
  SANCTIONS_QUASHED("adjudication.sanction.quashed"),
}

fun Instant.toOffsetDateFormat(): String =
  atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
