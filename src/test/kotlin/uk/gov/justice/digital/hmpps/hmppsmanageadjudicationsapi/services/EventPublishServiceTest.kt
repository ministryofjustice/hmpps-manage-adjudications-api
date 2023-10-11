package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase.Companion.REPORTED_ADJUDICATION_DTO
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class EventPublishServiceTest : ReportedAdjudicationTestBase() {

  private val snsService: SnsService = mock()
  private val auditService: AuditService = mock()
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())
  private val featureFlagsConfig: FeatureFlagsConfig = mock()

  private val eventPublishService = EventPublishService(
    snsService = snsService,
    auditService = auditService,
    clock = clock,
    featureFlagsConfig = featureFlagsConfig,
  )

  @Test
  fun `create adjudication event sends chargeNumber and prisonId when async is true`() {
    whenever(featureFlagsConfig.adjudications).thenReturn(true)
    eventPublishService.publishEvent(AdjudicationDomainEventType.ADJUDICATION_CREATED, REPORTED_ADJUDICATION_DTO)

    verify(snsService, atLeastOnce()).publishDomainEvent(
      AdjudicationDomainEventType.ADJUDICATION_CREATED,
      "${AdjudicationDomainEventType.ADJUDICATION_CREATED.description} ${REPORTED_ADJUDICATION_DTO.chargeNumber}",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        status = REPORTED_ADJUDICATION_DTO.status.name,
      ),
    )
  }

  @Test
  fun `create adjudication event is not sent when async is false`() {
    whenever(featureFlagsConfig.adjudications).thenReturn(false)
    eventPublishService.publishEvent(AdjudicationDomainEventType.ADJUDICATION_CREATED, REPORTED_ADJUDICATION_DTO)

    verify(snsService, never()).publishDomainEvent(any(), any(), any(), anyOrNull())
  }

  @CsvSource(
    "HEARING_CREATED", "HEARING_UPDATED", "HEARING_DELETED", "HEARING_COMPLETED_CREATED", "HEARING_COMPLETED_DELETED", "HEARING_REFERRAL_CREATED",
    "HEARING_REFERRAL_DELETED", "HEARING_OUTCOME_UPDATED", "HEARING_ADJOURN_CREATED", "HEARING_ADJOURN_DELETED",
  )
  @ParameterizedTest
  fun `hearing event provides hearing id`(event: AdjudicationDomainEventType) {
    eventPublishService.publishEvent(event, REPORTED_ADJUDICATION_DTO.also { it.hearingIdActioned = 1 })

    verify(snsService, atLeastOnce()).publishDomainEvent(
      event,
      "${event.description} ${REPORTED_ADJUDICATION_DTO.chargeNumber}",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        hearingId = 1,
        status = REPORTED_ADJUDICATION_DTO.status.name,
      ),
    )
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
