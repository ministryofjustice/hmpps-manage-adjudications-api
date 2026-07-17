package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.ObjectMapperConfig
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.TestControllerBase.Companion.REPORTED_ADJUDICATION_DTO
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsChangeType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsDetailsDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsEventDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.LossOfVisitsPunishmentDto
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.dtos.SuspendedPunishmentEvent
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.Measurement
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.PunishmentType
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.entities.ReportedAdjudicationStatus
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class EventPublishServiceTest : ReportedAdjudicationTestBase() {

  private val snsService: SnsService = mock()
  private val auditService: AuditService = mock()
  private val clock: Clock = Clock.fixed(Instant.ofEpochMilli(0), ZoneId.systemDefault())

  private val eventPublishService = EventPublishService(
    snsService = snsService,
    auditService = auditService,
    clock = clock,
  )

  @Test
  fun `create adjudication event sends chargeNumber and prisonId when async is true`() {
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
  fun `existing event JSON does not gain an empty loss of visits field`() {
    val objectMapper = requireNotNull(ObjectMapperConfig().objectMapper(Jackson2ObjectMapperBuilder.json()))
    val event = HMPPSDomainEvent(
      eventType = AdjudicationDomainEventType.ADJUDICATION_CREATED.value,
      additionalInformation = AdditionalInformation(
        chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
        status = REPORTED_ADJUDICATION_DTO.status.name,
      ),
      occurredAt = Instant.EPOCH,
      description = "existing event",
    )

    val json = objectMapper.writeValueAsString(event)

    org.assertj.core.api.Assertions.assertThat(json).doesNotContain("lossOfVisits")
  }

  @Test
  fun `loss of visits event includes the complete post-change visits state`() {
    val details = LossOfVisitsDetailsDto(
      changeType = LossOfVisitsChangeType.UPDATED,
      punishments = listOf(
        LossOfVisitsPunishmentDto(
          punishmentId = 10,
          type = PunishmentType.RESTRICTION_OF_SOCIAL_VISITS,
          duration = 28,
          measurement = Measurement.DAYS,
          startDate = LocalDate.of(2026, 7, 16),
          endDate = LocalDate.of(2026, 8, 12),
          hasChildUnder18 = true,
        ),
      ),
    )
    val event = LossOfVisitsEventDto(
      chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
      prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
      prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
      status = REPORTED_ADJUDICATION_DTO.status,
      details = details,
    )

    eventPublishService.publishLossOfVisitsEvent(event)

    verify(snsService).publishDomainEvent(
      AdjudicationDomainEventType.LOSS_OF_VISITS,
      "${AdjudicationDomainEventType.LOSS_OF_VISITS.description} ${event.chargeNumber}",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = event.chargeNumber,
        prisonId = event.prisonId,
        prisonerNumber = event.prisonerNumber,
        status = event.status.name,
        lossOfVisits = details,
      ),
    )
    verifyNoMoreInteractions(snsService)
  }

  @CsvSource(
    "HEARING_CREATED",
    "HEARING_UPDATED",
    "HEARING_DELETED",
    "HEARING_COMPLETED_CREATED",
    "HEARING_COMPLETED_DELETED",
    "HEARING_REFERRAL_CREATED",
    "HEARING_REFERRAL_DELETED",
    "HEARING_OUTCOME_UPDATED",
    "HEARING_ADJOURN_CREATED",
    "HEARING_ADJOURN_DELETED",
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

  @Test
  fun `send additional events if punishments version = 2 and additional events are present to be sent`() {
    val eventPublishServiceV2 = EventPublishService(
      snsService = snsService,
      auditService = auditService,
      clock = clock,
    )

    eventPublishServiceV2.publishEvent(
      AdjudicationDomainEventType.PUNISHMENTS_CREATED,
      REPORTED_ADJUDICATION_DTO.also {
        it.suspendedPunishmentEvents = setOf(
          SuspendedPunishmentEvent(
            agencyId = "LEI",
            chargeNumber = "suspended",
            status = ReportedAdjudicationStatus.CHARGE_PROVED,
          ),
        )
      },
    )

    verify(snsService, atLeastOnce()).publishDomainEvent(
      AdjudicationDomainEventType.PUNISHMENTS_CREATED,
      "${AdjudicationDomainEventType.PUNISHMENTS_CREATED.description} ${REPORTED_ADJUDICATION_DTO.chargeNumber}",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = REPORTED_ADJUDICATION_DTO.chargeNumber,
        prisonId = REPORTED_ADJUDICATION_DTO.originatingAgencyId,
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        status = REPORTED_ADJUDICATION_DTO.status.name,
      ),
    )

    verify(snsService, atLeastOnce()).publishDomainEvent(
      AdjudicationDomainEventType.PUNISHMENTS_UPDATED,
      "${AdjudicationDomainEventType.PUNISHMENTS_UPDATED.description} suspended",
      LocalDateTime.now(clock),
      AdditionalInformation(
        chargeNumber = "suspended",
        prisonId = "LEI",
        prisonerNumber = REPORTED_ADJUDICATION_DTO.prisonerNumber,
        status = ReportedAdjudicationStatus.CHARGE_PROVED.name,
      ),
    )
  }

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
