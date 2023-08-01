package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
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
  fun `create adjudication event sends chargeNumber and prisonId`() {
    whenever(featureFlagsConfig.adjudications).thenReturn(true)
    eventPublishService.publishEvent(AdjudicationDomainEventType.ADJUDICATION_CREATED, REPORTED_ADJUDICATION_DTO)

    verify(snsService, atLeastOnce()).publishDomainEvent(
      AdjudicationDomainEventType.ADJUDICATION_CREATED,
      "${AdjudicationDomainEventType.ADJUDICATION_CREATED.description} ${REPORTED_ADJUDICATION_DTO.chargeNumber}",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
      ),
    )
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
