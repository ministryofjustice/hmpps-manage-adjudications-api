package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.migrate.MigrateNewRecordService
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateNewRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateNewRecordService = MigrateNewRecordService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
