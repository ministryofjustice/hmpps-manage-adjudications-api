package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class AdultOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy" worksheet
    fun buildLookup(): Map<Int, OffenceCodeDetails> {
      val paragraph1 = OffenceCodeParagraph("1",)
      val paragraph2 = OffenceCodeParagraph("2")
      val paragraph3 = OffenceCodeParagraph(
        "3",
      )
      val paragraph4 = OffenceCodeParagraph("4",)
      val paragraph5 = OffenceCodeParagraph(
        "5",
      )
      val paragraph6 = OffenceCodeParagraph(
        "6",
      )
      val paragraph7 = OffenceCodeParagraph("7",)
      val paragraph8 = OffenceCodeParagraph(
        "8",
      )
      val paragraph9 = OffenceCodeParagraph(
        "9",
      )
      val paragraph10 =
        OffenceCodeParagraph("10")
      val paragraph11 = OffenceCodeParagraph(
        "11",
      )
      val paragraph12 = OffenceCodeParagraph(
        "12",
      )
      val paragraph13 = OffenceCodeParagraph("13")
      val paragraph14 = OffenceCodeParagraph(
        "14",
      )
      val paragraph15 =
        OffenceCodeParagraph("15")
      val paragraph16 = OffenceCodeParagraph(
        "16",
      )
      val paragraph17 = OffenceCodeParagraph(
        "17",
      )
      val paragraph18 = OffenceCodeParagraph(
        "18",
      )
      val paragraph19 = OffenceCodeParagraph(
        "19"
      )
      val paragraph20 = OffenceCodeParagraph("20")
      val paragraph21 =
        OffenceCodeParagraph("21")
      val paragraph22 = OffenceCodeParagraph("22")
      val paragraph23 =
        OffenceCodeParagraph("23")
      val paragraph24 = OffenceCodeParagraph(
        "24",
      )
      val paragraph24a = OffenceCodeParagraph(
        "24(a)",
      )

      return OffenceCodeLookupBuilder().nomisPrefix("51:")
        .code(1001).nomisCode("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1002).nomisCode("1B").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph1)
        .code(1003).nomisCode("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1004).nomisCode("1J").notCommittedOnOwnNomisCodes("25F").paragraph(paragraph1)
        .code(1005).nomisCode("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1006).nomisCode("1N").notCommittedOnOwnNomisCodes("25G").paragraph(paragraph1)
        .code(1021).nomisCode("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1022).nomisCode("1B").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph1)
        .code(1007).nomisCode("1A").notCommittedOnOwnNomisCodes("1A").paragraph(paragraph1)
        .code(1008).nomisCode("1F").notCommittedOnOwnNomisCodes("25E").paragraph(paragraph1)
        .code(4001).nomisCode("4").notCommittedOnOwnNomisCodes("4").paragraph(paragraph4)
        .code(5001).nomisCode("5").notCommittedOnOwnNomisCodes("25Q").paragraph(paragraph5)
        // Empty line in spreadsheet
        .code(7001).nomisCode("7").notCommittedOnOwnNomisCodes("25O").paragraph(paragraph7)
        .code(7002).nomisCode("7").notCommittedOnOwnNomisCodes("25O").paragraph(paragraph7)
        .code(8001).nomisCode("8D").notCommittedOnOwnNomisCodes("25AI").paragraph(paragraph8)
        .code(8002).nomisCode("8E").notCommittedOnOwnNomisCodes("25AJ").paragraph(paragraph8)
        // Empty line in spreadsheet
        .code(12001).nomisCode("12").notCommittedOnOwnNomisCodes("25AM").paragraph(paragraph12)
        .code(12002).nomisCode("12A").notCommittedOnOwnNomisCodes("25AH").paragraph(paragraph12)
        .code(14001).nomisCode("14").notCommittedOnOwnNomisCodes("25J").paragraph(paragraph14)
        .code(13001).nomisCode("13B").notCommittedOnOwnNomisCodes("25J").paragraph(paragraph13)
        .code(15001).nomisCode("15").notCommittedOnOwnNomisCodes("25T").paragraph(paragraph15)
        // Empty line in spreadsheet
        .code(24001).nomisCode("24").notCommittedOnOwnNomisCodes("25Y").paragraph(paragraph24)
        .code(24002).nomisCode("24").notCommittedOnOwnNomisCodes("25Y").paragraph(paragraph24)
        .code(23001).nomisCode("23AP").notCommittedOnOwnNomisCodes("25V").paragraph(paragraph23)
        .code(23002).nomisCode("25Z").notCommittedOnOwnNomisCodes("25Z").paragraph(paragraph23)
        .code(9001).nomisCode("9").notCommittedOnOwnNomisCodes("25U").paragraph(paragraph9)
        .code(9002).nomisCode("9").notCommittedOnOwnNomisCodes("25U").paragraph(paragraph9)
        .code(12101).nomisCode("12AQ").notCommittedOnOwnNomisCodes("25X").paragraph(paragraph12)
        .code(12102).nomisCode("12AQ").notCommittedOnOwnNomisCodes("25X").paragraph(paragraph12)
        .code(10001).nomisCode("10").notCommittedOnOwnNomisCodes("10").paragraph(paragraph10)
        .code(11001).nomisCode("11").notCommittedOnOwnNomisCodes("11").paragraph(paragraph11)
        // Empty line in spreadsheet
        .code(16001).nomisCode("16").notCommittedOnOwnNomisCodes("25C").paragraph(paragraph16)
        .code(24101).nomisCode("24A").notCommittedOnOwnNomisCodes("25L").paragraph(paragraph24a)
        .code(17001).nomisCode("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        .code(17002).nomisCode("17").notCommittedOnOwnNomisCodes("17").paragraph(paragraph17)
        // Empty line in spreadsheet
        .code(19001).nomisCode("19B").notCommittedOnOwnNomisCodes("25AE").paragraph(paragraph19)
        .code(19002).nomisCode("19C").notCommittedOnOwnNomisCodes("25AF").paragraph(paragraph19)
        .code(19003).nomisCode("19A").notCommittedOnOwnNomisCodes("25D").paragraph(paragraph19)
        .code(20001).nomisCode("20A").notCommittedOnOwnNomisCodes("20A").paragraph(paragraph20)
        .code(20002).nomisCode("20").notCommittedOnOwnNomisCodes("25AC").paragraph(paragraph20)
        // Empty line in spreadsheet
        .code(22001).nomisCode("22").notCommittedOnOwnNomisCodes("25W").paragraph(paragraph22)
        .code(23101).nomisCode("23").notCommittedOnOwnNomisCodes("25AN").paragraph(paragraph23)
        // Empty line in spreadsheet
        .code(2001).nomisCode("2A").notCommittedOnOwnNomisCodes("25K").paragraph(paragraph2)
        .code(2002).nomisCode("2C").notCommittedOnOwnNomisCodes("25M").paragraph(paragraph2)
        .code(2003).nomisCode("2D").notCommittedOnOwnNomisCodes("25N").paragraph(paragraph2)
        .code(2021).nomisCode("2A").notCommittedOnOwnNomisCodes("25K").paragraph(paragraph2)
        .code(2004).nomisCode("2B").notCommittedOnOwnNomisCodes("25L").paragraph(paragraph2)
        // Empty line in spreadsheet
        .code(3001).nomisCode("3").notCommittedOnOwnNomisCodes("25H").paragraph(paragraph3)
        .code(6001).nomisCode("6").notCommittedOnOwnNomisCodes("25R").paragraph(paragraph6)
        .code(23201).nomisCode("23AP").notCommittedOnOwnNomisCodes("25V").paragraph(paragraph23)
        .code(23202).nomisCode("25Z").notCommittedOnOwnNomisCodes("25Z").paragraph(paragraph23)
        // Empty line in spreadsheet
        .code(18001).nomisCode("18A").notCommittedOnOwnNomisCodes("25B").paragraph(paragraph18)
        .code(18002).nomisCode("18B").notCommittedOnOwnNomisCodes("25AO").paragraph(paragraph18)
        .code(21001).nomisCode("21").notCommittedOnOwnNomisCodes("25AL").paragraph(paragraph21)
        .build()
    }
  }
}
