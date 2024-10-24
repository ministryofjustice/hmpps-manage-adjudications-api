package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

import jakarta.validation.ValidationException

enum class OffenceCodes(val applicableVersions: List<Int> = listOf(1, 2), val nomisCode: String, val paragraph: String, private val withOthers: String? = null, val uniqueOffenceCodes: List<Int>, val paragraphDescription: Descriptions) {
  ADULT_51_1A(applicableVersions = listOf(1), nomisCode = "51:1A", paragraph = "1(a)", uniqueOffenceCodes = listOf(1001, 1003, 1005, 1021, 1007), paragraphDescription = Descriptions.YOI_2_ADULT_1A),
  ADULT_51_1A_24(applicableVersions = listOf(2), nomisCode = "51:1A (24)", paragraph = "1(a)", uniqueOffenceCodes = listOf(100124, 100324, 100524, 102124, 100724), paragraphDescription = Descriptions.YOI_2A_24_ADULT_1A_24),
  ADULT_51_1B(nomisCode = "51:1B", paragraph = "1", withOthers = "51:25D", uniqueOffenceCodes = listOf(1002, 1022), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1B_24(applicableVersions = listOf(2), nomisCode = "51:1B (24)", paragraph = "1(b)", uniqueOffenceCodes = listOf(102224), paragraphDescription = Descriptions.YOI_2A_24_ADULT_1B_24),
  ADULT_51_1C_24(applicableVersions = listOf(2), nomisCode = "51:1C (24)", paragraph = "1(c)", uniqueOffenceCodes = listOf(102324), paragraphDescription = Descriptions.YOI_2B_24_ADULT_1C_24),
  ADULT_51_1D_24(applicableVersions = listOf(2), nomisCode = "51:1D (24)", paragraph = "1(d)", uniqueOffenceCodes = listOf(102424), paragraphDescription = Descriptions.YOI_2C_24_ADULT_1D_24),
  ADULT_51_1J(nomisCode = "51:1J", paragraph = "1", withOthers = "51:25F", uniqueOffenceCodes = listOf(1004), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1N(nomisCode = "51:1N", paragraph = "1", withOthers = "51:25G", uniqueOffenceCodes = listOf(1006), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_1F(nomisCode = "51:1F", paragraph = "1", withOthers = "51:25E", uniqueOffenceCodes = listOf(1008), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  ADULT_51_4(nomisCode = "51:4", paragraph = "4", uniqueOffenceCodes = listOf(4001, 4002, 4003, 4004, 4005), paragraphDescription = Descriptions.YOI_5_ADULT_4),
  ADULT_51_5(nomisCode = "51:5", paragraph = "5", withOthers = "51:25Q", uniqueOffenceCodes = listOf(5001, 5002, 5003, 5004, 5005), paragraphDescription = Descriptions.YOI_6_ADULT_5),
  ADULT_51_7(nomisCode = "51:7", paragraph = "7", withOthers = "51:25O", uniqueOffenceCodes = listOf(7001, 7002), paragraphDescription = Descriptions.ADULT_7),
  ADULT_51_8D(nomisCode = "51:8D", paragraph = "8", withOthers = "51:25AI", uniqueOffenceCodes = listOf(8001), paragraphDescription = Descriptions.ADULT_8),
  ADULT_51_8E(nomisCode = "51:8E", paragraph = "8", withOthers = "51:25AJ", uniqueOffenceCodes = listOf(8002), paragraphDescription = Descriptions.ADULT_8),
  ADULT_51_12(nomisCode = "51:12", paragraph = "12", withOthers = "51:25AM", uniqueOffenceCodes = listOf(12001), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_12A(nomisCode = "51:12A", paragraph = "12", withOthers = "51:25A", uniqueOffenceCodes = listOf(12002), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_14(nomisCode = "51:14", paragraph = "14", withOthers = "51:25J", uniqueOffenceCodes = listOf(14001), paragraphDescription = Descriptions.YOI_15_ADULT_14),
  ADULT_51_13B(nomisCode = "51:13B", paragraph = "13", withOthers = "51:25J", uniqueOffenceCodes = listOf(13001), paragraphDescription = Descriptions.YOI_14_ADULT_13),
  ADULT_51_15(nomisCode = "51:15", paragraph = "15", withOthers = "51:25T", uniqueOffenceCodes = listOf(15001), paragraphDescription = Descriptions.ADULT_15),
  ADULT_51_24(nomisCode = "51:24", paragraph = "24", withOthers = "51:25Y", uniqueOffenceCodes = listOf(24001, 24002), paragraphDescription = Descriptions.ADULT_24),
  ADULT_51_23AP(nomisCode = "51:23AP", paragraph = "23", withOthers = "51:25V", uniqueOffenceCodes = listOf(23001, 23201), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  ADULT_51_25Z(nomisCode = "51:25Z", paragraph = "22", uniqueOffenceCodes = listOf(23002, 23202), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  ADULT_51_9(nomisCode = "51:9", paragraph = "9", withOthers = "51:25U", uniqueOffenceCodes = listOf(9001, 9002), paragraphDescription = Descriptions.ADULT_9),
  ADULT_51_12AQ(nomisCode = "51:12AQ", paragraph = "12", withOthers = "51:25X", uniqueOffenceCodes = listOf(12101, 12102), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  ADULT_51_10(nomisCode = "51:10", paragraph = "10", uniqueOffenceCodes = listOf(10001), paragraphDescription = Descriptions.ADULT_10),
  ADULT_51_11(nomisCode = "51:11", paragraph = "11", uniqueOffenceCodes = listOf(11001), paragraphDescription = Descriptions.ADULT_11),
  ADULT_51_16(nomisCode = "51:16", paragraph = "16", withOthers = "51:25C", uniqueOffenceCodes = listOf(16001), paragraphDescription = Descriptions.ADULT_16),

  @Deprecated("this was incorrect and only present for historical reasons")
  ADULT_51_24A(applicableVersions = listOf(1), nomisCode = "51:24A", paragraph = "24(a)", withOthers = "51:25L", uniqueOffenceCodes = listOf(24101), paragraphDescription = Descriptions.ADULT_24A),
  ADULT_51_24A_24(applicableVersions = listOf(2), nomisCode = "51:24A (24)", paragraph = "24(a)", uniqueOffenceCodes = listOf(2410124), paragraphDescription = Descriptions.ADULT_24A_24),
  ADULT_51_17A(applicableVersions = listOf(1), nomisCode = "51:17A", paragraph = "17(a)", uniqueOffenceCodes = listOf(17001), paragraphDescription = Descriptions.ADULT_17A),
  ADULT_51_17A_24(applicableVersions = listOf(2), nomisCode = "51:17A (24)", paragraph = "17(a)", uniqueOffenceCodes = listOf(1700124), paragraphDescription = Descriptions.ADULT_17A_24),
  ADULT_51_17(nomisCode = "51:17", paragraph = "17", uniqueOffenceCodes = listOf(17002), paragraphDescription = Descriptions.ADULT_17),
  ADULT_51_19B(nomisCode = "51:19B", paragraph = "19", withOthers = "51:25AE", uniqueOffenceCodes = listOf(19001), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_19C(nomisCode = "51:19C", paragraph = "19", withOthers = "51:25AF", uniqueOffenceCodes = listOf(19002), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_19A(nomisCode = "51:19A", paragraph = "19", withOthers = "51:25AD", uniqueOffenceCodes = listOf(19003), paragraphDescription = Descriptions.ADULT_19),
  ADULT_51_20A(applicableVersions = listOf(1), nomisCode = "51:20A", paragraph = "20(a)", uniqueOffenceCodes = listOf(20001), paragraphDescription = Descriptions.YOI_23_ADULT_20A),
  ADULT_51_20A_24(applicableVersions = listOf(2), nomisCode = "51:20A (24)", paragraph = "20(a)", uniqueOffenceCodes = listOf(2000124), paragraphDescription = Descriptions.YOI_23_24_ADULT_20A_24),
  ADULT_51_20(nomisCode = "51:20", paragraph = "20", withOthers = "51:25AC", uniqueOffenceCodes = listOf(20002), paragraphDescription = Descriptions.YOI_22_ADULT_20),
  ADULT_51_22(nomisCode = "51:22", paragraph = "22", withOthers = "51:25W", uniqueOffenceCodes = listOf(22001), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  ADULT_51_23(nomisCode = "51:23", paragraph = "23", withOthers = "51:25AN", uniqueOffenceCodes = listOf(23101), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  ADULT_51_2A(nomisCode = "51:2A", paragraph = "2", withOthers = "51:25K", uniqueOffenceCodes = listOf(2001, 2021), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2C(nomisCode = "51:2C", paragraph = "2", withOthers = "51:25M", uniqueOffenceCodes = listOf(2002), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2D(nomisCode = "51:2D", paragraph = "2", withOthers = "51:25N", uniqueOffenceCodes = listOf(2003), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_2B(nomisCode = "51:2B", paragraph = "2", withOthers = "51:25L", uniqueOffenceCodes = listOf(2004), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  ADULT_51_3(nomisCode = "51:3", paragraph = "3", withOthers = "51:25H", uniqueOffenceCodes = listOf(3001), paragraphDescription = Descriptions.ADULT_3),
  ADULT_51_6(nomisCode = "51:6", paragraph = "6", withOthers = "51:25R", uniqueOffenceCodes = listOf(6001), paragraphDescription = Descriptions.ADULT_6),
  ADULT_51_18A(nomisCode = "51:18A", paragraph = "18", withOthers = "51:25B", uniqueOffenceCodes = listOf(18001), paragraphDescription = Descriptions.ADULT_18),
  ADULT_51_18B(nomisCode = "51:18B", paragraph = "18", withOthers = "51:25AO", uniqueOffenceCodes = listOf(18002), paragraphDescription = Descriptions.ADULT_18),
  ADULT_51_21(nomisCode = "51:21", paragraph = "21", withOthers = "51:25AL", uniqueOffenceCodes = listOf(21001), paragraphDescription = Descriptions.YOI_24_ADULT_21),
  ADULT_51_23A_24(applicableVersions = listOf(2), nomisCode = "51:23A (24)", paragraph = "23(a)", uniqueOffenceCodes = listOf(2600124), paragraphDescription = Descriptions.YOI_26A_24_ADULT_23A_24),
  YOI_55_2(applicableVersions = listOf(1), nomisCode = "55:2", paragraph = "2", uniqueOffenceCodes = listOf(1001, 1003, 1005, 1021, 1007), paragraphDescription = Descriptions.YOI_2_ADULT_1A),
  YOI_55_2_24(applicableVersions = listOf(2), nomisCode = "55:2 (24)", paragraph = "2", uniqueOffenceCodes = listOf(100124, 100324, 100524, 102124, 100724), paragraphDescription = Descriptions.YOI_2A_24_ADULT_1A_24),
  YOI_55_1A(nomisCode = "55:1A", paragraph = "1", withOthers = "55:29D", uniqueOffenceCodes = listOf(1002, 1022), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_2A_24(applicableVersions = listOf(2), nomisCode = "55:2A (24)", paragraph = "2(a)", uniqueOffenceCodes = listOf(102224), paragraphDescription = Descriptions.YOI_2A_24_ADULT_1B_24),
  YOI_55_2B_24(applicableVersions = listOf(2), nomisCode = "55:2B (24)", paragraph = "2(b)", uniqueOffenceCodes = listOf(102324), paragraphDescription = Descriptions.YOI_2B_24_ADULT_1C_24),
  YOI_55_2C_24(applicableVersions = listOf(2), nomisCode = "55:2C (24)", paragraph = "2(c)", uniqueOffenceCodes = listOf(102424), paragraphDescription = Descriptions.YOI_2C_24_ADULT_1D_24),
  YOI_55_1L(nomisCode = "55:1L", paragraph = "1", withOthers = "55:29F", uniqueOffenceCodes = listOf(1004), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_1M(nomisCode = "55:1M", paragraph = "1", withOthers = "55:29G", uniqueOffenceCodes = listOf(1006), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_1E(nomisCode = "55:1E", paragraph = "1", withOthers = "55:29E", uniqueOffenceCodes = listOf(1008), paragraphDescription = Descriptions.YOI_1_ADULT_1),
  YOI_55_5(nomisCode = "55:5", paragraph = "5", uniqueOffenceCodes = listOf(4001, 4002, 4003, 4004, 4005), paragraphDescription = Descriptions.YOI_5_ADULT_4),
  YOI_55_6(nomisCode = "55:6", paragraph = "6", withOthers = "55:29Q", uniqueOffenceCodes = listOf(5001, 5002, 5003, 5004, 5005), paragraphDescription = Descriptions.YOI_6_ADULT_5),
  YOI_55_8(nomisCode = "55:8", paragraph = "8", withOthers = "55:29O", uniqueOffenceCodes = listOf(7001, 7002), paragraphDescription = Descriptions.YOI_8),
  YOI_55_9D(nomisCode = "55:9D", paragraph = "9", withOthers = "55:29AI", uniqueOffenceCodes = listOf(8001), paragraphDescription = Descriptions.YOI_9),
  YOI_55_9E(nomisCode = "55:9E", paragraph = "9", withOthers = "55:29AJ", uniqueOffenceCodes = listOf(8002), paragraphDescription = Descriptions.YOI_9),
  YOI_55_13(nomisCode = "55:13", paragraph = "13", withOthers = "55:29AM", uniqueOffenceCodes = listOf(12001), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_13A(nomisCode = "55:13A", paragraph = "13", withOthers = "55:29AH", uniqueOffenceCodes = listOf(12002), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_15(nomisCode = "55:15", paragraph = "15", withOthers = "55:29J", uniqueOffenceCodes = listOf(14001), paragraphDescription = Descriptions.YOI_15_ADULT_14),
  YOI_55_14B(nomisCode = "55:14B", paragraph = "14", withOthers = "55:29J", uniqueOffenceCodes = listOf(13001), paragraphDescription = Descriptions.YOI_14_ADULT_13),
  YOI_55_16(nomisCode = "55:16", paragraph = "16", withOthers = "55:29T", uniqueOffenceCodes = listOf(15001), paragraphDescription = Descriptions.YOI_16),
  YOI_55_27(nomisCode = "55:27", paragraph = "27", withOthers = "55:29Y", uniqueOffenceCodes = listOf(24001, 24002), paragraphDescription = Descriptions.YOI_27),
  YOI_55_26B(nomisCode = "55:26B", paragraph = "26", withOthers = "55:29V", uniqueOffenceCodes = listOf(23001, 23201), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  YOI_55_26C(nomisCode = "55:26C", paragraph = "25", withOthers = "55:29Z", uniqueOffenceCodes = listOf(23002, 23202), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  YOI_55_10(nomisCode = "55:10", paragraph = "10", withOthers = "55:29U", uniqueOffenceCodes = listOf(9001, 9002), paragraphDescription = Descriptions.YOI_10),
  YOI_55_13C(nomisCode = "55:13C", paragraph = "13", withOthers = "55:29X", uniqueOffenceCodes = listOf(12101), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_13B(nomisCode = "55:13B", paragraph = "13", withOthers = "55:29X", uniqueOffenceCodes = listOf(12102), paragraphDescription = Descriptions.YOI_13_ADULT_12),
  YOI_55_11(nomisCode = "55:11", paragraph = "11", uniqueOffenceCodes = listOf(10001), paragraphDescription = Descriptions.YOI_11),
  YOI_55_12(nomisCode = "55:12", paragraph = "12", uniqueOffenceCodes = listOf(11001), paragraphDescription = Descriptions.YOI_12),
  YOI_55_17(nomisCode = "55:17", paragraph = "17", withOthers = "55:29C", uniqueOffenceCodes = listOf(16001), paragraphDescription = Descriptions.YOI_17),

  @Deprecated("this was incorrect and only present for historical reasons")
  YOI_55_28(applicableVersions = listOf(1), nomisCode = "55:28", paragraph = "28", withOthers = "55:29I", uniqueOffenceCodes = listOf(24101), paragraphDescription = Descriptions.YOI_28),
  YOI_55_28_24(applicableVersions = listOf(2), nomisCode = "55:28 (24)", paragraph = "28", uniqueOffenceCodes = listOf(2410124), paragraphDescription = Descriptions.YOI_28_24),
  YOI_55_19(applicableVersions = listOf(1), nomisCode = "55:19", paragraph = "19", uniqueOffenceCodes = listOf(17001), paragraphDescription = Descriptions.YOI_19),
  YOI_55_19_24(applicableVersions = listOf(2), nomisCode = "55:19 (24)", paragraph = "19", uniqueOffenceCodes = listOf(1700124), paragraphDescription = Descriptions.YOI_19_24),
  YOI_55_18(nomisCode = "55:18", paragraph = "18", uniqueOffenceCodes = listOf(17002), paragraphDescription = Descriptions.YOI_18),
  YOI_55_21B(nomisCode = "55:21B", paragraph = "21", withOthers = "55:29AE", uniqueOffenceCodes = listOf(19001), paragraphDescription = Descriptions.YOI_21),
  YOI_55_21C(nomisCode = "55:21C", paragraph = "21", withOthers = "55:29AF", uniqueOffenceCodes = listOf(19002), paragraphDescription = Descriptions.YOI_21),
  YOI_55_21A(nomisCode = "55:21A", paragraph = "21", withOthers = "55:29AD", uniqueOffenceCodes = listOf(19003), paragraphDescription = Descriptions.YOI_21),
  YOI_55_23(applicableVersions = listOf(1), nomisCode = "55:23", paragraph = "23", uniqueOffenceCodes = listOf(20001), paragraphDescription = Descriptions.YOI_23_ADULT_20A),
  YOI_55_23_24(applicableVersions = listOf(2), nomisCode = "55:23 (24)", paragraph = "23", uniqueOffenceCodes = listOf(2000124), paragraphDescription = Descriptions.YOI_23_24_ADULT_20A_24),
  YOI_55_22(nomisCode = "55:22", paragraph = "22", withOthers = "55:29AC", uniqueOffenceCodes = listOf(20002), paragraphDescription = Descriptions.YOI_22_ADULT_20),
  YOI_55_25(nomisCode = "55:25", paragraph = "25", withOthers = "55:29W", uniqueOffenceCodes = listOf(22001), paragraphDescription = Descriptions.YOI_25_ADULT_22),
  YOI_55_26(nomisCode = "55:26", paragraph = "26", withOthers = "55:29AI", uniqueOffenceCodes = listOf(23101), paragraphDescription = Descriptions.YOI_26_ADULT_23),
  YOI_55_3A(nomisCode = "55:3A", paragraph = "3", withOthers = "55:29K", uniqueOffenceCodes = listOf(2001, 2021), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3C(nomisCode = "55:3C", paragraph = "3", withOthers = "55:29M", uniqueOffenceCodes = listOf(2002), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3D(nomisCode = "55:3D", paragraph = "3", withOthers = "55:29N", uniqueOffenceCodes = listOf(2003), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_3B(nomisCode = "55:3B", paragraph = "3", withOthers = "55:29L", uniqueOffenceCodes = listOf(2004), paragraphDescription = Descriptions.YOI_3_ADULT_2),
  YOI_55_4(nomisCode = "55:4", paragraph = "4", withOthers = "55:29H", uniqueOffenceCodes = listOf(3001), paragraphDescription = Descriptions.YOI_4),
  YOI_55_7(nomisCode = "55:7", paragraph = "7", withOthers = "55:29R", uniqueOffenceCodes = listOf(6001), paragraphDescription = Descriptions.YOI_7),
  YOI_55_20A(nomisCode = "55:20A", paragraph = "20", withOthers = "55:29AB", uniqueOffenceCodes = listOf(18001), paragraphDescription = Descriptions.YOI_20),
  YOI_55_20B(nomisCode = "55:20B", paragraph = "20", withOthers = "55:29AO", uniqueOffenceCodes = listOf(18002), paragraphDescription = Descriptions.YOI_20),
  YOI_55_24(nomisCode = "55:24", paragraph = "24", withOthers = "55:29AL", uniqueOffenceCodes = listOf(21001), paragraphDescription = Descriptions.YOI_24_ADULT_21),
  YOI_55_26A_24(applicableVersions = listOf(2), nomisCode = "55:26A (24)", paragraph = "26(a)", uniqueOffenceCodes = listOf(2600124), paragraphDescription = Descriptions.YOI_26A_24_ADULT_23A_24),
  MIGRATED_OFFENCE(nomisCode = "", paragraph = "", uniqueOffenceCodes = listOf(0), paragraphDescription = Descriptions.DEFAULT),
  ;

  fun getNomisCodeWithOthers() = this.withOthers ?: this.nomisCode

  companion object {
    fun validateOffenceCode(offenceCode: Int) =
      entries.flatMap { it.uniqueOffenceCodes }.firstOrNull { it == offenceCode } ?: throw ValidationException("Invalid offence code $offenceCode")

    fun Int.containsNomisCode(nomisCode: String): Boolean {
      val offenceCode = entries.first { it.uniqueOffenceCodes.contains(this) }

      return offenceCode.nomisCode == nomisCode ||
        nomisCode == "${offenceCode.nomisCode.substringBefore(":")}:${offenceCode.paragraph}" ||
        offenceCode.getNomisCodeWithOthers().contains(nomisCode)
    }
  }
}
