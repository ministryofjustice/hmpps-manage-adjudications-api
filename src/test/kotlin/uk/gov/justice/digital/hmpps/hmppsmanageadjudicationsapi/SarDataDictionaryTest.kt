package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class SarDataDictionaryTest {

  @Test
  fun `SAR data dictionary contains the supplied data elements and valid metadata`() {
    val rows = readDictionary()

    assertThat(rows).hasSizeGreaterThanOrEqualTo(SUPPLIED_DOCUMENT_ELEMENTS.size)
    assertThat(rows.map { it.key }).doesNotHaveDuplicates()
    assertThat(rows.map { it.key }).containsAll(SUPPLIED_DOCUMENT_ELEMENTS)

    rows.forEach { row ->
      REQUIRED_COLUMNS.forEach { column ->
        assertThat(row.values.getValue(column))
          .describedAs("%s must have a value for %s", row.key, column)
          .isNotBlank()
      }
      assertThat(row.values.getValue("Mandatory"))
        .describedAs("%s Mandatory", row.key)
        .isIn("Y", "N")
      assertThat(row.values.getValue("Included in Current Template"))
        .describedAs("%s Included in Current Template", row.key)
        .isIn("Y", "N")
      assertThat(row.values.getValue("Source type"))
        .describedAs("%s Source type", row.key)
        .isIn("Database", "Derived", "External")

      if (row.values.getValue("Included in Current Template") == "Y") {
        assertThat(row.values.getValue("Template label"))
          .describedAs("%s Template label", row.key)
          .isNotBlank()
      }
    }

    assertTemplateInclusion(rows, expected = "N", EXPLICITLY_EXCLUDED_ELEMENTS)
    assertTemplateInclusion(rows, expected = "Y", CONFIRMED_TEMPLATE_ELEMENTS)
  }

  @Test
  fun `SAR data dictionary covers every field label in the current template`() {
    val template = checkNotNull(javaClass.getResource("/template_hmpps-manage-adjudications-api.mustache")).readText()
    val summaryLabels = Regex("""<tr><td>([^<]+)</td><td>""").findAll(template).map { it.groupValues[1] }
    val tableLabels = Regex("""<td class="data-column-\d+">([^<]+)</td>""").findAll(template).map { it.groupValues[1] }
    val templateLabels = (summaryLabels + tableLabels).toSet()
    val documentedLabels = readDictionary()
      .flatMap { it.values.getValue("Template label").split(";") }
      .map(String::trim)
      .filter(String::isNotEmpty)
      .toSet()

    assertThat(documentedLabels).containsAll(templateLabels)
  }

  private fun assertTemplateInclusion(rows: List<DictionaryRow>, expected: String, elements: Set<String>) {
    elements.forEach { key ->
      val row = rows.single { it.key == key }
      assertThat(row.values.getValue("Included in Current Template"))
        .describedAs("%s Included in Current Template", key)
        .isEqualTo(expected)
    }
  }

  private fun readDictionary(): List<DictionaryRow> {
    val lines = Files.readAllLines(Path.of("doc/data-dictionary/sar-data-dictionary.csv"))
    val headers = parseCsvLine(lines.first())
    assertThat(headers).containsExactlyElementsOf(EXPECTED_HEADERS)

    return lines.drop(1).mapIndexed { index, line ->
      val values = parseCsvLine(line)
      assertThat(values)
        .describedAs("CSV row %s", index + 2)
        .hasSameSizeAs(headers)
      val mappedValues = headers.zip(values).toMap()
      DictionaryRow(
        key = "${mappedValues.getValue("Entity")}|${mappedValues.getValue("Element")}",
        values = mappedValues,
      )
    }
  }

  private fun parseCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val value = StringBuilder()
    var insideQuotes = false
    var index = 0

    while (index < line.length) {
      when {
        line[index] == '"' && insideQuotes && index + 1 < line.length && line[index + 1] == '"' -> {
          value.append('"')
          index++
        }
        line[index] == '"' -> insideQuotes = !insideQuotes
        line[index] == ',' && !insideQuotes -> {
          values += value.toString()
          value.clear()
        }
        else -> value.append(line[index])
      }
      index++
    }

    check(!insideQuotes) { "Unclosed quoted value in CSV row: $line" }
    values += value.toString()
    return values
  }

  private data class DictionaryRow(
    val key: String,
    val values: Map<String, String>,
  )

  private companion object {
    val EXPECTED_HEADERS = listOf(
      "Entity",
      "Element",
      "Description",
      "Data quality",
      "Mandatory",
      "Example value",
      "Included in Current Template",
      "Required changes",
      "Source type",
      "Source reference",
      "SAR API path",
      "Template label",
    )

    val REQUIRED_COLUMNS = EXPECTED_HEADERS - "Template label"

    val EXPLICITLY_EXCLUDED_ELEMENTS = setOf(
      "Offence|Offence Code",
      "Offence|Victim Prisoner's Number",
      "Offence|Victim Staff Username",
      "Offence|Victim Other Person's Name",
      "Witness|Code",
      "Adjudication|Offender Booking ID",
    )

    val CONFIRMED_TEMPLATE_ELEMENTS = setOf(
      "Damages|Reporter",
      "Damages|Repair Cost",
      "Witness|Comment",
      "Adjudication|Youth Offender",
      "Adjudication|Gender",
      "Hearing|Representative",
      "Outcome|Adjourn Reason",
      "Outcome|Details",
      "Outcome|Outcome Details",
      "Outcome|Not Proceed Reason",
      "Outcome|Quashed Reason",
      "Outcome|OIC Hearing ID",
      "Outcome|Refer Gov Reason",
      "Punishment|Other Privilege",
      "Punishment|Rehab Monitor",
    )

    val SUPPLIED_DOCUMENT_ELEMENTS = setOf(
      "Offence|Offence ID",
      "Offence|Offence Code",
      "Offence|Victim Prisoner's Number",
      "Offence|Victim Staff Username",
      "Offence|Victim Other Person's Name",
      "Offence|Protected Characteristic",
      "Damages|Code",
      "Damages|Details",
      "Damages|Reporter",
      "Damages|Repair Cost",
      "Witness|Code",
      "Witness|First Name",
      "Witness|Last Name",
      "Witness|Reporter",
      "Witness|Comment",
      "Evidence|Code",
      "Evidence|Identifier",
      "Evidence|Details",
      "Evidence|Reporter",
      "Adjudication|Prisoner Number",
      "Adjudication|Charge Number",
      "Adjudication|Originating Agency",
      "Adjudication|Incident Statement",
      "Adjudication|Incident Date/Time",
      "Adjudication|Incident Location",
      "Adjudication|Handover Deadline",
      "Adjudication|Status",
      "Adjudication|Status Reason",
      "Adjudication|Status Details",
      "Adjudication|Youth Offender",
      "Adjudication|Date/Time of Discovery",
      "Adjudication|Issuing Officer",
      "Adjudication|Gender",
      "Adjudication|Date/Time of Issue",
      "Adjudication|Date/Time of First Hearing",
      "Adjudication|Override Agency",
      "Adjudication|Agency Incident ID",
      "Adjudication|Offender Booking ID",
      "Hearing|Reported Adjudication ID",
      "Hearing|Agency ID",
      "Hearing|Hearing Location",
      "Hearing|Date/Time of Hearing",
      "Hearing|Charge Number",
      "Hearing|OIC Hearing ID",
      "Hearing|OIC Hearing Type",
      "Hearing|Representative",
      "Outcome|Outcome ID",
      "Outcome|Adjudicator",
      "Outcome|Adjourn Reason",
      "Outcome|Plea",
      "Outcome|Details",
      "Outcome|Outcome Code",
      "Outcome|Outcome Details",
      "Outcome|Not Proceed Reason",
      "Outcome|Quashed Reason",
      "Outcome|OIC Hearing ID",
      "Outcome|Refer Gov Reason",
      "Punishment|Punishment ID",
      "Punishment|Punishment Type",
      "Punishment|Punishment Duration",
      "Punishment|Punishment Start Date",
      "Punishment|Punishment End Date",
      "Punishment|Punishment Comments",
      "Punishment|Measurement",
      "Punishment|Privilege Type",
      "Punishment|Other Privilege",
      "Punishment|Stoppage Percentage",
      "Punishment|Activated By Charge Number",
      "Punishment|Suspended Until",
      "Punishment|Sanction Seq",
      "Punishment|Amount",
      "Punishment|Payback Notes",
      "Punishment|Consecutive to Charge Number",
      "Punishment|Rehab Completed",
      "Punishment|Rehab Not Completed Outcome",
      "Punishment|Rehab Details",
      "Punishment|Rehab Monitor",
    )
  }
}
