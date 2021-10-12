package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsManageAdjudicationsApi

fun main(args: Array<String>) {
  runApplication<HmppsManageAdjudicationsApi>(*args)
}
