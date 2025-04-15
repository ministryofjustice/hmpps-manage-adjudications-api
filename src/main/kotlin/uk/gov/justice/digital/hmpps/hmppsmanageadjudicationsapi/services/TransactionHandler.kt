package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionHandler {

  /**
   * Wraps the calling block in a new Spring transaction using @Transactional(propagation = Propagation.REQUIRES_NEW)
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun <T> newSpringTransaction(block: () -> T): T = block()
}