package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.services

class YouthOffenceCodes {
  companion object {
    // This information is taken from the spreadsheet of offences related to the ticket this work was done under.
    // Specifically these adult offences are in the "Dev Copy YO" worksheet

    /* first: paragraph front end code
       second: nomis code
       third: nomis committed with others code */
    fun buildLookup(): Map<Int, Triple<String, String, String>> =
      mapOf(
        1001 to Triple("2", "55:2", "55:2"),
        1002 to Triple("1", "55:1A", "55:29D"),
        1003 to Triple("2", "55:2", "55:2"),
        1004 to Triple("1", "55:1L", "55:29F"),
        1005 to Triple("2", "55:2", "55:2"),
        1006 to Triple("1", "55:1M", "55:29G"),
        1021 to Triple("2", "55:2", "55:2"),
        1022 to Triple("1", "55:1A", "55:29D"),
        1007 to Triple("2", "55:2", "55:2"),
        1008 to Triple("1", "55:1E", "55:29E"),
        4001 to Triple("5", "55:5", "55:5"),
        4002 to Triple("5", "55:5", "55:5"),
        4003 to Triple("5", "55:5", "55:5"),
        4004 to Triple("5", "55:5", "55:5"),
        4005 to Triple("5", "55:5", "55:5"),
        5001 to Triple("6", "55:6", "55:29Q"),
        5002 to Triple("6", "55:6", "55:29Q"),
        5003 to Triple("6", "55:6", "55:29Q"),
        5004 to Triple("6", "55:6", "55:29Q"),
        5005 to Triple("6", "55:6", "55:29Q"),
        7001 to Triple("8", "55:8", "55:29O"),
        7002 to Triple("8", "55:8", "55:29O"),
        8001 to Triple("9", "55:9D", "55:29AI"),
        8002 to Triple("9", "55:9E", "55:29AJ"),
        12001 to Triple("13", "55:13", "55:29AM"),
        12002 to Triple("13", "55:13A", "55:29AH"),
        14001 to Triple("15", "55:15", "55:29J"),
        13001 to Triple("14", "55:14B", "55:29J"),
        15001 to Triple("16", "55:16", "55:29T"),
        24001 to Triple("27", "55:27", "55:29Y"),
        24002 to Triple("27", "55:27", "55:29Y"),
        23001 to Triple("26", "55:26B", "55:29V"),
        23002 to Triple("26", "55:26C", "55:29Z"),
        9001 to Triple("10", "55:10", "55:29U"),
        9002 to Triple("10", "55:10", "55:29U"),
        12101 to Triple("13", "55:13C", "55:29X"),
        12102 to Triple("13", "55:13B", "55:29X"),
        10001 to Triple("11", "55:11", "55:11"),
        11001 to Triple("12", "55:12", "55:12"),
        16001 to Triple("17", "55:17", "55:29C"),
        24101 to Triple("19", "55:19", "55:29I"),
        17001 to Triple("18", "55:18", "55:18"),
        17002 to Triple("18", "55:18", "55:18"),
        19001 to Triple("21", "55:21B", "55:29AE"),
        19002 to Triple("21", "55:21C", "55:29AF"),
        19003 to Triple("21", "55:21A", "55:29AD"),
        20001 to Triple("23", "55:23", "55:23"),
        20002 to Triple("22", "55:20", "55:29AC"),
        22001 to Triple("25", "55:25", "55:29W"),
        23101 to Triple("26", "55:26", "55:29AI"),
        2001 to Triple("3", "55:3A", "55:29K"),
        2002 to Triple("3", "55:3C", "55:29M"),
        2003 to Triple("3", "55:3D", "55:29N"),
        2021 to Triple("3", "55:3A", "55:29K"),
        2004 to Triple("3", "55:3B", "55:29L"),
        3001 to Triple("4", "55:4", "55:29H"),
        6001 to Triple("7", "55:7", "55:29R"),
        23201 to Triple("26", "55:26B", "55:29V"),
        23202 to Triple("26", "55:26C", "55:29Z"),
        18001 to Triple("20", "55:20A", "55:29AB"),
        18002 to Triple("20", "55:20B", "55:29AO"),
        21001 to Triple("24", "55:24", "55:29AL")
      )
  }
}
