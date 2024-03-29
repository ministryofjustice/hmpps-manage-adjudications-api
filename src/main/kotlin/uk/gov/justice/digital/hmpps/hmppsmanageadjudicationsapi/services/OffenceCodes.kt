package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.validation.ValidationException

enum class OffenceCodes(val paragraph: String, private val withOthers: String? = null, val uniqueOffenceCodes: List<Int>, val paragraphDescription: Descriptions) {
  ADULT_51_1A(paragraph = "1(a)", uniqueOffenceCodes = listOf(1001, 1003, 1005, 1021, 1007), paragraphDescription = Descriptions.YOI_2_ADULT_1A),
  ADULT_51_1B(paragraph = "1", withOthers = "51:25D", uniqueOffenceCodes = listOf(1002, 1022), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1J(paragraph = "1", withOthers = "51:25F", uniqueOffenceCodes = listOf(1004), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1N(paragraph = "1", withOthers = "51:25G", uniqueOffenceCodes = listOf(1006), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1F(paragraph = "1", withOthers = "51:25E", uniqueOffenceCodes = listOf(1008), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_4(paragraph = "4", uniqueOffenceCodes = listOf(4001, 4002, 4003, 4004, 4005), paragraphDescription = Descriptions.YOI_5_ADULT_4),
  ADULT_51_5(paragraph = "5", withOthers = "51:25Q", uniqueOffenceCodes = listOf(5001, 5002, 5003, 5004, 5005), paragraphDescription = Descriptions.YOI_6_ADULT_5),
  ADULT_51_7(paragraph = "7", withOthers = "51:25O", uniqueOffenceCodes = listOf(7001, 7002), paragraphDescription = Descriptions.ADULT_7),
  ADULT_51_8D(paragraph = "8", withOthers = "51:25AI", uniqueOffenceCodes = listOf(8001), paragraphDescription = Descriptions.ADULT_8),
  ADULT_51_8E(paragraph = "8", withOthers = "51:25AJ", uniqueOffenceCodes = listOf(8002), paragraphDescription = Descriptions.ADULT_8),
  ADULT_51_12(paragraph = "12", withOthers = "51:25AM", uniqueOffenceCodes = listOf(12001), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_12A(paragraph = "12", withOthers = "51:25A", uniqueOffenceCodes = listOf(12002), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_14(paragraph = "14", withOthers = "51:25J", uniqueOffenceCodes = listOf(14001), paragraphDescription = Descriptions.YOI_15_ADULT_14),
  ADULT_51_13B(paragraph = "13", withOthers = "51:25J", uniqueOffenceCodes = listOf(13001), paragraphDescription = Descriptions.YOI_14_ADULT_13),
  ADULT_51_15(paragraph = "15", withOthers = "51:25T", uniqueOffenceCodes = listOf(15001), paragraphDescription = Descriptions.ADULT_15),
  ADULT_51_24(paragraph = "24", withOthers = "51:25Y", uniqueOffenceCodes = listOf(24001, 24002), paragraphDescription = Descriptions.ADULT_24),
  ADULT_51_23AP(paragraph = "23", withOthers = "51:25V", uniqueOffenceCodes = listOf(23001, 23201), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  ADULT_51_25Z(paragraph = "22", uniqueOffenceCodes = listOf(23002, 23202), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  ADULT_51_9(paragraph = "9", withOthers = "51:25U", uniqueOffenceCodes = listOf(9001, 9002), paragraphDescription = Descriptions.ADULT_9),
  ADULT_51_12AQ(paragraph = "12", withOthers = "51:25X", uniqueOffenceCodes = listOf(12101, 12102), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_10(paragraph = "10", uniqueOffenceCodes = listOf(10001), paragraphDescription = Descriptions.ADULT_10),
  ADULT_51_11(paragraph = "11", uniqueOffenceCodes = listOf(11001), paragraphDescription = Descriptions.ADULT_11),
  ADULT_51_16(paragraph = "16", withOthers = "51:25C", uniqueOffenceCodes = listOf(16001), paragraphDescription = Descriptions.ADULT_16),
  ADULT_51_24A(paragraph = "24(a)", uniqueOffenceCodes = listOf(24101), paragraphDescription = Descriptions.ADULT_24A),
  ADULT_51_17A(paragraph = "17(a)", withOthers = "51:25L", uniqueOffenceCodes = listOf(17001), paragraphDescription = Descriptions.ADULT_17A),
  ADULT_51_17(paragraph = "17", uniqueOffenceCodes = listOf(17002), paragraphDescription = Descriptions.ADULT_17),
  ADULT_51_19B(paragraph = "19", withOthers = "51:25AE", uniqueOffenceCodes = listOf(19001), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_19C(paragraph = "19", withOthers = "51:25AF", uniqueOffenceCodes = listOf(19002), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_19A(paragraph = "19", withOthers = "51:25AD", uniqueOffenceCodes = listOf(19003), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_20A(paragraph = "20(a)", uniqueOffenceCodes = listOf(20001), paragraphDescription = Descriptions.YOI_23_ADULT_20A),
  ADULT_51_20(paragraph = "20", withOthers = "51:25AC", uniqueOffenceCodes = listOf(20002), paragraphDescription = Descriptions.YOI_22_ADULT_20),
  ADULT_51_22(paragraph = "22", withOthers = "51:25W", uniqueOffenceCodes = listOf(22001), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  ADULT_51_23(paragraph = "23", withOthers = "51:25AN", uniqueOffenceCodes = listOf(23101), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  ADULT_51_2A(paragraph = "2", withOthers = "51:25K", uniqueOffenceCodes = listOf(2001, 2021), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2C(paragraph = "2", withOthers = "51:25M", uniqueOffenceCodes = listOf(2002), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2D(paragraph = "2", withOthers = "51:25N", uniqueOffenceCodes = listOf(2003), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2B(paragraph = "2", withOthers = "51:25L", uniqueOffenceCodes = listOf(2004), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_3(paragraph = "3", withOthers = "51:25H", uniqueOffenceCodes = listOf(3001), paragraphDescription = Descriptions.ADULT_3),
  ADULT_51_6(paragraph = "6", withOthers = "51:25R", uniqueOffenceCodes = listOf(6001), paragraphDescription = Descriptions.ADULT_6),
  ADULT_51_18A(paragraph = "18", withOthers = "51:25B", uniqueOffenceCodes = listOf(18001), paragraphDescription = Descriptions.ADULT_18),
  ADULT_51_18B(paragraph = "18", withOthers = "51:25AO", uniqueOffenceCodes = listOf(18002), paragraphDescription = Descriptions.ADULT_18),
  ADULT_51_21(paragraph = "21", withOthers = "51:25AL", uniqueOffenceCodes = listOf(21001), paragraphDescription = Descriptions.YOI_24_ADULT_21),
  YOI_55_2(paragraph = "2", uniqueOffenceCodes = listOf(1001, 1003, 1005, 1021, 1007), paragraphDescription = Descriptions.YOI_2_ADULT_1A),
  YOI_55_1A(paragraph = "1", withOthers = "55:29D", uniqueOffenceCodes = listOf(1002, 1022), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_1L(paragraph = "1", withOthers = "55:29F", uniqueOffenceCodes = listOf(1004), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_1M(paragraph = "1", withOthers = "55:29G", uniqueOffenceCodes = listOf(1006), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_1E(paragraph = "1", withOthers = "55:29E", uniqueOffenceCodes = listOf(1008), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_5(paragraph = "5", uniqueOffenceCodes = listOf(4001, 4002, 4003, 4004, 4005), paragraphDescription = Descriptions.YOI_5_ADULT_4),
  YOI_55_6(paragraph = "6", withOthers = "55:29Q", uniqueOffenceCodes = listOf(5001, 5002, 5003, 5004, 5005), paragraphDescription = Descriptions.YOI_6_ADULT_5),
  YOI_55_8(paragraph = "8", withOthers = "55:29O", uniqueOffenceCodes = listOf(7001, 7002), paragraphDescription = Descriptions.YOI_8),
  YOI_55_9D(paragraph = "9", withOthers = "55:29AI", uniqueOffenceCodes = listOf(8001), paragraphDescription = Descriptions.YOI_9),
  YOI_55_9E(paragraph = "9", withOthers = "55:29AJ", uniqueOffenceCodes = listOf(8002), paragraphDescription = Descriptions.YOI_9),
  YOI_55_13(paragraph = "13", withOthers = "55:29AM", uniqueOffenceCodes = listOf(12001), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_13A(paragraph = "13", withOthers = "55:29AH", uniqueOffenceCodes = listOf(12002), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_15(paragraph = "15", withOthers = "55:29J", uniqueOffenceCodes = listOf(14001), paragraphDescription = Descriptions.YOI_15_ADULT_14),
  YOI_55_14B(paragraph = "14", withOthers = "55:29J", uniqueOffenceCodes = listOf(13001), paragraphDescription = Descriptions.YOI_14_ADULT_13),
  YOI_55_16(paragraph = "16", withOthers = "55:29T", uniqueOffenceCodes = listOf(15001), paragraphDescription = Descriptions.YOI_16),
  YOI_55_27(paragraph = "27", withOthers = "55:29Y", uniqueOffenceCodes = listOf(24001, 24002), paragraphDescription = Descriptions.YOI_27),
  YOI_55_26B(paragraph = "26", withOthers = "55:29V", uniqueOffenceCodes = listOf(23001, 23201), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  YOI_55_26C(paragraph = "25", withOthers = "55:29Z", uniqueOffenceCodes = listOf(23002, 23202), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  YOI_55_10(paragraph = "10", withOthers = "55:29U", uniqueOffenceCodes = listOf(9001, 9002), paragraphDescription = Descriptions.YOI_10),
  YOI_55_13C(paragraph = "13", withOthers = "55:29X", uniqueOffenceCodes = listOf(12101), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_13B(paragraph = "13", withOthers = "55:29X", uniqueOffenceCodes = listOf(12102), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_11(paragraph = "11", uniqueOffenceCodes = listOf(10001), paragraphDescription = Descriptions.YOI_11),
  YOI_55_12(paragraph = "12", uniqueOffenceCodes = listOf(11001), paragraphDescription = Descriptions.YOI_12),
  YOI_55_17(paragraph = "17", withOthers = "55:29C", uniqueOffenceCodes = listOf(16001), paragraphDescription = Descriptions.YOI_17),
  YOI_55_28(paragraph = "28", uniqueOffenceCodes = listOf(24101), paragraphDescription = Descriptions.YOI_28),
  YOI_55_19(paragraph = "19", withOthers = "55:29I", uniqueOffenceCodes = listOf(17001), paragraphDescription = Descriptions.YOI_19),
  YOI_55_18(paragraph = "18", uniqueOffenceCodes = listOf(17002), paragraphDescription = Descriptions.YOI_18),
  YOI_55_21B(paragraph = "21", withOthers = "55:29AE", uniqueOffenceCodes = listOf(19001), paragraphDescription = Descriptions.YOI_21),
  YOI_55_21C(paragraph = "21", withOthers = "55:29AF", uniqueOffenceCodes = listOf(19002), paragraphDescription = Descriptions.YOI_21),
  YOI_55_21A(paragraph = "21", withOthers = "55:29AD", uniqueOffenceCodes = listOf(19003), paragraphDescription = Descriptions.YOI_21),
  YOI_55_23(paragraph = "23", uniqueOffenceCodes = listOf(20001), paragraphDescription = Descriptions.YOI_23_ADULT_20A),
  YOI_55_22(paragraph = "22", withOthers = "55:29AC", uniqueOffenceCodes = listOf(20002), paragraphDescription = Descriptions.YOI_22_ADULT_20),
  YOI_55_25(paragraph = "25", withOthers = "55:29W", uniqueOffenceCodes = listOf(22001), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  YOI_55_26(paragraph = "26", withOthers = "55:29AI", uniqueOffenceCodes = listOf(23101), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  YOI_55_3A(paragraph = "3", withOthers = "55:29K", uniqueOffenceCodes = listOf(2001, 2021), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3C(paragraph = "3", withOthers = "55:29M", uniqueOffenceCodes = listOf(2002), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3D(paragraph = "3", withOthers = "55:29N", uniqueOffenceCodes = listOf(2003), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3B(paragraph = "3", withOthers = "55:29L", uniqueOffenceCodes = listOf(2004), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_4(paragraph = "4", withOthers = "55:29H", uniqueOffenceCodes = listOf(3001), paragraphDescription = Descriptions.YOI_4),
  YOI_55_7(paragraph = "7", withOthers = "55:29R", uniqueOffenceCodes = listOf(6001), paragraphDescription = Descriptions.YOI_7),
  YOI_55_20A(paragraph = "20", withOthers = "55:29AB", uniqueOffenceCodes = listOf(18001), paragraphDescription = Descriptions.YOI_20),
  YOI_55_20B(paragraph = "20", withOthers = "55:29AO", uniqueOffenceCodes = listOf(18002), paragraphDescription = Descriptions.YOI_20),
  YOI_55_24(paragraph = "24", withOthers = "55:29AL", uniqueOffenceCodes = listOf(21001), paragraphDescription = Descriptions.YOI_24_ADULT_21),
  MIGRATED_OFFENCE(paragraph = "", uniqueOffenceCodes = listOf(0), paragraphDescription = Descriptions.DEFAULT),
  ;

  fun getNomisCode() = convertToCode(this.name)

  fun getNomisCodeWithOthers() = this.withOthers ?: getNomisCode()

  companion object {
    private val matchAdultOrYoi = "[^_]*_".toRegex()
    fun convertToCode(name: String) = name.replaceFirst(matchAdultOrYoi, "").replace("_", ":")
    fun getAdultOffenceCodes() = OffenceCodes.values().filter { it.getNomisCode().startsWith("51:") }
    fun getYouthOffenceCodes() = OffenceCodes.values().filter { it.getNomisCode().startsWith("55:") }

    fun validateOffenceCode(offenceCode: Int) =
      OffenceCodes.values().flatMap { it.uniqueOffenceCodes }.firstOrNull { it == offenceCode } ?: throw ValidationException("Invalid offence code $offenceCode")

    fun Int.containsNomisCode(nomisCode: String): Boolean {
      val offenceCode = OffenceCodes.values().first { it.uniqueOffenceCodes.contains(this) }

      return offenceCode.getNomisCode() == nomisCode || offenceCode.getNomisCodeWithOthers().contains(nomisCode)
    }
  }
}
