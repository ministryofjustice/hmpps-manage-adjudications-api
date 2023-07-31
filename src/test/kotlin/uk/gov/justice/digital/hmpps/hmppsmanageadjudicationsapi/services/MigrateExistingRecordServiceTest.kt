package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services.reported.ReportedAdjudicationTestBase

class MigrateExistingRecordServiceTest : ReportedAdjudicationTestBase() {

  private val migrateExistingRecordService = MigrateExistingRecordService(reportedAdjudicationRepository)

  override fun `throws an entity not found if the reported adjudication for the supplied id does not exists`() {
    // na
  }
}
