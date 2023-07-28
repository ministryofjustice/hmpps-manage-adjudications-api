package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OffenceCodeLookupServiceTest {
  private val offenceCodeLookupService: OffenceCodeLookupService = OffenceCodeLookupService()

  @Test
  fun `offence codes have values set for all items`() {
    assertValuesSetForAllItems(1001..1008)
    assertValuesSetForAllItems(1021..1022)
    assertValuesSetForAllItems(2001..2004)
    assertValuesSetForAllItems(2021..2021)
    assertValuesSetForAllItems(3001..3001)
    assertValuesSetForAllItems(4001..4001)
    assertValuesSetForAllItems(5001..5001)
    assertValuesSetForAllItems(6001..6001)
    assertValuesSetForAllItems(7001..7002)
    assertValuesSetForAllItems(8001..8002)
    assertValuesSetForAllItems(9001..9002)
    assertValuesSetForAllItems(10001..10001)
    assertValuesSetForAllItems(11001..11001)
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

  @Test
  fun `get offence code`() {
    assertThat(OffenceCodes.getOffenceCode(4001)).isEqualTo(OffenceCodes.ADULT_51_4)
  }

  private fun assertValuesSetForAllItems(offenceCodes: IntRange) {
    offenceCodes.forEach {
      assertValuesSetForItem(it, false, "51:")
      assertValuesSetForItem(it, true, "55:")
    }
  }

  private fun assertValuesSetForItem(code: Int, isYouthOffender: Boolean, nomisCodePrefix: String) {
    assertThat(offenceCodeLookupService.getParagraphNumber(code, isYouthOffender)).isNotBlank
    assertThat(offenceCodeLookupService.getParagraphDescription(code, isYouthOffender)).isNotBlank
    assertThat(offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(code, isYouthOffender)).hasSizeGreaterThan(0)
    offenceCodeLookupService.getCommittedOnOwnNomisOffenceCodes(code, isYouthOffender).startsWith(nomisCodePrefix)
    assertThat(offenceCodeLookupService.getNotCommittedOnOwnNomisOffenceCode(code, isYouthOffender)).startsWith(nomisCodePrefix)
  }
}
