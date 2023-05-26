package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config.FeatureFlagsService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.AdjudicationDetailsToPublish
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.LegacyNomisGateway
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.NomisAdjudicationCreationRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OffenderOicSanctionRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingRequest
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.OicHearingResultRequest
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class EventWrapperService(
  private val snsService: SnsService,
  private val clock: Clock,
  private val legacyNomisGateway: LegacyNomisGateway,
  private val featureFlagsService: FeatureFlagsService,
  private val auditService: AuditService,
) {

  fun requestAdjudicationCreationData(offenderNo: String): NomisAdjudicationCreationRequest {
    return if (featureFlagsService.isEmitEventsForAdjudications()) {
      val bookingId = legacyNomisGateway.getPrisonerInfo(offenderNo)?.bookingId ?: throw EntityNotFoundException(offenderNo)
      NomisAdjudicationCreationRequest(adjudicationNumber = UUID.randomUUID().toString(), bookingId = bookingId)
    } else {
      legacyNomisGateway.requestAdjudicationCreationData(offenderNo)
    }
  }

  fun publishAdjudication(adjudicationDetailsToPublish: AdjudicationDetailsToPublish) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.ADJUDICATION_CREATED,
        "An adjudication has been created: ${adjudicationDetailsToPublish.adjudicationNumber}",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationDetailsToPublish.adjudicationNumber,
          prisonerNumber = adjudicationDetailsToPublish.offenderNo,
          prisonId = adjudicationDetailsToPublish.agencyId,
          bookingId = adjudicationDetailsToPublish.bookingId,
        ),
      )
    } else {
      legacyNomisGateway.publishAdjudication(adjudicationDetailsToPublish)
    }

    auditService.sendMessage(
      AuditType.ADJUDICATION_CREATED,
      adjudicationDetailsToPublish.adjudicationNumber,
      adjudicationDetailsToPublish,
    )
  }

  fun createHearing(adjudicationNumber: String, oicHearingRequest: OicHearingRequest): String {
    val hearingId = if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_CREATED,
        "Hearing (ID=${oicHearingRequest.oicHearingId}) has been created for adjudication number $adjudicationNumber",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingRequest.oicHearingId,
        ),
      )
      oicHearingRequest.oicHearingId
    } else {
      legacyNomisGateway.createHearing(adjudicationNumber.toLong(), oicHearingRequest).toString()
    }

    auditService.sendMessage(
      AuditType.HEARING_CREATED,
      adjudicationNumber,
      oicHearingRequest,
    )

    return hearingId
  }

  fun amendHearing(adjudicationNumber: String, oicHearingRequest: OicHearingRequest) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_UPDATED,
        "Hearing (ID=${oicHearingRequest.oicHearingId}) has been updated for adjudication number $adjudicationNumber",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingRequest.oicHearingId,
        ),
      )
    } else {
      legacyNomisGateway.amendHearing(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingRequest.oicHearingId.toLong(),
        oicHearingRequest = oicHearingRequest,
      )
    }
  }

  fun deleteHearing(adjudicationNumber: String, oicHearingId: String) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_DELETED,
        "Hearing (ID=$oicHearingId}) has been deleted for adjudication number $adjudicationNumber",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingId,
        ),
      )
    } else {
      legacyNomisGateway.deleteHearing(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId.toLong(),
      )
    }
  }

  fun createHearingResult(
    adjudicationNumber: String,
    oicHearingId: String,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_RESULT_CREATED,
        "Hearing result has been created for adjudication number $adjudicationNumber, hearing $oicHearingId",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingId,
        ),
      )
    } else {
      legacyNomisGateway.createHearingResult(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId.toLong(),
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun amendHearingResult(
    adjudicationNumber: String,
    oicHearingId: String,
    oicHearingResultRequest: OicHearingResultRequest,
  ) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_RESULT_UPDATED,
        "Hearing result has been updated for adjudication number $adjudicationNumber, hearing $oicHearingId",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingId,
        ),
      )
    } else {
      legacyNomisGateway.amendHearingResult(
        adjudicationNumber = adjudicationNumber.toLong(),
        oicHearingId = oicHearingId.toLong(),
        oicHearingResultRequest = oicHearingResultRequest,
      )
    }
  }

  fun deleteHearingResult(adjudicationNumber: String, oicHearingId: String) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.HEARING_RESULT_DELETED,
        "Hearing result has been deleted for adjudication number $adjudicationNumber, hearing $oicHearingId",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          hearingNumber = oicHearingId,
        ),
      )
    } else {
      legacyNomisGateway.deleteHearingResult(adjudicationNumber = adjudicationNumber.toLong(), oicHearingId = oicHearingId.toLong())
    }
  }

  fun createSanction(adjudicationNumber: String, sanction: OffenderOicSanctionRequest): Long? {
    val sanctionSeq = if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.SANCTION_CREATED,
        "Sanction has been created for adjudication number $adjudicationNumber, sequence ${sanction.sanctionSeq}",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          sanctionSequence = sanction.sanctionSeq,
        ),
      )

      sanction.sanctionSeq
    } else {
      legacyNomisGateway.createSanction(adjudicationNumber.toLong(), sanction)
    }

    return sanctionSeq
  }

  fun deleteSanction(adjudicationNumber: String, sanctionSeq: Long) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.SANCTION_DELETED,
        "Sanction has been deleted for adjudication number $adjudicationNumber, sequence $sanctionSeq",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
          sanctionSequence = sanctionSeq,
        ),
      )
    } else {
      legacyNomisGateway.deleteSanction(adjudicationNumber.toLong(), sanctionSeq)
    }
  }

  fun createSanctions(adjudicationNumber: String, sanctions: List<OffenderOicSanctionRequest>) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      sanctions.forEach {
        if (it.sanctionSeq != null) deleteSanction(adjudicationNumber, it.sanctionSeq)
      }
    } else {
      legacyNomisGateway.createSanctions(adjudicationNumber.toLong(), sanctions)
    }
  }

  fun updateSanctions(adjudicationNumber: String, sanctions: List<OffenderOicSanctionRequest>) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      sanctions.forEach {
        snsService.publishDomainEvent(
          AdjudicationDomainEventType.SANCTION_UDPATED,
          "Sanction has been updated for adjudication number $adjudicationNumber, sequence ${it.sanctionSeq}",
          occurredAt = LocalDateTime.now(clock),
          AdditionalInformation(
            adjudicationNumber = adjudicationNumber,
            sanctionSequence = it.sanctionSeq,
          ),
        )
      }
    } else {
      legacyNomisGateway.updateSanctions(adjudicationNumber.toLong(), sanctions)
    }
  }

  fun quashSanctions(adjudicationNumber: String) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.SANCTIONS_QUASHED,
        "Sanctions have been quashed for adjudication number $adjudicationNumber",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
        ),
      )
    } else {
      legacyNomisGateway.quashSanctions(adjudicationNumber.toLong())
    }
  }

  fun deleteSanctions(adjudicationNumber: String) {
    if (featureFlagsService.isEmitEventsForAdjudications()) {
      snsService.publishDomainEvent(
        AdjudicationDomainEventType.SANCTIONS_DELETED,
        "Sanctions have been deleted for adjudication number $adjudicationNumber",
        occurredAt = LocalDateTime.now(clock),
        AdditionalInformation(
          adjudicationNumber = adjudicationNumber,
        ),
      )
    } else {
      legacyNomisGateway.deleteSanctions(adjudicationNumber.toLong())
    }
  }
}
