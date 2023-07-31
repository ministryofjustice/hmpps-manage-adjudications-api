package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.repositories.ReportedAdjudicationRepository

@Transactional
@Service
class MigrateService(
  private val reportedAdjudicationRepository: ReportedAdjudicationRepository,
) {

  fun reset() {
    reportedAdjudicationRepository.deleteByMigratedIsTrue()
  }
}
