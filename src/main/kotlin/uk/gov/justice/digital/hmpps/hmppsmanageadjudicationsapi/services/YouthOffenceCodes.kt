package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class YouthOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy YO" worksheet
    fun buildLookup(): Map<Int, OffenceCodeDetails> {
      val paragraph1 = OffenceCodeParagraph("1",)
      val paragraph3 = OffenceCodeParagraph("3",)
      val paragraph4 = OffenceCodeParagraph(
        "4",
      )
      val paragraph5 = OffenceCodeParagraph("5",)
      val paragraph6 = OffenceCodeParagraph(
        "6",
      )
      val paragraph7 = OffenceCodeParagraph(
        "7",
      )
      val paragraph8 = OffenceCodeParagraph("8",)
      val paragraph9 = OffenceCodeParagraph(
        "9",
      )
      val paragraph10 = OffenceCodeParagraph(
        "10",
      )
      val paragraph11 =
        OffenceCodeParagraph("11",)
      val paragraph12 = OffenceCodeParagraph(
        "12",
      )
      val paragraph13 = OffenceCodeParagraph(
        "13",
      )
      val paragraph14 = OffenceCodeParagraph(
        "14",
      )
      val paragraph15 = OffenceCodeParagraph(
        "15",
      )
      val paragraph16 =
        OffenceCodeParagraph("16",)
      val paragraph17 = OffenceCodeParagraph(
        "17",
      )
      val paragraph19 = OffenceCodeParagraph(
        "19",
      )
      val paragraph20 = OffenceCodeParagraph(
        "20",
      )
      val paragraph21 = OffenceCodeParagraph(
        "21",
      )
      val paragraph22 = OffenceCodeParagraph("22",)
      val paragraph23 = OffenceCodeParagraph("23",)
      val paragraph24 =
        OffenceCodeParagraph("24",)
      val paragraph25 = OffenceCodeParagraph("25",)
      val paragraph26 =
        OffenceCodeParagraph("26",)
      val paragraph27 = OffenceCodeParagraph(
        "27",
      )

      return OffenceCodeLookupBuilder().nomisPrefix("55:")
        .code(1001).nomisCode("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1002).nomisCode("1A").notCommittedOnOwnNomisCodes("29D").paragraph(paragraph1)
        .code(1003).nomisCode("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1004).nomisCode("1L").notCommittedOnOwnNomisCodes("29F").paragraph(paragraph1)
        .code(1005).nomisCode("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1006).nomisCode("1M").notCommittedOnOwnNomisCodes("29G").paragraph(paragraph1)
        .code(1021).nomisCode("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1022).nomisCode("1A").notCommittedOnOwnNomisCodes("29D").paragraph(paragraph1)
        .code(1007).nomisCode("2").notCommittedOnOwnNomisCodes("2").paragraph(paragraph1)
        .code(1008).nomisCode("1E").notCommittedOnOwnNomisCodes("29E").paragraph(paragraph1)
        .code(4001).nomisCode("5").notCommittedOnOwnNomisCodes("5").paragraph(paragraph5)
        .code(5001).nomisCode("6").notCommittedOnOwnNomisCodes("29Q").paragraph(paragraph6)
        // Empty line in spreadsheet
        .code(7001).nomisCode("8").notCommittedOnOwnNomisCodes("29O").paragraph(paragraph8)
        .code(7002).nomisCode("8").notCommittedOnOwnNomisCodes("29O").paragraph(paragraph8)
        .code(8001).nomisCode("9D").notCommittedOnOwnNomisCodes("29AI").paragraph(paragraph9)
        .code(8002).nomisCode("9E").notCommittedOnOwnNomisCodes("29AJ").paragraph(paragraph9)
        // Empty line in spreadsheet
        .code(12001).nomisCode("13").notCommittedOnOwnNomisCodes("29AM").paragraph(paragraph13)
        .code(12002).nomisCode("13A").notCommittedOnOwnNomisCodes("29AH").paragraph(paragraph13)
        .code(14001).nomisCode("15").notCommittedOnOwnNomisCodes("29J").paragraph(paragraph15)
        .code(13001).nomisCode("14B").notCommittedOnOwnNomisCodes("29J").paragraph(paragraph14)
        .code(15001).nomisCode("16").notCommittedOnOwnNomisCodes("29T").paragraph(paragraph16)
        // Empty line in spreadsheet
        .code(24001).nomisCode("27").notCommittedOnOwnNomisCodes("29Y").paragraph(paragraph27)
        .code(24002).nomisCode("27").notCommittedOnOwnNomisCodes("29Y").paragraph(paragraph27)
        .code(23001).nomisCode("26B").notCommittedOnOwnNomisCodes("29V").paragraph(paragraph26)
        .code(23002).nomisCode("26C").notCommittedOnOwnNomisCodes("29Z").paragraph(paragraph26)
        .code(9001).nomisCode("10").notCommittedOnOwnNomisCodes("29U").paragraph(paragraph10)
        .code(9002).nomisCode("10").notCommittedOnOwnNomisCodes("29U").paragraph(paragraph10)
        .code(12101).nomisCode("13C").notCommittedOnOwnNomisCodes("29X").paragraph(paragraph13)
        .code(12102).nomisCode("13B").notCommittedOnOwnNomisCodes("29X").paragraph(paragraph13)
        .code(10001).nomisCode("11").notCommittedOnOwnNomisCodes("11").paragraph(paragraph11)
        .code(11001).nomisCode("12").notCommittedOnOwnNomisCodes("12").paragraph(paragraph12)
        // Empty line in spreadsheet
        .code(16001).nomisCode("17").notCommittedOnOwnNomisCodes("29C").paragraph(paragraph17)
        .code(24101).nomisCode("19").notCommittedOnOwnNomisCodes("29I").paragraph(paragraph19)
        .code(17001).nomisCode("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        .code(17002).nomisCode("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        // Empty line in spreadsheet
        .code(19001).nomisCode("21B").notCommittedOnOwnNomisCodes("29AE").paragraph(paragraph21)
        .code(19002).nomisCode("21C").notCommittedOnOwnNomisCodes("29AF").paragraph(paragraph21)
        .code(19003).nomisCode("21A").notCommittedOnOwnNomisCodes("29AD").paragraph(paragraph21)
        .code(20001).nomisCode("23").notCommittedOnOwnNomisCodes("23").paragraph(paragraph23)
        .code(20002).nomisCode("20").notCommittedOnOwnNomisCodes("29AC").paragraph(paragraph22)
        // Empty line in spreadsheet
        .code(22001).nomisCode("25").notCommittedOnOwnNomisCodes("29W").paragraph(paragraph25)
        .code(23101).nomisCode("26").notCommittedOnOwnNomisCodes("29AI").paragraph(paragraph26)
        // Empty line in spreadsheet
        .code(2001).nomisCode("3A").notCommittedOnOwnNomisCodes("29K").paragraph(paragraph3)
        .code(2002).nomisCode("3C").notCommittedOnOwnNomisCodes("29M").paragraph(paragraph3)
        .code(2003).nomisCode("3D").notCommittedOnOwnNomisCodes("29N").paragraph(paragraph3)
        .code(2021).nomisCode("3A").notCommittedOnOwnNomisCodes("29K").paragraph(paragraph3)
        .code(2004).nomisCode("3B").notCommittedOnOwnNomisCodes("29L").paragraph(paragraph3)
        // Empty line in spreadsheet
        .code(3001).nomisCode("4").notCommittedOnOwnNomisCodes("29H").paragraph(paragraph4)
        .code(6001).nomisCode("7").notCommittedOnOwnNomisCodes("29R").paragraph(paragraph7)
        .code(23201).nomisCode("26B").notCommittedOnOwnNomisCodes("29V").paragraph(paragraph26)
        .code(23202).nomisCode("26C").notCommittedOnOwnNomisCodes("29Z").paragraph(paragraph26)
        // Empty line in spreadsheet
        .code(18001).nomisCode("20A").notCommittedOnOwnNomisCodes("29AB").paragraph(paragraph20)
        .code(18002).nomisCode("20B").notCommittedOnOwnNomisCodes("29AO").paragraph(paragraph20)
        .code(21001).nomisCode("24").notCommittedOnOwnNomisCodes("29AL").paragraph(paragraph24)
        .build()
    }
  }
}
