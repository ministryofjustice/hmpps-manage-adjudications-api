package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.pagination

data class PageResponse<T>(
  val pageNumber: Long,
  val pageSize: Long,
  val totalResults: Long,
  val results: List<T>,
  val firstPage: Long = 1
) {

  fun <R> map(transform: (T) -> R): PageResponse<R> = PageResponse(this.pageNumber, this.pageSize, this.totalResults, this.results.map(transform), this.firstPage)

  companion object {
    fun <T> emptyPageRequest(pageRequest: PageRequest): PageResponse<T> = PageResponse(pageRequest.pageNumber, pageRequest.pageSize, 0, emptyList(), pageRequest.firstPage)
  }
}

data class PageRequest(
  val pageNumber: Long,
  val pageSize: Long,
  val firstPage: Long = 1
)
