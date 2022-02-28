package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.jupiter.api.Test

class OffenceCodeLookupServiceTest {
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()

  @Test
  fun `offence codes have values set for all items`() {
    assertValuesSetForAllItems(1001..1008)
    assertValuesSetForAllItems(2001..2004)
    assertValuesSetForAllItems(3001..3001)
    assertValuesSetForAllItems(4001..4001)
    assertValuesSetForAllItems(5001..5001)
    assertValuesSetForAllItems(6001..6001)
    assertValuesSetForAllItems(7001..7002)
    assertValuesSetForAllItems(8001..8002)
    assertValuesSetForAllItems(9001..9002)
    assertValuesSetForAllItems(10001..10002)
    assertValuesSetForAllItems(12001..12002)
    assertValuesSetForAllItems(12101..12102)
    assertValuesSetForAllItems(13001..13001)
    assertValuesSetForAllItems(14001..14001)
    assertValuesSetForAllItems(15001..15001)
    assertValuesSetForAllItems(16001..16001)
    assertValuesSetForAllItems(17001..17002)
    assertValuesSetForAllItems(18001..18002)
    assertValuesSetForAllItems(19001..19003)
    assertValuesSetForAllItems(20001..20002)
    assertValuesSetForAllItems(21001..21001)
    assertValuesSetForAllItems(22001..22001)
    assertValuesSetForAllItems(23101..23101)
    assertValuesSetForAllItems(23001..23002)
    assertValuesSetForAllItems(23101..23101)
    assertValuesSetForAllItems(23201..23202)
    assertValuesSetForAllItems(24001..24002)
    assertValuesSetForAllItems(24101..24101)
  }

  private fun assertValuesSetForAllItems(offenceCodes: IntRange) {
    offenceCodes.forEach {
      assertThat(offenceCodeLookupService.getParagraphNumber(it)).isNotBlank
      assertThat(offenceCodeLookupService.getParagraphDescription(it)).isNotBlank
      assertThat(offenceCodeLookupService.getParagraphCode(it)).isNotBlank
      assertThat(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(it)).hasSizeGreaterThan(0)
      offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(it).forEach { it ->
        assertThat(it).startsWith("51:")
      }
      assertThat(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(it)).startsWith("51:")
    }
  }
}
