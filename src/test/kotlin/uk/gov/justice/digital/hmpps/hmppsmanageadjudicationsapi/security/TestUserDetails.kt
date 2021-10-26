package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class TestUserDetails(private val username: String) :
  UserDetails {
  override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
    TODO("Not yet implemented")
  }

  override fun getPassword(): String {
    TODO("Not yet implemented")
  }

  override fun getUsername(): String = username

  override fun isAccountNonExpired(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isAccountNonLocked(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isCredentialsNonExpired(): Boolean {
    TODO("Not yet implemented")
  }

  override fun isEnabled(): Boolean {
    TODO("Not yet implemented")
  }
}
