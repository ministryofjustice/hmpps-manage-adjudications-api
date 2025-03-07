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
  val chargeNumber: String? = null,
  val prisonerNumber: String? = null,
  val hearingId: Long? = null,
  val punishmentId: Long? = null,
  val nomsNumber: String? = null,
  val reason: String? = null,
  val prisonId: String? = null,
  val status: String? = null,
  val removedNomsNumber: String? = null,
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

enum class AdjudicationDomainEventType(
  val value: String,
  val description: String,
  val auditType: AuditType,
  val incHearingId: Boolean = false,
) {
  ADJUDICATION_CREATED(
    "adjudication.report.created",
    "An adjudication has been created: ",
    AuditType.ADJUDICATION_CREATED,
  ),
  DAMAGES_UPDATED("adjudication.damages.updated", "Adjudication damages updated: ", AuditType.DAMAGES_UPDATED),
  EVIDENCE_UPDATED("adjudication.evidence.updated", "Adjudication evidence updated: ", AuditType.EVIDENCE_UPDATED),
  HEARING_CREATED("adjudication.hearing.created", "Adjudication hearing created: ", AuditType.HEARING_CREATED, true),
  HEARING_UPDATED("adjudication.hearing.updated", "Adjudication hearing updated: ", AuditType.HEARING_UPDATED, true),
  HEARING_DELETED("adjudication.hearing.deleted", "Adjudication hearing deleted: ", AuditType.HEARING_DELETED, true),
  HEARING_COMPLETED_CREATED(
    "adjudication.hearingCompleted.created",
    "Adjudication hearing completed created: ",
    AuditType.HEARING_COMPLETED_CREATED,
    true,
  ),
  HEARING_COMPLETED_DELETED(
    "adjudication.hearingCompleted.deleted",
    "Adjudication hearing completed deleted: ",
    AuditType.HEARING_COMPLETED_DELETED,
    true,
  ),
  HEARING_REFERRAL_CREATED(
    "adjudication.hearingReferral.created",
    "Adjudication hearing referral created: ",
    AuditType.HEARING_REFERRAL_CREATED,
    true,
  ),
  HEARING_REFERRAL_DELETED(
    "adjudication.hearingReferral.deleted",
    "Adjudication hearing referral deleted: ",
    AuditType.HEARING_REFERRAL_DELETED,
    true,
  ),
  HEARING_ADJOURN_CREATED(
    "adjudication.hearingAdjourn.created",
    "Adjudication hearing adjourn created: ",
    AuditType.HEARING_ADJOURN_CREATED,
    true,
  ),
  HEARING_ADJOURN_DELETED(
    "adjudication.hearingAdjourn.deleted",
    "Adjudication hearing adjourn deleted: ",
    AuditType.HEARING_ADJOURN_DELETED,
    true,
  ),
  HEARING_OUTCOME_UPDATED(
    "adjudication.hearingOutcome.updated",
    "Adjudication hearing outcome updated: ",
    AuditType.HEARING_OUTCOME_UPDATED,
    true,
  ),
  PUNISHMENTS_CREATED(
    "adjudication.punishments.created",
    "Adjudication punishments created: ",
    AuditType.PUNISHMENTS_CREATED,
  ),
  PUNISHMENTS_UPDATED(
    "adjudication.punishments.updated",
    "Adjudication punishments updated: ",
    AuditType.PUNISHMENTS_UPDATED,
  ),
  PUNISHMENTS_DELETED(
    "adjudication.punishments.deleted",
    "Adjudication punishments deleted: ",
    AuditType.PUNISHMENTS_DELETED,
  ),
  QUASHED("adjudication.outcome.quashed", "Adjudication quashed: ", AuditType.QUASHED),
  UNQUASHED("adjudication.outcome.unquashed", "Adjudication unquashed: ", AuditType.UNQUASHED),
  PROSECUTION_REFERRAL_OUTCOME(
    "adjudication.referral.outcome.prosecution",
    "Adjudication prosecution from referral: ",
    AuditType.REFERRAL_OUTCOME_PROSECUTION,
    true,
  ),
  NOT_PROCEED_REFERRAL_OUTCOME(
    "adjudication.referral.outcome.notProceed",
    "Adjudication not proceed from referral: ",
    AuditType.REFERRAL_OUTCOME_NOT_PROCEED,
    true,
  ),
  REFERRAL_OUTCOME_DELETED(
    "adjudication.referral.outcome.deleted",
    "Adjudication referral outcome deleted: ",
    AuditType.REFERRAL_OUTCOME_DELETED,
    true,
  ),
  REF_POLICE_OUTCOME(
    "adjudication.outcome.referPolice",
    "Adjudication referred to police: ",
    AuditType.REF_POLICE_OUTCOME_CREATED,
  ),
  NOT_PROCEED_OUTCOME(
    "adjudication.outcome.notProceed",
    "Adjudication not proceeded with: ",
    AuditType.NOT_PROCEED_OUTCOME_CREATED,
  ),
  OUTCOME_UPDATED("adjudication.outcome.updated", "Adjudication outcome updated: ", AuditType.OUTCOME_UPDATED),
  NOT_PROCEED_OUTCOME_DELETED(
    "adjudication.outcome.notProceed.deleted",
    "Adjudication not proceed outcome deleted: ",
    AuditType.NOT_PROCEED_OUTCOME_DELETED,
  ),
  REFERRAL_OUTCOME_REFER_GOV(
    "adjudication.referral.outcome.referGov",
    "Adjudication referral outcome refer gov created: ",
    AuditType.REFERRAL_OUTCOME_REFER_GOV,
  ),
  REFERRAL_DELETED("adjudication.referral.deleted", "Adjudication referral deleted: ", AuditType.REFERRAL_DELETED),
}

fun Instant.toOffsetDateFormat(): String = atZone(ZoneId.of("Europe/London")).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
