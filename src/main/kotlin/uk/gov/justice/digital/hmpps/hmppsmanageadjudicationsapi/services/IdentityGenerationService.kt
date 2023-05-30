package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service

@Service
class IdentityGenerationService {
  // TODO: Turn this into a DB sequence
  fun generateAdjudicationNumber() = (99900000000..99999999999).random()
}
