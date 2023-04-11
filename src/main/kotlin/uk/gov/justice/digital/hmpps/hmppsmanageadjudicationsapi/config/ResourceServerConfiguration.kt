package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration {
  @Bean
  fun filterChain(http: HttpSecurity): SecurityFilterChain {
    http.headers().frameOptions().sameOrigin().and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeHttpRequests { auth ->
        auth.requestMatchers(
          "/webjars/**", "/favicon.ico", "/csrf",
          "/health/**", "/info",
          "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
          "/swagger-resources", "/swagger-resources/configuration/ui", "/swagger-resources/configuration/security",
          "/draft-adjudications/orphaned",
        ).permitAll()
          .anyRequest()
          .authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
    return http.build()
  }
}
