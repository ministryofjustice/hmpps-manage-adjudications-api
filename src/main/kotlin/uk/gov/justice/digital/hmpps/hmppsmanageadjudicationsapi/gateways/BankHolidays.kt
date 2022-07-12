package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class BankHoliday(
  val title: String,
  val date: LocalDate,
  val notes: String = "",
  val bunting: Boolean = false
)

data class RegionBankHolidays(
  val division: String,
  val events: List<BankHoliday>
)

data class BankHolidays(
  @JsonProperty("england-and-wales")
  val englandAndWales: RegionBankHolidays,
  @JsonProperty("scotland")
  val scotland: RegionBankHolidays,
  @JsonProperty("northern-ireland")
  val northernIreland: RegionBankHolidays,
  val lastUpdated: Long = System.currentTimeMillis()
)
