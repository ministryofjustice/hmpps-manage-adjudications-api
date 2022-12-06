package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class AdultOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy" worksheet
    /* first: paragraph front end code
       second: nomis code
       third: nomis committed with others code */

    fun buildLookup(): Map<Int, Triple<String, String, String>> =
      mapOf(
        1001 to Triple("1(a)", "51:1A", "51:1A"),
        1002 to Triple("1", "51:1B", "51:25D"),
        1003 to Triple("1(a)", "51:1A", "51:1A"),
        1004 to Triple("1", "51:1J", "51:25F"),
        1005 to Triple("1(a)", "51:1A", "51:1A"),
        1006 to Triple("1", "51:1N", "51:25G"),
        1021 to Triple("1(a)", "51:1A", "51:1A"),
        1022 to Triple("1", "51:1B", "51:25D"),
        1007 to Triple("1(a)", "51:1A", "51:1A"),
        1008 to Triple("1", "51:1F", "51:25E"),
        4001 to Triple("4", "51:4", "51:4"),
        4002 to Triple("4", "51:4", "51:4"),
        4003 to Triple("4", "51:4", "51:4"),
        4004 to Triple("4", "51:4", "51:4"),
        4005 to Triple("4", "51:4", "51:4"),
        5001 to Triple("5", "51:5", "51:25Q"),
        5002 to Triple("5", "51:5", "51:25Q"),
        5003 to Triple("5", "51:5", "51:25Q"),
        5004 to Triple("5", "51:5", "51:25Q"),
        5005 to Triple("5", "51:5", "51:25Q"),
        7001 to Triple("7", "51:7", "51:25O"),
        7002 to Triple("7", "51:7", "51:25O"),
        8001 to Triple("8", "51:8D", "51:25AI"),
        8002 to Triple("8", "51:8E", "51:25AJ"),
        12001 to Triple("12", "51:12", "51:25AM"),
        12002 to Triple("12", "51:12A", "51:25AH"),
        14001 to Triple("14", "51:14", "51:25J"),
        13001 to Triple("13", "51:13B", "51:25J"),
        15001 to Triple("15", "51:15", "51:25T"),
        24001 to Triple("24", "51:24", "51:25Y"),
        24002 to Triple("24", "51:24", "51:25Y"),
        23001 to Triple("23", "51:23AP", "51:25V"),
        23002 to Triple("23", "51:25Z", "51:25Z"),
        9001 to Triple("9", "51:9", "51:25U"),
        9002 to Triple("9", "51:9", "51:25U"),
        12101 to Triple("12", "51:12AQ", "51:25X"),
        12102 to Triple("12", "51:12AQ", "51:25X"),
        10001 to Triple("10", "51:10", "51:10"),
        11001 to Triple("11", "51:11", "51:11"),
        16001 to Triple("16", "51:16", "51:25C"),
        24101 to Triple("17(a)", "51:17A", "51:25L"),
        17001 to Triple("17", "51:17", "51:17"),
        17002 to Triple("17", "51:17", "51:17"),
        19001 to Triple("19", "51:19B", "51:25AE"),
        19002 to Triple("19", "51:19C", "51:25AF"),
        19003 to Triple("19", "51:19A", "51:25D"),
        20001 to Triple("20(a)", "51:20A", "51:20A"),
        20002 to Triple("20", "51:20", "51:25AC"),
        22001 to Triple("22", "51:22", "51:25W"),
        23101 to Triple("23", "51:23", "51:25AN"),
        2001 to Triple("2", "51:2A", "51:25K"),
        2002 to Triple("2", "51:2C", "51:25M"),
        2003 to Triple("2", "51:2D", "51:25N"),
        2021 to Triple("2", "51:2A", "51:25K"),
        2004 to Triple("2", "51:2B", "51:25L"),
        3001 to Triple("3", "51:3", "51:25H"),
        6001 to Triple("6", "51:6", "51:25R"),
        23201 to Triple("23", "51:23AP", "51:25V"),
        23202 to Triple("23", "51:25Z", "51:25Z"),
        18001 to Triple("18", "51:18A", "51:25B"),
        18002 to Triple("18", "51:18B", "51:25AO"),
        21001 to Triple("21", "51:21", "51:25AL")
      )
  }
}
