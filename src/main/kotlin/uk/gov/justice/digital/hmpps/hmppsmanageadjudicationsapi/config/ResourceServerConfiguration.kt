package uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket
import uk.gov.justice.digital.hmpps.hmppsmanageadjudicationsapi.controllers.DraftAdjudicationController

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
class ResourceServerConfiguration : WebSecurityConfigurerAdapter() {
  override fun configure(http: HttpSecurity) {
    http.headers().frameOptions().sameOrigin().and()
      .sessionManagement()
      .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      // Can't have CSRF protection as requires session
      .and().csrf().disable()
      .authorizeRequests { auth ->
        auth.antMatchers(
          "/webjars/**", "/favicon.ico", "/csrf",
          "/health/**", "/info",
          "/v3/api-docs/**", "/api/swagger.json", "/swagger-ui/**",
          "/swagger-resources", "/swagger-resources/configuration/ui",
          "/swagger-resources/configuration/security"
        ).permitAll()
          .anyRequest()
          .authenticated()
      }.oauth2ResourceServer().jwt().jwtAuthenticationConverter(AuthAwareTokenConverter())
  }

  @Bean
  fun api(): Docket? {
    return Docket(DocumentationType.OAS_30)
      .useDefaultResponseMessages(false)
      .select()
      .apis(RequestHandlerSelectors.basePackage(DraftAdjudicationController::class.java.getPackage().name))
      .paths(PathSelectors.any())
      .build()
  }
}
