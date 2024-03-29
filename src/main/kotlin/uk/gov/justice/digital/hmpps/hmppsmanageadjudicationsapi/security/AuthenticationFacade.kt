package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security

interface AuthenticationFacade {
  val currentUsername: String?
  val activeCaseload: String
  val isAlo: Boolean
}
