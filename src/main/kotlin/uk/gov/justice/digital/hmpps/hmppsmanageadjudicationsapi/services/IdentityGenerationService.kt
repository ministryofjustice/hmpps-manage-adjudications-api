package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class IdentityGenerationService {
  fun generateAdjudicationNumber() = UUID.randomUUID().toString()

  fun generateHearingId() = UUID.randomUUID().toString()
}
