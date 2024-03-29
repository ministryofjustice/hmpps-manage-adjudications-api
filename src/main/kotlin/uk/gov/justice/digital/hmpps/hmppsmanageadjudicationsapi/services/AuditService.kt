package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security.AuthenticationFacade
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.Instant

@Service
class AuditService(
  @Value("\${spring.application.name}")
  private val serviceName: String,
  private val hmppsQueueService: HmppsQueueService,
  private val telemetryClient: TelemetryClient,
  private val objectMapper: ObjectMapper,
  private val authenticationFacade: AuthenticationFacade,
) {
  private val auditQueue by lazy { hmppsQueueService.findByQueueId("audit") as HmppsQueue }
  private val auditSqsClient by lazy { auditQueue.sqsClient }
  private val auditQueueUrl by lazy { auditQueue.queueUrl }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sendMessage(auditType: AuditType, id: String, details: Any, username: String? = null) {
    val auditEvent = AuditEvent(
      what = auditType.name,
      who = username ?: authenticationFacade.currentUsername ?: "adjudications-api",
      service = serviceName,
      details = objectMapper.writeValueAsString(details),
    )
    log.debug("Audit {} ", auditEvent)

    val result =
      auditSqsClient.sendMessage(
        SendMessageRequest.builder()
          .queueUrl(auditQueueUrl)
          .messageBody(auditEvent.toJson())
          .build(),
      ).get()

    telemetryClient.trackEvent(
      auditEvent.what,
      mapOf("messageId" to result.messageId(), "id" to id),
      null,
    )
  }

  private fun Any.toJson() = objectMapper.writeValueAsString(this)
}

data class AuditEvent(
  val what: String,
  val `when`: Instant = Instant.now(),
  val who: String,
  val service: String,
  val details: String? = null,
)

enum class AuditType {
  ADJUDICATION_CREATED,
  DAMAGES_UPDATED,
  EVIDENCE_UPDATED,
  HEARING_CREATED,
  HEARING_UPDATED,
  HEARING_DELETED,
  HEARING_COMPLETED_CREATED,
  HEARING_COMPLETED_DELETED,
  HEARING_REFERRAL_CREATED,
  HEARING_REFERRAL_DELETED,
  HEARING_ADJOURN_CREATED,
  HEARING_ADJOURN_DELETED,
  HEARING_OUTCOME_UPDATED,
  PUNISHMENTS_CREATED,
  PUNISHMENTS_UPDATED,
  PUNISHMENTS_DELETED,
  QUASHED,
  UNQUASHED,
  REFERRAL_OUTCOME_PROSECUTION,
  REFERRAL_OUTCOME_NOT_PROCEED,
  REFERRAL_OUTCOME_DELETED,
  REF_POLICE_OUTCOME_CREATED,
  NOT_PROCEED_OUTCOME_CREATED,
  OUTCOME_UPDATED,
  NOT_PROCEED_OUTCOME_DELETED,
  REFERRAL_OUTCOME_REFER_GOV,
  REFERRAL_DELETED,
}
