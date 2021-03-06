package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.gateways.pagination

import org.springframework.data.domain.Pageable

fun getPageableUrlParameters(pageable: Pageable): String = getPageUrlParameters(pageable).plus(getSortUrlParameters(pageable)).joinToString("&")

private fun getPageUrlParameters(pageable: Pageable): List<String> {
  val params = mapOf(
    "size" to pageable.pageSize,
    "page" to pageable.pageNumber
  )
  return params.map { it.key + "=" + it.value }
}

private fun getSortUrlParameters(pageable: Pageable): List<String> {
  return pageable.sort.map { "sort=" + it.property + "," + it.direction }.toList()
}
